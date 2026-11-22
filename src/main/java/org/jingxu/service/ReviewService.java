package org.jingxu.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jingxu.mapper.ReviewMapper;
import org.jingxu.pojo.Actor;
import org.jingxu.pojo.PdfAttachment;
import org.jingxu.util.JsonSupport;
import org.jingxu.util.WebProblem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.jingxu.util.Data.*;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class ReviewService {
    private final ReviewMapper mapper;
    private final CompetitionService competitions;
    private final SubmissionService submissions;
    private final ObjectMapper json;

    public ReviewService(ReviewMapper mapper, CompetitionService competitions, SubmissionService submissions, ObjectMapper json) {
        this.mapper = mapper;
        this.competitions = competitions;
        this.submissions = submissions;
        this.json = json;
    }

    public Map<String, Object> judgeList(Actor actor, Map<String, ?> values) {
        int page = queryNumber(values, "page", 1, 100_000);
        int size = queryNumber(values, "size", 20, 100);
        Map<String, Object> query = fields("judgeId", actor.id(), "size", size, "offset", (page - 1) * size,
                "status", optional(values, "status", 12),
                "competitionId", text(values, "competitionId").isBlank() ? null : number(values, "competitionId"));
        List<Map<String, Object>> rows = mapper.judgeList(query);
        rows.forEach(row -> row.put("judgingClosesAt", withOffset(row, "judgingClosesAt")));
        return page(rows, page, size, mapper.judgeCount(query));
    }

    public Map<String, Object> judgeView(Actor actor, long id) {
        Map<String, Object> row = require(id);
        Map<String, Object> competition = competitions.require(number(row, "competitionId"));
        requireJudgeAccess(actor, row, competition);
        Map<String, Object> score = mapper.score(id);
        if (score != null) {
            score.put("criterionScores", readScores(score));
            score.put("submittedAt", withOffset(score, "submittedAt"));
        }
        LocalDateTime now = competitions.now();
        boolean canScore = "ASSIGNED".equals(row.get("status")) && "PUBLISHED".equals(competition.get("status"))
                && competition.get("currentPublicationId") == null
                && !now.isBefore(date(competition, "judgingOpensAt")) && now.isBefore(date(competition, "judgingClosesAt"));
        return fields("id", id, "anonymousCode", row.get("anonymousCode"), "versionNo", row.get("versionNo"),
                "status", row.get("status"), "competitionName", competition.get("name"), "score", score,
                "scoringRubric", competitions.criteria(competition), "canScore", canScore,
                "judgingClosesAt", withOffset(competition, "judgingClosesAt"));
    }

    public PdfAttachment judgeFile(Actor actor, long id) {
        Map<String, Object> assignment = require(id);
        requireJudgeAccess(actor, assignment, competitions.require(number(assignment, "competitionId")));
        return submissions.judgeFile(assignment);
    }

    public List<Map<String, Object>> candidates(Actor actor, long id) {
        competitions.checkManager(actor, competitions.require(id));
        return mapper.candidateList(id);
    }

    public List<Map<String, Object>> assignments(Actor actor, long submissionId) {
        Map<String, Object> work = submissions.require(submissionId);
        competitions.checkManager(actor, competitions.require(number(work, "competitionId")));
        return mapper.listForSubmission(submissionId);
    }

    public Map<String, Object> assign(Actor actor, long id, long judgeId) {
        long competitionId = number(submissions.require(id), "competitionId");
        Map<String, Object> competition = competitions.managedLock(actor, competitionId);
        LocalDateTime now = competitions.now();
        requireReviewOpen(competition);
        if (!now.isBefore(date(competition, "judgingClosesAt"))) throw WebProblem.conflict("当前评审已截止。");
        if (judgeId == actor.id() || mapper.candidateList(competitionId).stream().noneMatch(judge -> number(judge, "id") == judgeId)) {
            throw WebProblem.conflict("该账户当前不能评审本届赛事。");
        }
        Map<String, Object> submission = submissions.lockFinal(id, competition);
        if (!"ELIGIBLE".equals(submission.get("eligibilityStatus"))) throw WebProblem.conflict("该作品已取消参评。");
        long versionId = number(submission, "lockedVersionId");
        if (mapper.activeCount(versionId) >= number(competition, "requiredJudges")) throw WebProblem.conflict("该作品评委名额已满。");
        Map<String, Object> values = fields("versionId", versionId, "judgeId", judgeId, "now", now);
        Map<String, Object> existing = mapper.byJudgeAndVersion(values);
        if (existing == null) {
            mapper.insertAssignment(values);
        } else {
            if (!"REVOKED".equals(existing.get("status"))) throw WebProblem.conflict("该评委已有分派记录。");
            values.put("id", existing.get("id"));
            mapper.changeAssignment(fields("id", values.get("id"), "status", "ASSIGNED", "reason", null));
        }
        competitions.mark(actor, "JUDGE_ASSIGNED", competitionId, fields("assignmentId", values.get("id"), "judgeId", judgeId));
        return mapper.find(number(values, "id"));
    }

    public Map<String, Object> score(Actor actor, long id, Map<String, ?> values) {
        Map<String, Object> competition = competitions.lock(number(require(id), "competitionId"));
        Map<String, Object> assignment = mapper.lock(id);
        requireScoring(actor, assignment, competition);
        Map<String, Object> previous = mapper.score(id);
        if (previous == null) {
            if (integer(values, "rowVersion", 0, Integer.MAX_VALUE) != 0) throw WebProblem.conflict("评分已更新。");
        } else matchVersion(previous, values);
        Map<String, BigDecimal> scores = validateScores(values.get("criterionScores"), competitions.criteria(competition), false);
        Map<String, Object> saved = fields("id", id, "scores", JsonSupport.encode(json, scores), "feedback", optional(values, "feedback", 1200));
        if (previous == null) mapper.insertScore(saved);
        else mapper.saveScore(saved);
        return judgeView(actor, id);
    }

    public Map<String, Object> submit(Actor actor, long id, Map<String, ?> values) {
        Map<String, Object> competition = competitions.lock(number(require(id), "competitionId"));
        Map<String, Object> assignment = mapper.lock(id);
        requireScoring(actor, assignment, competition);
        Map<String, Object> score = mapper.score(id);
        if (score == null) throw WebProblem.conflict("请先保存评分草稿。");
        matchVersion(score, values);
        validateScores(readScores(score), competitions.criteria(competition), true);
        mapper.finishScore(fields("id", id, "now", competitions.now()));
        mapper.changeAssignment(fields("id", id, "status", "SUBMITTED", "reason", null));
        return judgeView(actor, id);
    }

    public Map<String, Object> recuse(Actor actor, long id, String reason) {
        Map<String, Object> competition = competitions.lock(number(require(id), "competitionId"));
        Map<String, Object> assignment = mapper.lock(id);
        requireJudgeAccess(actor, assignment, competition);
        requireReviewOpen(competition);
        requireAssigned(assignment);
        mapper.changeAssignment(fields("id", id, "status", "RECUSED", "reason", reason));
        return fields("id", id, "status", "RECUSED");
    }

    public Map<String, Object> revoke(Actor actor, long id, String reason) {
        long competitionId = number(require(id), "competitionId");
        Map<String, Object> competition = competitions.managedLock(actor, competitionId);
        Map<String, Object> assignment = mapper.lock(id);
        requireReviewOpen(competition);
        requireAssigned(assignment);
        mapper.changeAssignment(fields("id", id, "status", "REVOKED", "reason", reason));
        competitions.mark(actor, "JUDGE_REVOKED", competitionId, fields("assignmentId", id, "reason", reason));
        return mapper.find(id);
    }

    public Map<String, Object> reopen(Actor actor, long id, String reason) {
        long competitionId = number(require(id), "competitionId");
        Map<String, Object> competition = competitions.managedLock(actor, competitionId);
        requireReviewOpen(competition);
        Map<String, Object> assignment = mapper.lock(id);
        competitions.requireWindow(competition, "judgingOpensAt", "judgingClosesAt", competitions.now());
        if (!"SUBMITTED".equals(assignment.get("status")) || !"ELIGIBLE".equals(assignment.get("eligibilityStatus"))) {
            throw WebProblem.conflict("该评分当前不可重开。");
        }
        Map<String, Object> previous = mapper.score(id);
        competitions.mark(actor, "SCORE_REOPENED", competitionId, fields("assignmentId", id, "reason", reason,
                "criterionScores", readScores(previous), "feedback", previous.get("feedback"),
                "submittedAt", withOffset(previous, "submittedAt")));
        mapper.finishScore(fields("id", id, "now", null));
        mapper.changeAssignment(fields("id", id, "status", "ASSIGNED", "reason", reason));
        return mapper.find(id);
    }

    public Map<String, BigDecimal> readScores(Map<String, ?> row) {
        try {
            return json.readValue(text(row, "criterionScores"), new TypeReference<Map<String, BigDecimal>>() {});
        } catch (Exception exception) {
            throw new IllegalStateException("Stored score is invalid", exception);
        }
    }

    public Map<String, BigDecimal> validateScores(Object source, List<Map<String, Object>> criteria, boolean complete) {
        if (!(source instanceof Map<?, ?> input)) throw WebProblem.invalid("请填写评分。");
        Map<String, BigDecimal> scores = new TreeMap<>();
        for (var criterion : criteria) {
            String key = text(criterion, "id");
            Object value = input.get(key);
            if (value == null || value.toString().isBlank()) {
                if (complete) throw WebProblem.invalid("每项评分都须填写。");
                continue;
            }
            try {
                BigDecimal score = new BigDecimal(value.toString());
                if (score.signum() < 0 || score.compareTo(BigDecimal.valueOf(100)) > 0 || score.stripTrailingZeros().scale() > 2) {
                    throw WebProblem.invalid("评分须在 0 至 100 之间，最多两位小数。");
                }
                scores.put(key, score.stripTrailingZeros());
            } catch (NumberFormatException exception) {
                throw WebProblem.invalid("评分必须是数字。");
            }
        }
        if (input.keySet().stream().anyMatch(key -> criteria.stream().noneMatch(c -> c.get("id").equals(key)))) {
            throw WebProblem.invalid("存在未配置的评分维度。");
        }
        return scores;
    }

    private Map<String, Object> require(long id) {
        Map<String, Object> assignment = mapper.find(id);
        if (assignment == null) throw WebProblem.missing();
        return assignment;
    }

    private void requireScoring(Actor actor, Map<String, ?> assignment, Map<String, ?> competition) {
        requireJudgeAccess(actor, assignment, competition);
        requireReviewOpen(competition);
        requireAssigned(assignment);
        competitions.requireWindow(competition, "judgingOpensAt", "judgingClosesAt", competitions.now());
    }

    private static void requireAssigned(Map<String, ?> assignment) {
        if (!"ASSIGNED".equals(assignment.get("status"))) throw WebProblem.conflict("当前分派状态不可操作。");
    }

    private void requireReviewOpen(Map<String, ?> competition) {
        competitions.requireOpen(competition);
        if (competition.get("currentPublicationId") != null) throw WebProblem.conflict("该赛事评分已冻结。");
    }

    private static void requireJudgeAccess(Actor actor, Map<String, ?> row, Map<String, ?> competition) {
        if (actor.id() != number(row, "judgeId") || !Set.of("ASSIGNED", "SUBMITTED").contains(text(row, "status"))
                || !"ELIGIBLE".equals(row.get("eligibilityStatus")) || !row.get("versionId").equals(row.get("lockedVersionId"))
                || !Set.of("PUBLISHED", "ARCHIVED").contains(text(competition, "status"))) throw WebProblem.missing();
    }
}
