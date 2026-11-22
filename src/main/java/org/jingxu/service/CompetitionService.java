package org.jingxu.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jingxu.mapper.CompetitionMapper;
import org.jingxu.pojo.Actor;
import org.jingxu.util.JsonSupport;
import org.jingxu.util.WebProblem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jingxu.util.Data.*;

/** 管理赛事生命周期、赛程及公开公告。 */
@Service
public class CompetitionService {
    public static final List<String> DATES = List.of("registrationOpensAt", "registrationClosesAt", "correctionClosesAt",
            "qualificationClosesAt", "submissionOpensAt", "submissionClosesAt", "judgingOpensAt", "judgingClosesAt");
    private static final Set<Integer> STRICT_DATES = Set.of(1, 5, 7);
    private final CompetitionMapper mapper;
    private final AccountService accounts;
    private final Clock clock;
    private final ObjectMapper json;

    /** 接入赛事数据、账户审计和评分规则序列化。 */
    public CompetitionService(CompetitionMapper mapper, AccountService accounts, Clock clock, ObjectMapper json) {
        this.mapper = mapper;
        this.accounts = accounts;
        this.clock = clock;
        this.json = json;
    }

    /** 根据可见范围及筛选条件分页查询赛事。 */
    public Map<String, Object> list(Actor organiser, Map<String, ?> values) {
        int page = queryNumber(values, "page", 1, 100_000);
        int size = queryNumber(values, "size", 20, 100);
        Map<String, Object> filter = fields("publicOnly", organiser == null, "now", now(),
                "organizerId", organiser == null || organiser.has("ADMIN") ? null : organiser.id(),
                "q", optional(values, "q", 120), "mode", optional(values, "mode", 12),
                "phase", optional(values, "phase", 24), "offset", (page - 1) * size, "size", size);
        List<Map<String, Object>> items = mapper.list(filter).stream().map(row -> view(row, organiser == null)).toList();
        return page(items, page, size, mapper.count(filter));
    }

    /** 返回公开可访问的赛事详情。 */
    public Map<String, Object> publicView(long id) {
        Map<String, Object> row = require(id);
        if ("DRAFT".equals(row.get("status"))) throw WebProblem.missing();
        return view(row, true);
    }

    /** 校验管理权后返回赛事完整资料。 */
    public Map<String, Object> adminView(Actor actor, long id) {
        Map<String, Object> row = require(id);
        checkManager(actor, row);
        return view(row, false);
    }

    /** 按编号读取赛事，缺失时返回相应错误。 */
    public Map<String, Object> require(long id) {
        Map<String, Object> row = mapper.find(id);
        if (row == null) throw WebProblem.missing();
        return row;
    }

    /** 锁定赛事行以保护同一赛事中的并发写入。 */
    public Map<String, Object> lock(long id) {
        Map<String, Object> row = mapper.lock(id);
        if (row == null) throw WebProblem.missing();
        return row;
    }

    /** 判断操作者的管理权限，并检查评审角色的利益冲突。 */
    public void checkManager(Actor actor, Map<String, ?> row) {
        if ((!actor.has("ADMIN") && (!actor.has("ORGANIZER") || actor.id() != number(row, "organizerId")))
                || mapper.judgeInvolved(fields("competitionId", row.get("id"), "actorId", actor.id()))) {
            throw WebProblem.missing();
        }
    }

    /** 验证表单并保存新的赛事草稿。 */
    @Transactional
    public Map<String, Object> create(Actor actor, Map<String, ?> values) {
        Map<String, Object> row = validated(values);
        row.put("competitionNo", identifier("CMP-"));
        row.put("organizerId", actor.id());
        mapper.insert(row);
        mark(actor, "COMPETITION_CREATED", number(row, "id"), Map.of());
        return adminView(actor, number(row, "id"));
    }

    /** 按版本保存草稿规则、赛程和维度。 */
    @Transactional
    public Map<String, Object> edit(Actor actor, long id, Map<String, ?> values) {
        Map<String, Object> current = managedLock(actor, id);
        requireDraft(current);
        matchVersion(current, values);
        Map<String, Object> merged = fields();
        merged.putAll(view(current, false));
        merged.putAll(values);
        Map<String, Object> row = validated(merged);
        row.put("id", id);
        mapper.updateDraft(row);
        mark(actor, "COMPETITION_EDITED", id, Map.of());
        return adminView(actor, id);
    }

