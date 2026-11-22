package org.jingxu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jingxu.mapper.ResultMapper;
import org.jingxu.mapper.ReviewMapper;
import org.jingxu.pojo.Actor;
import org.jingxu.util.JsonSupport;
import org.jingxu.util.WebProblem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.jingxu.util.Data.*;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class ResultService {
    private final ResultMapper mapper;
    private final ReviewMapper reviewMapper;
    private final ReviewService reviews;
    private final CompetitionService competitions;
    private final ObjectMapper json;
    private final byte[] previewKey = new byte[32];

    public ResultService(ResultMapper mapper, ReviewMapper reviewMapper, ReviewService reviews,
                         CompetitionService competitions, ObjectMapper json) {
        this.mapper = mapper;
        this.reviewMapper = reviewMapper;
        this.reviews = reviews;
        this.competitions = competitions;
        this.json = json;
        new SecureRandom().nextBytes(previewKey);
    }

    private record Inputs(String mode, Long baseId, boolean noAwards, String reason,
                          Map<Long, Map<String, Object>> changes) {}
    private record Draft(List<Map<String, Object>> results, List<Map<String, Object>> basis, List<String> problems) {}

    public Map<String, Object> preview(Actor actor, long id, Map<String, ?> values) {
        Map<String, Object> competition = competitions.managedLock(actor, id);
        Inputs input = inputs(values, required(values, "mode", 12));
        validateMode(input, competition);
        Draft draft = compute(competition, input);
        long expires = competitions.now().toEpochSecond(ZoneOffset.UTC) + 600;
        return fields("items", draft.results(), "problems", draft.problems(), "canPublish", draft.problems().isEmpty(),
                "previewToken", draft.problems().isEmpty() ? sign(actor, competition, input, draft, expires) : null);
    }

    public Map<String, Object> publish(Actor actor, long id, String mode, Map<String, ?> values) {
        Map<String, Object> competition = competitions.managedLock(actor, id);
        Inputs input = inputs(values, mode);
        String requestId = SubmissionService.requireRequestId(required(values, "requestId", 64));
        String requestHash = SubmissionService.hash(JsonSupport.encode(json, input));
        Map<String, Object> previous = mapper.request(fields("competitionId", id, "requestId", requestId));
        if (previous != null) {
            if (!requestHash.equals(previous.get("requestHash"))) throw WebProblem.conflict("请求标识已用于不同结果。");
            return batch(actor, id, number(previous, "id"));
        }
        validateMode(input, competition);

        // 第 1 步 持有赛事锁时重算完整的发布内容，并校验同一份有效预览。
        Draft draft = compute(competition, input);
        if (!draft.problems().isEmpty()) throw WebProblem.conflict(String.join(" ", draft.problems()));
        String token = required(values, "previewToken", 120);
        long expires = expires(token);
        if (expires <= competitions.now().toEpochSecond(ZoneOffset.UTC) || !MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8), sign(actor, competition, input, draft, expires).getBytes(StandardCharsets.UTF_8))) {
            throw new WebProblem(409, "STALE_PREVIEW", "预览已过期或数据有变动，请重新核对。");
        }
        int version = input.baseId() == null ? 1 : (int) number(find(id, input.baseId()), "versionNo") + 1;
        Map<String, Object> batch = fields("competitionId", id, "versionNo", version, "actorId", actor.id(),
                "now", competitions.now(), "reason", input.reason(), "mode", mode, "noAwards", input.noAwards(),
                "requestId", requestId, "requestHash", requestHash, "basis", JsonSupport.encode(json, draft.basis()));

        // 第 2 步 批次、结果快照及当前指针一并提交。
        mapper.insertBatch(batch);
        long publicationId = number(batch, "id");
        for (Map<String, Object> row : draft.results()) {
            row.put("publicationId", publicationId);
            row.put("competitionId", id);
            row.put("recipientNamesJson", JsonSupport.encode(json, row.get("recipientNames")));
            mapper.insertEntry(row);
        }
        mapper.selectBatch(fields("competitionId", id, "publicationId", publicationId));
        competitions.mark(actor, "RESULT_" + mode, id, fields("publicationId", publicationId, "reason", input.reason()));
        return batch(actor, id, publicationId);
    }

    public List<Map<String, Object>> batches(Actor actor, long id) {
        competitions.checkManager(actor, competitions.require(id));
        List<Map<String, Object>> rows = mapper.batches(id);
        rows.forEach(ResultService::formatBatch);
        return rows;
    }

    public Map<String, Object> batch(Actor actor, long id, long publicationId) {
        competitions.checkManager(actor, competitions.require(id));
        Map<String, Object> row = find(id, publicationId);
        formatBatch(row);
        List<Map<String, Object>> entries = mapper.entries(publicationId);
        for (Map<String, Object> entry : entries) {
            entry.put("recipientNames", JsonSupport.stringList(json, entry.get("recipientNames")));
        }
        row.put("items", entries);
        try {
            row.put("scoringBasis", json.readTree(text(row, "scoringBasis")));
        } catch (Exception exception) {
            throw new IllegalStateException("Stored scoring basis is invalid", exception);
        }
        return row;
    }

    private static void formatBatch(Map<String, Object> row) {
        row.put("publishedAt", withOffset(row, "publishedAt"));
        row.put("currentBatch", flag(row.get("currentBatch")));
        row.put("noAwards", flag(row.get("noAwards")));
    }

    private Map<String, Object> find(long id, long publicationId) {
        Map<String, Object> row = mapper.batch(fields("competitionId", id, "publicationId", publicationId));
        if (row == null) throw WebProblem.missing();
        return row;
    }

    private Draft compute(Map<String, Object> competition, Inputs input) {
        long id = number(competition, "id");
        List<Map<String, Object>> criteria = competitions.criteria(competition);
        int required = (int) number(competition, "requiredJudges");
        List<Map<String, Object>> results = new ArrayList<>();
        List<Map<String, Object>> basis = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        Map<Long, Map<String, Object>> previous = new HashMap<>();
        if (input.baseId() != null) {
            for (Map<String, Object> row : mapper.entries(input.baseId())) previous.put(number(row, "registrationId"), row);
        }
        if (competitions.now().isBefore(date(competition, "judgingClosesAt"))) problems.add("评审窗口尚未截止。");
        for (Map<String, Object> source : mapper.eligibleRegistrations(id)) {
            long registration = number(source, "registrationId");
            Map<String, Object> old = previous.getOrDefault(registration, Map.of());
            Map<String, Object> change = input.changes().getOrDefault(registration, Map.of());
            Map<String, Object> result = baseResult(source, old, change, input);
            List<Map<String, Object>> scores = source.get("lockedVersionId") == null ? List.of()
                    : reviewMapper.activeScores(number(source, "lockedVersionId"));
            if ("RANKED".equals(result.get("outcomeStatus"))) {
                if (scores.size() != required) problems.add(text(source, "registrationNo") + " 的有效评分未齐备。");
                else result.put("totalScore", total(scores, criteria));
            }
            results.add(result);
            basis.add(fields("registrationId", registration, "lockedVersionId", source.get("lockedVersionId"),
                    "outcomeStatus", result.get("outcomeStatus"), "scores", scores.stream().map(score ->
                            fields("assignmentId", score.get("assignmentId"), "rowVersion", score.get("rowVersion"),
                                    "criterionScores", reviews.readScores(score))).toList()));
        }
        if (input.changes().keySet().stream().anyMatch(key -> results.stream().noneMatch(row -> number(row, "registrationId") == key))) {
            throw WebProblem.invalid("结果调整中包含不属于本届的报名。");
        }
        rank(results);
        boolean hasAwards = results.stream().anyMatch(row -> row.get("awardName") != null);
        if (hasAwards == input.noAwards()) problems.add(hasAwards ? "已有拟获奖条目，不能同时选择无获奖。" : "请配置奖项或明确确认无获奖。");
        return new Draft(results, basis, problems);
    }

    private Map<String, Object> baseResult(Map<String, Object> source, Map<String, Object> old,
                                           Map<String, Object> change, Inputs input) {
        boolean missing = source.get("submissionId") == null;
        String status = missing ? "NO_WORK" : "EXCLUDED".equals(source.get("eligibilityStatus")) ? "EXCLUDED" : "RANKED";
        String reason = "EXCLUDED".equals(status) ? text(source, "eligibilityReason") : null;
        if (!old.isEmpty()) {
            status = text(old, "outcomeStatus");
            reason = (String) old.get("outcomeReason");
        }
        if (change.get("excludeReason") != null) {
            if (!"CORRECTION".equals(input.mode()) || missing) throw WebProblem.invalid("参评范围仅可在更正时按规则调整。");
            status = "EXCLUDED";
            reason = text(change, "excludeReason");
        } else if (Boolean.TRUE.equals(change.get("restore"))) {
            if (!"CORRECTION".equals(input.mode()) || missing || !"EXCLUDED".equals(status)) {
                throw WebProblem.invalid("该报名不满足恢复参评的条件。");
            }
            status = "RANKED";
            reason = null;
        }
        if (!"RANKED".equals(status) && change.get("awardName") != null && !"".equals(change.get("awardName"))) {
            throw WebProblem.invalid("未参与排名的报名不能授奖。");
        }
        Object award = change.containsKey("awardName") ? change.get("awardName") : old.get("awardName");
        boolean awarded = "RANKED".equals(status) && award != null && !award.toString().isBlank();
        Object names = awarded
                ? change.getOrDefault("recipientNames", JsonSupport.stringList(json, old.get("recipientNames")))
                : List.of();
        String display = text(source, "teamDisplayName");
        if (display.isBlank()) display = missing ? text(source, "registrationNo") : text(source, "anonymousCode");
        return fields("registrationId", source.get("registrationId"), "registrationNo", source.get("registrationNo"),
                "workTitle", change.getOrDefault("workTitle", old.getOrDefault("workTitle", source.get("title"))),
                "entryDisplay", change.getOrDefault("entryDisplay", old.getOrDefault("entryDisplay", display)),
                "outcomeStatus", status, "outcomeReason", reason, "totalScore", null, "rankNo", null,
                "awardName", awarded ? award : null, "recipientNames", names);
    }

    // 每位评委先按冻结权重汇总，最后一次性对平均分保留两位小数。
    private BigDecimal total(List<Map<String, Object>> scores, List<Map<String, Object>> criteria) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Map<String, Object> row : scores) {
            Map<String, BigDecimal> values = reviews.validateScores(reviews.readScores(row), criteria, true);
            for (Map<String, Object> criterion : criteria) {
                BigDecimal weight = new BigDecimal(text(criterion, "weight")).movePointLeft(2);
                sum = sum.add(values.get(text(criterion, "id")).multiply(weight));
            }
        }
        return sum.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
    }

    // 使用竞赛排名。相同两位小数分数共享名次，例如 1、1、3。
    private static void rank(List<Map<String, Object>> results) {
        Comparator<BigDecimal> scores = Comparator.nullsLast(Comparator.reverseOrder());
        results.sort(Comparator.comparing((Map<String, Object> row) -> (BigDecimal) row.get("totalScore"), scores)
                .thenComparingLong(row -> number(row, "registrationId")));
        BigDecimal previous = null;
        int rank = 0;
        for (int index = 0; index < results.size(); index++) {
            Map<String, Object> row = results.get(index);
            BigDecimal score = (BigDecimal) row.get("totalScore");
            if (score == null) break;
            if (!score.equals(previous)) rank = index + 1;
            row.put("rankNo", rank);
            row.put("totalScoreText", score.toPlainString());
            previous = score;
        }
    }

    private Inputs inputs(Map<String, ?> values, String mode) {
        if (!Set.of("FIRST", "CORRECTION").contains(mode)) throw WebProblem.invalid("结果发布模式无效。");
        boolean correction = "CORRECTION".equals(mode);
        Object noAwards = values.get("noAwards");
        if (noAwards != null && !(noAwards instanceof Boolean)) throw WebProblem.invalid("无获奖标识格式有误。");
        return new Inputs(mode, correction ? number(values, "currentPublicationId") : null,
                Boolean.TRUE.equals(noAwards), correction ? required(values, "reason", 400) : null, edits(values.get("awards")));
    }

    private Map<Long, Map<String, Object>> edits(Object raw) {
        Map<Long, Map<String, Object>> rows = new TreeMap<>();
        if (raw == null) return rows;
        if (!(raw instanceof List<?> list)) throw WebProblem.invalid("拟奖项和调整须为列表。");
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> edit)) throw WebProblem.invalid("结果调整格式有误。");
            Map<String, Object> values = fields();
            edit.forEach((key, value) -> values.put(String.valueOf(key), value));
            Map<String, Object> change = fields();
            if (values.containsKey("awardName")) change.put("awardName", optional(values, "awardName", 60));
            if (values.containsKey("workTitle")) change.put("workTitle", required(values, "workTitle", 120));
            if (values.containsKey("entryDisplay")) change.put("entryDisplay", required(values, "entryDisplay", 80));
            if (values.containsKey("recipientNames")) change.put("recipientNames", publicNames(values.get("recipientNames")));
            if (values.containsKey("excludeReason")) change.put("excludeReason", required(values, "excludeReason", 400));
            if (Boolean.TRUE.equals(values.get("restore"))) change.put("restore", true);
            if (rows.putIfAbsent(number(values, "registrationId"), change) != null) throw WebProblem.invalid("同一报名只能调整一次。");
        }
        return rows;
    }

    private static List<String> publicNames(Object source) {
        if (!(source instanceof List<?> list) || list.size() > 20) {
            throw WebProblem.invalid("公开获奖姓名须为列表，最多填写 20 位。");
        }
        Set<String> names = new LinkedHashSet<>();
        for (Object item : list) {
            if (!(item instanceof String name) || name.isBlank() || name.strip().length() > 60) {
                throw WebProblem.invalid("公开获奖姓名须为 1 至 60 个字符。");
            }
            names.add(name.strip());
        }
        return List.copyOf(names);
    }

    private static void validateMode(Inputs input, Map<String, ?> competition) {
        if (!Set.of("PUBLISHED", "ARCHIVED").contains(text(competition, "status"))) throw WebProblem.conflict("赛事当前不可发布结果。");
        Long current = competition.get("currentPublicationId") == null ? null : number(competition, "currentPublicationId");
        if (!java.util.Objects.equals(current, input.baseId())) throw WebProblem.conflict("当前结果批次已变化。");
    }

    private String sign(Actor actor, Map<String, ?> competition, Inputs input, Draft draft, long expires) {
        String payload = JsonSupport.encode(json, fields("actor", actor.id(), "id", competition.get("id"), "version",
                competition.get("rowVersion"), "expires", expires, "input", input, "results", draft.results(), "basis", draft.basis()));
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(previewKey, "HmacSHA256"));
            return expires + "." + HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not sign result preview", exception);
        }
    }

    private static long expires(String token) {
        if (!token.matches("[0-9]{1,12}\\.[a-f0-9]{64}")) return 0;
        return Long.parseLong(token.substring(0, token.indexOf('.')));
    }
}
