package org.jingxu.service;

import org.jingxu.mapper.SubmissionMapper;
import org.jingxu.pojo.Actor;
import org.jingxu.pojo.PdfAttachment;
import org.jingxu.util.WebProblem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.jingxu.util.Data.*;

/** 管理匿名作品交件、历史版本和评审锁定。 */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class SubmissionService {
    private final SubmissionMapper mapper;
    private final CompetitionService competitions;
    private final RegistrationService registrations;
    private final PdfStorage storage;

    /** 接入交件数据、赛程、报名和私有文件存储。 */
    public SubmissionService(SubmissionMapper mapper, CompetitionService competitions,
                             RegistrationService registrations, PdfStorage storage) {
        this.mapper = mapper;
        this.competitions = competitions;
        this.registrations = registrations;
        this.storage = storage;
    }

    /** 验证报名成员权限后读取作品历史。 */
    public Map<String, Object> studentView(Actor actor, long registrationId) {
        registrations.requireMember(actor, registrations.require(registrationId));
        Map<String, Object> submission = mapper.forRegistration(registrationId);
        return submission == null ? fields("registrationId", registrationId, "versions", List.of()) : view(submission);
    }

    /** 校验 PDF 和交件期限，幂等保存作品版本。 */
    public Map<String, Object> upload(Actor actor, long registrationId, String titleInput,
                                      String requestInput, MultipartFile file) {
        Map<String, Object> parent = registrations.require(registrationId);
        registrations.requireCaptain(actor, parent);
        String title = required(fields("作品名称", titleInput), "作品名称", 120);
        String requestId = requireRequestId(requestInput);
        try (PdfStorage.StagedPdf staged = storage.stage(file)) {
            Map<String, Object> competition = competitions.lock(number(parent, "competitionId"));
            Map<String, Object> registration = registrations.lock(registrationId);
            registrations.requireCaptain(actor, registration);
            if (!"APPROVED".equals(registration.get("status"))) throw WebProblem.conflict("报名审核通过后可提交作品。");

            // 第 1 步 先识别原始请求，截止后仍可确认已成功接收的版本。
            Map<String, Object> row = mapper.forRegistration(registrationId);
            String requestHash = hash(staged.sha256() + "\n" + title);
            if (row != null) {
                Map<String, Object> previous = mapper.request(fields("id", row.get("id"), "requestId", requestId));
                if (previous != null) {
                    if (!requestHash.equals(previous.get("requestHash"))) throw WebProblem.conflict("请求标识已用于不同作品。");
                    return versionView(previous, text(row, "anonymousCode"));
                }
            }
            LocalDateTime now = competitions.now();
            competitions.requireWindow(competition, "submissionOpensAt", "submissionClosesAt", now);
            if (row == null) {
                row = fields("registrationId", registrationId, "anonymousCode", identifier("A-"));
                mapper.insert(row);
            }
            long id = number(row, "id");
            Map<String, Object> latest = mapper.latest(id);
            long version = latest == null ? 1 : number(latest, "versionNo") + 1;

            // 第 2 步 同卷原子落盘后登记版本，避免成功记录指向未保存的文件。
            String storageKey = storage.commit(staged);
            mapper.addVersion(fields("id", id, "versionNo", version, "requestId", requestId, "requestHash", requestHash,
                    "title", title, "storageKey", storageKey, "fileSha256", staged.sha256(), "actorId", actor.id(), "now", now));
            return versionView(mapper.latest(id), text(row, "anonymousCode"));
        }
    }

    /** 验证报名成员身份后下载指定版本。 */
    public PdfAttachment studentFile(Actor actor, long registrationId, int versionNo) {
        registrations.requireMember(actor, registrations.require(registrationId));
        Map<String, Object> row = mapper.forRegistration(registrationId);
        if (row == null) throw WebProblem.missing();
        return file(row, versionNo);
    }

    /** 验证赛管权限后下载指定作品版本。 */
    public PdfAttachment adminFile(Actor actor, long submissionId, int versionNo) {
        Map<String, Object> row = require(submissionId);
        competitions.checkManager(actor, competitions.require(number(row, "competitionId")));
        return file(row, versionNo);
    }

    /** 按作品和版本号查找对应文件。 */
    private PdfAttachment file(Map<String, ?> row, int versionNo) {
        Map<String, Object> version = mapper.version(fields("id", row.get("id"), "version", versionNo));
        if (version == null) throw WebProblem.missing();
        return storage.attachment(text(version, "storageKey"), text(row, "anonymousCode") + "-V" + versionNo);
    }

    /** 以匿名编号返回仅供当前任务评审的文件。 */
    public PdfAttachment judgeFile(Map<String, ?> assignment) {
        Map<String, Object> version = mapper.versionById(number(assignment, "versionId"));
        if (version == null) throw WebProblem.missing();
        return storage.attachment(text(version, "storageKey"), text(assignment, "anonymousCode"));
    }

    /** 验证赛事管理权后读取作品详情。 */
    public Map<String, Object> adminView(Actor actor, long id) {
        Map<String, Object> row = require(id);
        competitions.checkManager(actor, competitions.require(number(row, "competitionId")));
        return view(row);
    }

    /** 分页查询参评作品、版本与评分情况。 */
    public Map<String, Object> list(Actor actor, long competitionId, Map<String, ?> values) {
        competitions.checkManager(actor, competitions.require(competitionId));
        int page = queryNumber(values, "page", 1, 100_000);
        int size = queryNumber(values, "size", 20, 100);
        Map<String, Object> query = fields("competitionId", competitionId, "q", optional(values, "q", 32),
                "state", optional(values, "state", 12), "size", size, "offset", (page - 1) * size);
        List<Map<String, Object>> rows = mapper.workList(query);
        rows.forEach(row -> row.put("lockedAt", withOffset(row, "lockedAt")));
        return page(rows, page, size, mapper.workCount(query));
    }

    /** 记录作品参评决定及原因。 */
    public Map<String, Object> eligibility(Actor actor, long id, Map<String, ?> values) {
        long competitionId = number(require(id), "competitionId");
        Map<String, Object> competition = competitions.managedLock(actor, competitionId);
        competitions.requireOpen(competition);
        if (competition.get("currentPublicationId") != null) throw WebProblem.conflict("已发布的结果须通过更正处理。");
        Map<String, Object> submission = mapper.lock(id);
        matchVersion(submission, values);
        String next = switch (required(values, "decision", 12)) {
            case "EXCLUDE" -> "EXCLUDED";
            case "RESTORE" -> "ELIGIBLE";
            default -> throw WebProblem.invalid("参评决定无效。");
        };
        String reason = required(values, "reason", 400);
        mapper.eligibility(fields("id", id, "status", next, "reason", "EXCLUDED".equals(next) ? reason : null));
        competitions.mark(actor, "SUBMISSION_ELIGIBILITY", competitionId, fields("submissionId", id, "status", next, "reason", reason));
        return adminView(actor, id);
    }

    /** 按编号读取作品，缺失时返回相应错误。 */
    public Map<String, Object> require(long id) {
        Map<String, Object> row = mapper.find(id);
        if (row == null) throw WebProblem.missing();
        return row;
    }

    /** 交件窗口关闭后，将最后一个有效版本锁定用于评审。 */
    public Map<String, Object> lockFinal(long id, Map<String, ?> competition) {
        Map<String, Object> row = mapper.lock(id);
        LocalDateTime now = competitions.now();
        if (now.isBefore(date(competition, "submissionClosesAt"))) throw WebProblem.conflict("作品截止后才可锁定版本。");
        if (row.get("lockedVersionId") != null) return row;
        Map<String, Object> latest = mapper.latest(id);
        if (latest == null) throw WebProblem.conflict("该作品没有有效版本。");
        mapper.lockVersion(fields("id", id, "versionId", latest.get("id"), "now", now));
        return mapper.lock(id);
    }

    /** 组装作品版本列表并格式化锁定时间。 */
    private Map<String, Object> view(Map<String, Object> row) {
        row.put("lockedAt", withOffset(row, "lockedAt"));
        row.put("versions", mapper.versions(number(row, "id")).stream()
                .map(version -> versionView(version, text(row, "anonymousCode"))).toList());
        return row;
    }

    /** 生成面向用户的作品版本和回执信息。 */
    private static Map<String, Object> versionView(Map<String, Object> version, String code) {
        return fields("id", version.get("id"), "versionNo", version.get("versionNo"), "title", version.get("title"),
                "submittedAt", withOffset(version, "submittedAt"), "receipt", code + "-V" + version.get("versionNo"));
    }

    /** 校验交件或发布请求标识的格式。 */
    static String requireRequestId(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_-]{8,64}")) throw WebProblem.invalid("请求标识格式有误。");
        return value;
    }

    /** 计算幂等校验使用的 SHA-256 字符串摘要。 */
    static String hash(String value) {
        return HexFormat.of().formatHex(PdfStorage.sha256().digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