    /** 删除未发布草稿并记录赛事名称供审计查询。 */
    @Transactional
    public void delete(Actor actor, long id) {
        Map<String, Object> draft = managedLock(actor, id);
        requireDraft(draft);
        mapper.deleteDraft(id);
        mark(actor, "COMPETITION_DELETED", id, fields("targetName", draft.get("name")));
    }

    /** 在报名截止前将草稿发布。 */
    @Transactional
    public Map<String, Object> publish(Actor actor, long id, Map<String, ?> values) {
        Map<String, Object> row = managedLock(actor, id);
        requireDraft(row);
        matchVersion(row, values);
        if (!now().isBefore(date(row, "registrationClosesAt"))) {
            throw WebProblem.conflict("报名截止时间已过，请先更新赛程。");
        }
        return transition(actor, id, "PUBLISHED", null);
    }

    /** 停止赛事并记录取消原因。 */
    @Transactional
    public Map<String, Object> cancel(Actor actor, long id, Map<String, ?> values) {
        Map<String, Object> row = managedLock(actor, id);
        requireOpen(row);
        if (row.get("currentPublicationId") != null) throw WebProblem.conflict("已发布结果的赛事可以归档。");
        return transition(actor, id, "CANCELLED", required(values, "reason", 400));
    }

    /** 归档已经发布结果的赛事。 */
    @Transactional
    public Map<String, Object> archive(Actor actor, long id) {
        Map<String, Object> row = managedLock(actor, id);
        requireOpen(row);
        if (row.get("currentPublicationId") == null) throw WebProblem.conflict("发布结果后方可归档。");
        return transition(actor, id, "ARCHIVED", null);
    }

    /** 延长单个阶段，必要时将后续节点同步顺延。 */
    @Transactional
    public Map<String, Object> extend(Actor actor, long id, Map<String, ?> values) {
        Map<String, Object> row = managedLock(actor, id);
        requireOpen(row);
        matchVersion(row, values);
        String field = required(values, "stage", 24) + "ClosesAt";
        LocalDateTime now = now();
        if (row.get("currentPublicationId") != null || !DATES.contains(field)
                || (!"judgingClosesAt".equals(field) && !now.isBefore(date(row, field)))) {
            throw WebProblem.conflict("当前阶段不可延期。");
        }
        LocalDateTime current = date(row, field);
        LocalDateTime revised = utcInput(values, "newDeadline");
        if (!revised.isAfter(current) || !revised.isAfter(now)) throw WebProblem.invalid("新截止时间须晚于当前时间及原截止时间。");
        String reason = required(values, "reason", 400);
        row.put(field, revised);
        int next = DATES.indexOf(field) + 1;
        if (next < DATES.size() && revised.isAfter(date(row, DATES.get(next)))) {
            // 将受影响的后续阶段整体顺延，保留既定窗口长度。
            Duration shift = Duration.between(date(row, DATES.get(next)), revised);
            for (String later : DATES.subList(next, DATES.size())) {
                row.put(later, date(row, later).plus(shift));
            }
        }
        validateDates(row);
        mapper.updateSchedule(row);
        mark(actor, "SCHEDULE_EXTENDED", id, fields("stage", field, "reason", reason, "deadline", revised));
        return adminView(actor, id);
    }

    /** 读取管理端赛事概览和统计。 */
    public Map<String, Object> overview(Actor actor, long id) {
        Map<String, Object> result = adminView(actor, id);
        result.putAll(mapper.overview(id));
        return result;
    }

    /** 返回指定赛事当前公开的公告。 */
    @Transactional(readOnly = true)
    public Map<String, Object> announcement(long id) {
        Map<String, Object> result = mapper.announcement(id);
        if (result == null) throw WebProblem.missing();
        attachAwards(List.of(result));
        return result;
    }

