package org.jingxu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jingxu.mapper.AccountMapper;
import org.jingxu.pojo.Actor;
import org.jingxu.util.AuditDisplay;
import org.jingxu.util.JsonSupport;
import org.jingxu.util.WebProblem;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import static org.jingxu.util.Data.*;

@Service
public class AccountService {
    private static final Set<String> ROLES = Set.of("STUDENT", "JUDGE", "ORGANIZER", "ADMIN");
    private final AccountMapper mapper;
    private final BCryptPasswordEncoder passwords;
    private final Clock clock;
    private final ObjectMapper json;
    private final String dummyHash;

    public AccountService(AccountMapper mapper, BCryptPasswordEncoder passwords, Clock clock, ObjectMapper json) {
        this.mapper = mapper;
        this.passwords = passwords;
        this.clock = clock;
        this.json = json;
        this.dummyHash = passwords.encode(UUID.randomUUID().toString());
    }

    @EventListener
    @Transactional
    public void bootstrapAdmin(ContextRefreshedEvent event) {
        if (event.getApplicationContext().getParent() != null) return;
        String username = System.getenv("STUDENT_ADMIN_USERNAME");
        String password = System.getenv("STUDENT_ADMIN_PASSWORD");
        if (username == null && password == null) return;
        if (username == null || password == null) {
            throw new IllegalStateException("Set both STUDENT_ADMIN_USERNAME and STUDENT_ADMIN_PASSWORD");
        }
        mapper.lockAdminRole();
        if (mapper.activeAdmins() > 0) return;
        Map<String, Object> account = createAccount(username, password);
        mapper.addRole(fields("id", account.get("id"), "role", "ADMIN"));
        audit(null, "BOOTSTRAP_ADMIN", "ACCOUNT", account.get("id"), Map.of());
    }

    @Transactional
    public long register(Map<String, ?> values) {
        String claimed = required(values, "claimedStudentNo", 32);
        String fullName = required(values, "fullName", 60);
        String department = optional(values, "department", 80);
        String major = optional(values, "major", 80);
        Map<String, Object> row = createAccount(required(values, "username", 48), text(values, "password"));
        long id = number(row, "id");
        mapper.insertProfile(fields("id", id, "claimedStudentNo", claimed, "fullName", fullName,
                "department", department, "major", major));
        mapper.addRole(fields("id", id, "role", "STUDENT"));
        return id;
    }

    private Map<String, Object> createAccount(String username, String password) {
        if (!username.matches("[\\p{L}\\p{N}_.-]{3,48}")) {
            throw WebProblem.invalid("用户名须为 3 至 48 个字符，可使用字母、数字、点、下划线和连字符。");
        }
        if (password.length() < 10 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw WebProblem.invalid("密码至少 10 个字符，最多 72 字节。");
        }
        Map<String, Object> row = fields("username", username, "hash", passwords.encode(password));
        try {
            mapper.insertAccount(row);
        } catch (DuplicateKeyException exception) {
            throw WebProblem.conflict("该用户名已被使用。");
        }
        return row;
    }

