package org.jingxu.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jingxu.mapper.RegistrationMapper;
import org.jingxu.pojo.Actor;
import org.jingxu.util.JsonSupport;
import org.jingxu.util.WebProblem;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jingxu.util.Data.*;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class RegistrationService {
    private final RegistrationMapper mapper;
    private final CompetitionService competitions;
    private final AccountService accounts;
    private final ObjectMapper json;

    public RegistrationService(RegistrationMapper mapper, CompetitionService competitions, AccountService accounts, ObjectMapper json) {
        this.mapper = mapper;
        this.competitions = competitions;
        this.accounts = accounts;
        this.json = json;
    }

    public Map<String, Object> studentList(Actor actor, Map<String, ?> values) {
        Long competition = text(values, "competitionId").isBlank() ? null : number(values, "competitionId");
        return list(competition, actor.id(), false, values);
    }

    public Map<String, Object> adminList(Actor actor, long competitionId, Map<String, ?> values) {
        competitions.checkManager(actor, competitions.require(competitionId));
        return list(competitionId, null, true, values);
    }

    private Map<String, Object> list(Long competitionId, Long studentId, boolean admin, Map<String, ?> values) {
        int page = queryNumber(values, "page", 1, 100_000);
        int size = queryNumber(values, "size", 20, 100);
        Map<String, Object> query = fields("competitionId", competitionId, "studentId", studentId, "admin", admin,
                "status", optional(values, "status", 20), "registrationNo", optional(values, "registrationNo", 32),
                "q", optional(values, "q", 120), "size", size, "offset", (page - 1) * size);
        List<Map<String, Object>> rows = mapper.list(query);
        for (Map<String, Object> row : rows) {
            if (row.containsKey("hasResult")) row.put("hasResult", flag(row.get("hasResult")));
            for (String field : List.of("registrationClosesAt", "correctionClosesAt", "submissionClosesAt", "submittedAt")) {
                row.put(field, withOffset(row, field));
            }
        }
        return page(rows, page, size, mapper.count(query));
    }

    public Map<String, Object> studentDetail(Actor actor, long id) {
        Map<String, Object> registration = require(id);
        requireMember(actor, registration);
        return detail(registration);
    }

    public Map<String, Object> adminDetail(Actor actor, long id) {
        Map<String, Object> registration = require(id);
        competitions.checkManager(actor, competitions.require(number(registration, "competitionId")));
        if ("DRAFT".equals(registration.get("status"))) throw WebProblem.missing();
        return detail(registration);
    }

    private Map<String, Object> detail(Map<String, Object> registration) {
        long id = number(registration, "id");
        registration.put("submittedAt", withOffset(registration, "submittedAt"));
        List<Map<String, Object>> roster = mapper.members(id);
        roster.forEach(member -> {
            member.remove("accountStatus");
            member.remove("studentRole");
        });
        registration.put("members", roster.stream().filter(member -> !"REMOVED".equals(member.get("invitationStatus"))).toList());
        List<Map<String, Object>> history = mapper.history(id);
        for (Map<String, Object> event : history) {
            event.put("createdAt", withOffset(event, "createdAt"));
            if (event.get("rosterSnapshot") instanceof String snapshot) {
                try {
                    event.put("rosterSnapshot", json.readValue(snapshot, new TypeReference<List<Map<String, Object>>>() {}));
                } catch (Exception exception) {
                    throw new IllegalStateException("Stored roster snapshot is invalid", exception);
                }
            }
        }
        registration.put("history", history);
        return registration;
    }

    public Map<String, Object> create(Actor actor, long competitionId, Map<String, ?> values) {
        Map<String, Object> competition = competitions.lock(competitionId);
        LocalDateTime now = competitions.now();
        competitions.requireWindow(competition, "registrationOpensAt", "registrationClosesAt", now);
        accounts.requireVerified(actor.id());
        String name = "TEAM".equals(competition.get("entryMode")) ? required(values, "teamDisplayName", 80) : null;
        Map<String, Object> row = fields("registrationNo", identifier("REG-"), "competitionId", competitionId,
                "captainId", actor.id(), "teamDisplayName", name);
        mapper.insert(row);
        mapper.addMember(fields("id", row.get("id"), "studentId", actor.id(), "captain", true,
                "status", "CONFIRMED", "confirmedAt", now));
        return studentDetail(actor, number(row, "id"));
    }

    public Map<String, Object> rename(Actor actor, long id, Map<String, ?> values) {
        Map<String, Object> competition = competitions.lock(number(require(id), "competitionId"));
        Map<String, Object> row = lock(id);
        requireCaptain(actor, row);
        requireTeam(competition);
        checkEditable(row, competition, competitions.now());
        matchVersion(row, values);
        mapper.rename(fields("id", id, "name", required(values, "teamDisplayName", 80)));
        return studentDetail(actor, id);
    }

    public void delete(Actor actor, long id) {
        Map<String, Object> competition = competitions.lock(number(require(id), "competitionId"));
        Map<String, Object> row = lock(id);
        requireCaptain(actor, row);
        competitions.requireOpen(competition);
        if (!"DRAFT".equals(row.get("status"))) throw WebProblem.conflict("仅未正式提交的报名草稿可以删除。");
        mapper.purgeDraftMembers(id);
        mapper.deleteDraft(id);
    }

    public List<Map<String, Object>> invitations(Actor actor) {
        return mapper.invitations(actor.id());
    }

    public Map<String, Object> invite(Actor actor, long id, Map<String, ?> values) {
        Map<String, Object> competition = competitions.lock(number(require(id), "competitionId"));
        Map<String, Object> row = lock(id);
        requireCaptain(actor, row);
        requireTeam(competition);
        checkEditable(row, competition, competitions.now());
        accounts.requireVerified(actor.id());
        Long student = mapper.inviteRecipient(required(values, "inviteCode", 36));
        if (student == null || student == actor.id()) throw WebProblem.invalid("该邀请码不可用。");
        Map<String, Object> previous = member(id, student);
        if (previous != null && Set.of("INVITED", "CONFIRMED").contains(text(previous, "invitationStatus"))) {
            throw WebProblem.conflict("该成员已在报名名单中。");
        }
        Map<String, Object> valuesToWrite = fields("id", id, "studentId", student, "captain", false,
                "status", "INVITED", "confirmedAt", null);
        if (previous == null) mapper.addMember(valuesToWrite);
        else mapper.changeMember(valuesToWrite);
        mapper.bump(id);
        return studentDetail(actor, id);
    }

    public void removeMember(Actor actor, long id, long studentId) {
        Map<String, Object> competition = competitions.lock(number(require(id), "competitionId"));
        Map<String, Object> row = lock(id);
        requireCaptain(actor, row);
        checkEditable(row, competition, competitions.now());
        if (studentId == actor.id() || mapper.changeMember(fields("id", id, "studentId", studentId,
                "status", "REMOVED", "confirmedAt", null)) != 1) throw WebProblem.missing();
        mapper.bump(id);
    }

    public Map<String, Object> answer(Actor actor, long id, Map<String, ?> values) {
        Map<String, Object> competition = competitions.lock(number(require(id), "competitionId"));
        Map<String, Object> row = lock(id);
        Map<String, Object> member = member(id, actor.id());
        if (member == null || !"INVITED".equals(member.get("invitationStatus"))) throw WebProblem.missing();
        LocalDateTime now = competitions.now();
        checkEditable(row, competition, now);
        String decision = required(values, "decision", 12);
        if (!Set.of("ACCEPT", "DECLINE").contains(decision)) throw WebProblem.invalid("邀请决定无效。");
        boolean accepted = "ACCEPT".equals(decision);
        if (accepted) accounts.requireVerified(actor.id());
        String next = accepted ? "CONFIRMED" : "DECLINED";
        mapper.changeMember(fields("id", id, "studentId", actor.id(), "status", next, "confirmedAt", accepted ? now : null));
        mapper.bump(id);
        return fields("registrationId", id, "invitationStatus", next);
    }

    public Map<String, Object> submit(Actor actor, long id, Map<String, ?> values, boolean resubmission) {
        long competitionId = number(require(id), "competitionId");
        Map<String, Object> competition = competitions.lock(competitionId);
        Map<String, Object> row = lock(id);
        requireCaptain(actor, row);
        LocalDateTime now = competitions.now();
        String expected = resubmission ? "CHANGES_REQUESTED" : "DRAFT";
        if (!expected.equals(row.get("status"))) throw WebProblem.conflict("报名当前无法提交。");
        checkEditable(row, competition, now);
        matchVersion(row, values);

        // 第 1 步 仅将已确认且身份有效的参赛成员提交审核。
        List<Map<String, Object>> roster = mapper.members(id).stream()
                .filter(member -> "CONFIRMED".equals(member.get("invitationStatus"))).toList();
        if (roster.size() < number(competition, "minTeamSize") || roster.size() > number(competition, "maxTeamSize")
                || roster.stream().anyMatch(member -> !"ACTIVE".equals(member.get("accountStatus"))
                || !"VERIFIED".equals(member.get("verificationStatus")) || !flag(member.get("studentRole")))) {
            throw WebProblem.conflict("确认人数或成员身份不满足参赛条件。");
        }

        // 第 2 步 同一事务替换资格占用并保存快照。任一重复占用使整笔变更回滚。
        mapper.release(id);
        try {
            for (Map<String, Object> member : roster) {
                mapper.claim(fields("competitionId", competitionId, "studentId", member.get("studentId"), "id", id, "now", now));
            }
        } catch (DuplicateKeyException exception) {
            throw new WebProblem(409, "DUPLICATE_PARTICIPATION", "至少一名成员已在本届赛事中正式报名。");
        }
        List<Map<String, Object>> snapshot = roster.stream().map(member -> fields("studentId", member.get("studentId"),
                "fullName", member.get("fullName"), "isCaptain", member.get("isCaptain"))).toList();
        advance(row, actor.id(), "PENDING", null, JsonSupport.encode(json, snapshot), now, now);
        return studentDetail(actor, id);
    }

    public Map<String, Object> withdraw(Actor actor, long id, Map<String, ?> values) {
        Map<String, Object> competition = competitions.lock(number(require(id), "competitionId"));
        Map<String, Object> row = lock(id);
        requireCaptain(actor, row);
        matchVersion(row, values);
        LocalDateTime now = competitions.now();
        competitions.requireWindow(competition, "registrationOpensAt", "registrationClosesAt", now);
        if (!Set.of("PENDING", "CHANGES_REQUESTED").contains(text(row, "status"))) {
            throw WebProblem.conflict("当前报名不可撤回。");
        }
        advance(row, actor.id(), "WITHDRAWN", "报名负责人撤回", null, now, null);
        mapper.release(id);
        return studentDetail(actor, id);
    }

    public Map<String, Object> decide(Actor actor, long id, Map<String, ?> values) {
        long competitionId = number(require(id), "competitionId");
        Map<String, Object> competition = competitions.managedLock(actor, competitionId);
        Map<String, Object> row = lock(id);
        LocalDateTime now = competitions.now();
        competitions.requireWindow(competition, "registrationOpensAt", "qualificationClosesAt", now);
        if (!"PENDING".equals(row.get("status"))) throw WebProblem.conflict("仅待审核报名可以处理。");
        matchVersion(row, values);
        String next = switch (required(values, "decision", 12)) {
            case "APPROVE" -> "APPROVED";
            case "RETURN" -> "CHANGES_REQUESTED";
            case "REJECT" -> "REJECTED";
            default -> throw WebProblem.invalid("审核决定无效。");
        };
        if ("CHANGES_REQUESTED".equals(next) && !now.isBefore(date(competition, "correctionClosesAt"))) {
            throw WebProblem.conflict("补正窗口已关闭。");
        }
        String reason = "APPROVED".equals(next) ? optional(values, "reason", 400) : required(values, "reason", 400);
        advance(row, actor.id(), next, reason, null, now, null);
        if ("REJECTED".equals(next)) mapper.release(id);
        competitions.mark(actor, "REGISTRATION_REVIEWED", competitionId, fields("registrationId", id, "decision", next));
        return adminDetail(actor, id);
    }

    @Scheduled(initialDelay = 15000, fixedDelay = 60000)
    public void expireDue() {
        for (long competitionId : mapper.dueCompetitions(competitions.now())) {
            Map<String, Object> event = competitions.lock(competitionId);
            LocalDateTime now = competitions.now();
            if (!"PUBLISHED".equals(event.get("status")) || now.isBefore(date(event, "qualificationClosesAt"))) continue;
            for (Map<String, Object> row : mapper.unsettled(competitionId)) {
                advance(row, null, "EXPIRED", "资格审核截止时尚未通过", null, now, null);
                mapper.release(number(row, "id"));
            }
        }
    }

    public Map<String, Object> result(Actor actor, long id) {
        requireMember(actor, require(id));
        Map<String, Object> result = mapper.currentResult(id);
        if (result == null) throw WebProblem.missing();
        return result;
    }

    public Map<String, Object> require(long id) {
        Map<String, Object> row = mapper.find(id);
        if (row == null) throw WebProblem.missing();
        return row;
    }

    public Map<String, Object> lock(long id) {
        Map<String, Object> row = mapper.lock(id);
        if (row == null) throw WebProblem.missing();
        return row;
    }

    public void requireCaptain(Actor actor, Map<String, ?> row) {
        if (actor.id() != number(row, "captainId")) throw WebProblem.missing();
    }

    public void requireMember(Actor actor, Map<String, ?> row) {
        if (actor.id() == number(row, "captainId")) return;
        Map<String, Object> member = member(number(row, "id"), actor.id());
        if (member == null || !"CONFIRMED".equals(member.get("invitationStatus"))) throw WebProblem.missing();
    }

    private Map<String, Object> member(long id, long studentId) {
        return mapper.members(id).stream().filter(row -> number(row, "studentId") == studentId).findFirst().orElse(null);
    }

    private void checkEditable(Map<String, ?> row, Map<String, ?> competition, LocalDateTime now) {
        String end = switch (text(row, "status")) {
            case "DRAFT" -> "registrationClosesAt";
            case "CHANGES_REQUESTED" -> "correctionClosesAt";
            default -> throw WebProblem.conflict("当前报名记录不可编辑。");
        };
        competitions.requireWindow(competition, "registrationOpensAt", end, now);
    }

    private static void requireTeam(Map<String, ?> competition) {
        if (!"TEAM".equals(competition.get("entryMode"))) throw WebProblem.conflict("此赛事为个人赛。");
    }

    private void advance(Map<String, ?> row, Long actor, String next, String reason, String snapshot,
                         LocalDateTime now, LocalDateTime submitted) {
        long id = number(row, "id");
        mapper.advance(fields("id", id, "status", next, "submittedAt", submitted));
        mapper.log(fields("id", id, "actorId", actor, "from", row.get("status"), "to", next, "reason", reason,
                "snapshot", snapshot, "now", now));
    }

}