    /** 分页查询已发布的获奖公告。 */
    @Transactional(readOnly = true)
    public Map<String, Object> announcements(Map<String, ?> values) {
        int page = queryNumber(values, "page", 1, 100_000);
        int size = 8;
        List<Map<String, Object>> items = mapper.announcements(fields("size", size, "offset", (page - 1) * size));
        attachAwards(items);
        return page(items, page, size, mapper.countAnnouncements());
    }

    /** 按公告批次合并公开获奖条目和名单。 */
    private void attachAwards(List<Map<String, Object>> announcements) {
        if (announcements.isEmpty()) return;
        List<Long> batchIds = announcements.stream().map(row -> number(row, "publicationId")).toList();
        Map<Long, List<Map<String, Object>>> byBatch = new HashMap<>();
        for (Map<String, Object> award : mapper.awards(batchIds)) {
            long batch = number(award, "publicationId");
            award.remove("publicationId");
            award.put("recipientNames", JsonSupport.stringList(json, award.get("recipientNames")));
            byBatch.computeIfAbsent(batch, ignored -> new ArrayList<>()).add(award);
        }
        for (Map<String, Object> announcement : announcements) {
            long batch = number(announcement, "publicationId");
            announcement.remove("publicationId");
            announcement.put("noAwards", flag(announcement.get("noAwards")));
            announcement.put("publishedAt", withOffset(announcement, "publishedAt"));
            announcement.put("awards", byBatch.getOrDefault(batch, List.of()));
        }
    }

    /** 锁定赛事后验证当前操作者的管理权。 */
    public Map<String, Object> managedLock(Actor actor, long id) {
        Map<String, Object> row = lock(id);
        checkManager(actor, row);
        return row;
    }

    /** 记录赛事级操作及结构化摘要。 */
    public void mark(Actor actor, String action, long id, Map<String, Object> summary) {
        accounts.audit(actor.id(), action, "COMPETITION", id, summary);
    }

    /** 要求赛事为允许业务写入的已发布状态。 */
    public void requireOpen(Map<String, ?> competition) {
        if (!"PUBLISHED".equals(competition.get("status"))) throw WebProblem.conflict("赛事当前不可写入。");
    }

    /** 校验开始时刻可操作，截止时刻起关闭。 */
    public void requireWindow(Map<String, ?> row, String start, String end, LocalDateTime at) {
        requireOpen(row);
        if (at.isBefore(date(row, start)) || !at.isBefore(date(row, end))) {
            throw WebProblem.conflict("当前不在允许操作的赛事时间窗口内。");
        }
    }