    public Actor login(Map<String, ?> values) {
        String username = required(values, "username", 48);
        String password = text(values, "password");
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) throw invalidCredentials();
        Map<String, Object> row = mapper.credentials(username);
        if (!passwords.matches(password, row == null ? dummyHash : text(row, "passwordHash")) || row == null) {
            throw invalidCredentials();
        }
        Actor actor = resolve(number(row, "id"));
        if (actor == null) throw invalidCredentials();
        return actor;
    }

    private static WebProblem invalidCredentials() {
        return new WebProblem(401, "INVALID_CREDENTIALS", "用户名或密码错误。");
    }

    public Actor resolve(long id) {
        Map<String, Object> row = mapper.activeAccount(id);
        return row == null ? null : new Actor(id, text(row, "username"), Set.copyOf(mapper.roles(id)));
    }

    public Map<String, Object> profile(Actor actor) {
        Map<String, Object> row = requireAccount(actor.id());
        row.put("roles", actor.roles());
        return row;
    }

    @Transactional
    public Map<String, Object> updateProfile(Actor actor, Map<String, ?> values) {
        Map<String, Object> current = requireAccount(actor.id());
        mapper.updateProfile(fields("id", actor.id(),
                "fullName", values.containsKey("fullName") ? required(values, "fullName", 60) : current.get("fullName"),
                "department", values.containsKey("department") ? optional(values, "department", 80) : current.get("department"),
                "major", values.containsKey("major") ? optional(values, "major", 80) : current.get("major")));
        return profile(actor);
    }

    @Transactional
    public String rotateInvite(Actor actor) {
        requireVerified(actor.id());
        String code = UUID.randomUUID().toString().replace("-", "");
        mapper.rotateInvite(fields("id", actor.id(), "inviteCode", code));
        return code;
    }

    public Map<String, Object> requireVerified(long id) {
        Map<String, Object> row = requireAccount(id);
        if (!"VERIFIED".equals(row.get("verificationStatus")) || !"ACTIVE".equals(row.get("status"))) {
            throw WebProblem.conflict("请完成校内身份核验后再参与赛事。");
        }
        return row;
    }

    public Map<String, Object> requireAccount(long id) {
        Map<String, Object> row = mapper.account(id);
        if (row == null) throw WebProblem.missing();
        return row;
    }

    public Map<String, Object> adminAccount(long id) {
        Map<String, Object> row = requireAccount(id);
        row.remove("inviteCode");
        row.put("roles", mapper.roles(id));
        return row;
    }

    public Map<String, Object> list(Map<String, ?> values) {
        int page = queryNumber(values, "page", 1, 100_000);
        int size = queryNumber(values, "size", 20, 100);
        Map<String, Object> query = fields("q", optional(values, "q", 48), "status", optional(values, "status", 12),
                "role", optional(values, "role", 16), "verificationStatus", optional(values, "verificationStatus", 12),
                "size", size, "offset", (page - 1) * size);
        return page(mapper.listAccounts(query), page, size, mapper.countAccounts(query));
    }

    @Transactional
    public Map<String, Object> state(Actor actor, long id, String state, String reason) {
        if (!Set.of("ACTIVE", "DISABLED", "DELETED").contains(state)) throw WebProblem.invalid("无效的账户状态。");
        mapper.lockAdminRole();
        Map<String, Object> target = requireAccount(id);
        if ("DELETED".equals(target.get("status")) && !"DELETED".equals(state)) throw WebProblem.conflict("该账户已删除。");
        if (!"ACTIVE".equals(state)) protectAdmin(id, target);
        mapper.updateAccount(fields("id", id, "status", state, "deletedAt", "DELETED".equals(state) ? now() : null));
        audit(actor.id(), "ACCOUNT_STATE", "ACCOUNT", id, fields("status", state, "reason", reason));
        return adminAccount(id);
    }

    @Transactional
    public Map<String, Object> changeRoles(Actor actor, long id, Object requestedRoles) {
        if (!(requestedRoles instanceof Collection<?> names)) throw WebProblem.invalid("请提供角色列表。");
        Set<String> roles = new TreeSet<>();
        for (Object name : names) {
            if (!(name instanceof String role) || !ROLES.contains(role)) throw WebProblem.invalid("角色无效。");
            roles.add(role);
        }
        mapper.lockAdminRole();
        Map<String, Object> target = requireAccount(id);
        if (roles.contains("STUDENT") && target.get("verificationStatus") == null) {
            throw WebProblem.invalid("学生角色需要学生档案。");
        }
        if (!roles.contains("ADMIN")) protectAdmin(id, target);
        mapper.removeRoles(id);
        for (String role : roles) mapper.addRole(fields("id", id, "role", role));
        audit(actor.id(), "ACCOUNT_ROLES", "ACCOUNT", id, fields("roles", roles));
        return adminAccount(id);
    }

    private void protectAdmin(long id, Map<String, Object> account) {
        if ("ACTIVE".equals(account.get("status")) && mapper.roles(id).contains("ADMIN") && mapper.activeAdmins() <= 1) {
            throw WebProblem.conflict("至少保留一个有效的平台管理员。");
        }
    }

    @Transactional
    public Map<String, Object> verify(Actor actor, long id, Map<String, ?> values) {
        String decision = required(values, "decision", 12);
        if (!Set.of("APPROVE", "REJECT").contains(decision)) throw WebProblem.invalid("无效的核验决定。");
        String reason = required(values, "reason", 400);
        mapper.lockAdminRole();
        Map<String, Object> target = requireAccount(id);
        if (target.get("verificationStatus") == null || !"ACTIVE".equals(target.get("status"))) throw WebProblem.missing();
        boolean approved = "APPROVE".equals(decision);
        if (!approved && "VERIFIED".equals(target.get("verificationStatus"))) {
            throw WebProblem.conflict("已绑定的正式身份需要受控修正。");
        }
        String studentNo = approved ? required(values, "studentNo", 32) : null;
        String fullName = values.containsKey("fullName") ? required(values, "fullName", 60) : text(target, "fullName");
        try {
            mapper.verifyProfile(fields("id", id, "studentNo", studentNo, "fullName", fullName,
                    "status", approved ? "VERIFIED" : "REJECTED", "actorId", actor.id(), "now", now(),
                    "inviteCode", approved ? UUID.randomUUID().toString().replace("-", "") : null));
        } catch (DuplicateKeyException exception) {
            throw WebProblem.conflict("正式学号已绑定其他账户。");
        }
        audit(actor.id(), "STUDENT_VERIFICATION", "ACCOUNT", id, fields("decision", decision, "reason", reason));
        return adminAccount(id);
    }

    public Map<String, Object> audits(Map<String, ?> values) {
        int page = queryNumber(values, "page", 1, 100_000);
        int size = queryNumber(values, "size", 20, 100);
        Map<String, Object> query = fields("type", optional(values, "targetType", 32),
                "target", optional(values, "targetKey", 64), "action", optional(values, "action", 48), "size", size,
                "offset", (page - 1) * size, "actorId", text(values, "actorId").isBlank() ? null : number(values, "actorId"),
                "from", text(values, "from").isBlank() ? null : utcInput(values, "from"),
                "to", text(values, "to").isBlank() ? null : utcInput(values, "to"));
        List<Map<String, Object>> records = mapper.listAudit(query);
        records.forEach(record -> AuditDisplay.describe(record, json));
        return page(records, page, size, mapper.countAudit(query));
    }

    public void audit(Long actorId, String action, String type, Object target, Map<String, Object> summary) {
        mapper.audit(fields("actorId", actorId, "action", action, "type", type, "target", target.toString(),
                "summary", JsonSupport.encode(json, summary), "now", now()));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