    /** 返回微秒精度的当前业务时刻。 */
    public LocalDateTime now() { return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS); }

    /** 限定操作仅针对尚未发布的赛事草稿。 */
    private static void requireDraft(Map<String, ?> row) {
        if (!"DRAFT".equals(row.get("status"))) throw WebProblem.conflict("仅赛事草稿可执行此操作。");
    }

    /** 保存赛事状态变化并追加审计。 */
    private Map<String, Object> transition(Actor actor, long id, String status, String reason) {
        mapper.updateState(fields("id", id, "status", status, "reason", reason));
        mark(actor, "COMPETITION_" + status, id, fields("reason", reason));
        return adminView(actor, id);
    }

    /** 校验并归一化参赛方式、队伍人数、赛程和评分规则。 */
    private Map<String, Object> validated(Map<String, ?> values) {
        String mode = required(values, "entryMode", 12);
        if (!Set.of("INDIVIDUAL", "TEAM").contains(mode)) throw WebProblem.invalid("请选择个人赛或团队赛。");
        int min = "TEAM".equals(mode) ? integer(values, "minTeamSize", 2, 20) : 1;
        int max = "TEAM".equals(mode) ? integer(values, "maxTeamSize", min, 20) : 1;
        Map<String, Object> row = fields("name", required(values, "name", 120), "edition", required(values, "edition", 40),
                "rulesText", required(values, "rulesText", 8000), "entryMode", mode, "minTeamSize", min, "maxTeamSize", max,
                "requiredJudges", integer(values, "requiredJudges", 1, 20), "scoringRubric", rubric(values.get("scoringRubric")));
        for (String field : DATES) row.put(field, utcInput(values, field));
        validateDates(row);
        return row;
    }

    /** 按赛程顺序检查各节点与严格的起止条件。 */
    private void validateDates(Map<String, ?> row) {
        for (int i = 1; i < DATES.size(); i++) {
            int comparison = date(row, DATES.get(i - 1)).compareTo(date(row, DATES.get(i)));
            if (comparison > 0 || (comparison == 0 && STRICT_DATES.contains(i))) {
                throw WebProblem.invalid("赛程顺序有误，请核对各阶段的开始与截止时间。");
            }
        }
    }

    /** 校验评分维度和权重总和并编码保存。 */
    private String rubric(Object value) {
        if (!(value instanceof List<?> dimensions) || dimensions.isEmpty() || dimensions.size() > 12) {
            throw WebProblem.invalid("评分规则须包含 1 至 12 个评分维度。");
        }
        Set<String> ids = new HashSet<>();
        List<Map<String, Object>> normalized = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO;
        for (Object entry : dimensions) {
            if (!(entry instanceof Map<?, ?> dimension)) throw WebProblem.invalid("评分维度格式有误。");
            String id = dimension.get("id") instanceof String key ? key : "";
            String label = dimension.get("label") instanceof String title ? title.strip() : "";
            if (!id.matches("[a-zA-Z0-9_-]{1,24}") || label.isEmpty() || label.length() > 40 || !ids.add(id)) {
                throw WebProblem.invalid("评分维度的编号须唯一，且必须填写名称。");
            }
            BigDecimal weight;
            try {
                weight = new BigDecimal(String.valueOf(dimension.get("weight")));
            } catch (NumberFormatException exception) {
                throw WebProblem.invalid("评分权重须为数字。");
            }
            if (weight.signum() <= 0 || weight.compareTo(BigDecimal.valueOf(100)) > 0 || weight.stripTrailingZeros().scale() > 2) {
                throw WebProblem.invalid("评分权重须在 0 至 100 之间，最多两位小数。");
            }
            normalized.add(fields("id", id, "label", label, "weight", weight));
            sum = sum.add(weight);
        }
        if (sum.compareTo(BigDecimal.valueOf(100)) != 0) throw WebProblem.invalid("评分权重总和须为 100%。");
        return JsonSupport.encode(json, normalized);
    }

    /** 将赛事保存的评分规则还原为维度列表。 */
    public List<Map<String, Object>> criteria(Map<String, ?> row) {
        try {
            return json.readValue(text(row, "scoringRubric"), new TypeReference<>() {});
        } catch (Exception exception) {
            throw new IllegalStateException("Stored rubric is invalid", exception);
        }
    }

    /** 转换赛程字段和阶段，按公开范围裁剪内容。 */
    private Map<String, Object> view(Map<String, Object> raw, boolean publicOnly) {
        Map<String, Object> row = fields();
        row.putAll(raw);
        row.put("phase", phase(raw));
        row.put("scoringRubric", criteria(raw));
        for (String field : DATES) row.put(field, withOffset(raw, field));
        if (publicOnly) {
            row.remove("organizerId");
            row.remove("rowVersion");
            row.put("hasAnnouncement", row.remove("currentPublicationId") != null);
        }
        return row;
    }

    /** 根据当前发布批次和赛程计算展示阶段。 */
    public String phase(Map<String, ?> row) {
        String status = text(row, "status");
        if (Set.of("DRAFT", "CANCELLED").contains(status)) return status;
        if (row.get("currentPublicationId") != null) return "RESULTS";
        LocalDateTime at = now();
        List<String> checkpoints = List.of("registrationOpensAt", "registrationClosesAt", "qualificationClosesAt",
                "submissionOpensAt", "submissionClosesAt", "judgingOpensAt", "judgingClosesAt");
        List<String> phases = List.of("UPCOMING", "REGISTRATION", "QUALIFICATION", "AWAITING_SUBMISSION",
                "SUBMISSION", "AWAITING_JUDGING", "JUDGING");
        for (int i = 0; i < checkpoints.size(); i++) {
            if (at.isBefore(date(row, checkpoints.get(i)))) return phases.get(i);
        }
        return "AWAITING_RESULTS";
    }
}
