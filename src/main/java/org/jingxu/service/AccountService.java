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

/** 管理账号认证、学生档案和工作人员授权。 */
@Service
public class AccountService {
    private static final Set<String> ROLES = Set.of("STUDENT", "JUDGE", "ORGANIZER", "ADMIN");
    private final AccountMapper mapper;
    private final BCryptPasswordEncoder passwords;
    private final Clock clock;
    private final ObjectMapper json;
    private final String dummyHash;

    /** 接入账号数据、密码摘要、时钟和 JSON 序列化。 */
    public AccountService(AccountMapper mapper, BCryptPasswordEncoder passwords, Clock clock, ObjectMapper json) {
        this.mapper = mapper;
        this.passwords = passwords;
        this.clock = clock;
        this.json = json;
        this.dummyHash = passwords.encode(UUID.randomUUID().toString());
    }

    /** 按部署配置创建首个可用的工作人员管理员。 */
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

    /** 在同一事务中建立学生账号、档案和学生角色。 */
    @Transactional
    public long register(Map<String, ?> values) {
        String claimed = optional(values, "claimedStudentNo", 32);
        if (claimed.isEmpty()) throw WebProblem.invalid("请填写申报学号。");
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

    /** 校验登录名允许的字符和长度。 */
    private static String validateUsername(String username) {
        if (!username.matches("[\\p{L}\\p{N}_.-]{3,48}")) {
            throw WebProblem.invalid("用户名须为 3 至 48 个字符，可使用字母、数字、点、下划线和连字符。");
        }
        return username;
    }

    /** 校验密码长度并保存密码摘要，返回新建账号。 */
    private Map<String, Object> createAccount(String username, String password) {
        validateUsername(username);
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

    /** 校验凭据并解析当前有效账号和角色。 */
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

    /** 返回统一的凭据错误，避免暴露账号存在性。 */
    private static WebProblem invalidCredentials() {
        return new WebProblem(401, "INVALID_CREDENTIALS", "用户名或密码错误。");
    }

    /** 按账号状态和有效角色重建登录身份。 */
    public Actor resolve(long id) {
        Map<String, Object> row = mapper.activeAccount(id);
        return row == null ? null : new Actor(id, text(row, "username"), Set.copyOf(mapper.roles(id)));
    }

    /** 读取当前账号资料并附上会话角色。 */
    public Map<String, Object> profile(Actor actor) {
        Map<String, Object> row = requireAccount(actor.id());
        row.put("roles", actor.roles());
        return row;
    }

    /** 保存学生可编辑字段，保护已核验姓名。 */
    @Transactional
    public Map<String, Object> updateProfile(Actor actor, Map<String, ?> values) {
        Map<String, Object> current = requireAccount(actor.id());
        mapper.updateProfile(fields("id", actor.id(),
                "fullName", values.containsKey("fullName") ? required(values, "fullName", 60) : current.get("fullName"),
                "department", values.containsKey("department") ? optional(values, "department", 80) : current.get("department"),
                "major", values.containsKey("major") ? optional(values, "major", 80) : current.get("major")));
        return profile(actor);
    }

    /** 为已核验学生生成新的组队邀请码。 */
    @Transactional
    public String rotateInvite(Actor actor) {
        requireVerified(actor.id());
        String code = UUID.randomUUID().toString().replace("-", "");
        mapper.rotateInvite(fields("id", actor.id(), "inviteCode", code));
        return code;
    }

    /** 要求账号仍然可用且已经完成学号核验。 */
    public Map<String, Object> requireVerified(long id) {
        Map<String, Object> row = requireAccount(id);
        if (!"VERIFIED".equals(row.get("verificationStatus")) || !"ACTIVE".equals(row.get("status"))) {
            throw WebProblem.conflict("请完成校内身份核验后再参与赛事。");
        }
        return row;
    }

    /** 读取账号，缺失时返回资源不存在。 */
    public Map<String, Object> requireAccount(long id) {
        Map<String, Object> row = mapper.account(id);
        if (row == null) throw WebProblem.missing();
        return row;
    }

    /** 组装管理端详情，排除组队邀请码。 */
    public Map<String, Object> adminAccount(long id) {
        Map<String, Object> row = requireAccount(id);
        row.remove("inviteCode");
        row.put("roles", mapper.roles(id));
        return row;
    }

    /** 分页查询账号并将有效角色转为列表。 */
    public Map<String, Object> list(Map<String, ?> values) {
        int page = queryNumber(values, "page", 1, 100_000);
        int size = queryNumber(values, "size", 20, 100);
        Map<String, Object> query = fields("q", optional(values, "q", 48), "status", optional(values, "status", 12),
                "role", optional(values, "role", 16), "verificationStatus", optional(values, "verificationStatus", 12),
                "size", size, "offset", (page - 1) * size);
        List<Map<String, Object>> rows = mapper.listAccounts(query);
        for (Map<String, Object> row : rows) {
            String codes = text(row, "roleCodes");
            row.remove("roleCodes");
            row.put("roles", codes.isEmpty() ? List.of() : List.of(codes.split(",")));
        }
        return page(rows, page, size, mapper.countAccounts(query));
    }

    /** 原子保存工作人员账号、姓名和兼任角色。 */
    @Transactional
    public Map<String, Object> createStaff(Actor actor, Map<String, ?> values) {
        // 第 1 步 校验工作人员角色及姓名。
        Set<String> roles = requestedRoles(values.get("roles"));
        validateAccountRoles(false, roles);
        String fullName = required(values, "fullName", 60);
        // 第 2 步 在同一事务保存账号、角色和审计记录。
        mapper.lockAdminRole();
        long id = number(createAccount(required(values, "username", 48), text(values, "password")), "id");
        mapper.updateStaffName(fields("id", id, "fullName", fullName));
        for (String role : roles) mapper.addRole(fields("id", id, "role", role));
        audit(actor.id(), "ACCOUNT_STAFF_CREATED", "ACCOUNT", id, fields("roles", roles));
        return adminAccount(id);
    }

    /** 修改唯一登录名，保持原账号编号和业务关联。 */
    @Transactional
    public Map<String, Object> changeUsername(Actor actor, long id, Map<String, ?> values) {
        String username = validateUsername(required(values, "username", 48));
        mapper.lockAdminRole();
        Map<String, Object> target = requireAccount(id);
        if ("DELETED".equals(target.get("status"))) throw WebProblem.conflict("该账户已删除。");
        if (!username.equals(target.get("username"))) {
            try {
                mapper.updateUsername(fields("id", id, "username", username));
            } catch (DuplicateKeyException exception) {
                throw WebProblem.conflict("该用户名已被使用。");
            }
            audit(actor.id(), "ACCOUNT_USERNAME", "ACCOUNT", id, Map.of());
        }
        return adminAccount(id);
    }

    /** 修改工作人员姓名并追加操作审计。 */
    @Transactional
    public Map<String, Object> changeStaffName(Actor actor, long id, Map<String, ?> values) {
        String fullName = required(values, "fullName", 60);
        mapper.lockAdminRole();
        Map<String, Object> target = requireAccount(id);
        if ("DELETED".equals(target.get("status"))) throw WebProblem.conflict("该账户已删除。");
        if (target.get("verificationStatus") != null) throw WebProblem.conflict("学生姓名由身份核验维护。");
        if (!fullName.equals(target.get("fullName"))) {
            mapper.updateStaffName(fields("id", id, "fullName", fullName));
            audit(actor.id(), "ACCOUNT_NAME", "ACCOUNT", id, Map.of());
        }
        return adminAccount(id);
    }

    /** 变更账号状态并防止最后一个管理员被停用。 */
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

    /** 按账号档案限制角色组合，并记录最终授权。 */
    @Transactional
    public Map<String, Object> changeRoles(Actor actor, long id, Object requestedRoles) {
        Set<String> roles = requestedRoles(requestedRoles);
        mapper.lockAdminRole();
        Map<String, Object> target = requireAccount(id);
        validateAccountRoles(target.get("verificationStatus") != null, roles);
        if (!roles.contains("ADMIN")) protectAdmin(id, target);
        mapper.removeRoles(id);
        for (String role : roles) mapper.addRole(fields("id", id, "role", role));
        audit(actor.id(), "ACCOUNT_ROLES", "ACCOUNT", id, fields("roles", roles));
        return adminAccount(id);
    }

    /** 校验角色非空、合法并去重。 */
    private static Set<String> requestedRoles(Object selection) {
        if (!(selection instanceof Collection<?> names) || names.isEmpty()) {
            throw WebProblem.invalid("每个账户至少保留一个角色。");
        }
        Set<String> roles = new TreeSet<>();
        for (Object name : names) {
            if (!(name instanceof String role) || !ROLES.contains(role)) throw WebProblem.invalid("角色无效。");
            roles.add(role);
        }
        return roles;
    }

    /** 固定学生角色，工作人员可组合其余角色。 */
    private static void validateAccountRoles(boolean student, Set<String> roles) {
        if (student && !roles.equals(Set.of("STUDENT"))) {
            throw WebProblem.invalid("学生账号只能保留学生角色。");
        }
        if (!student && roles.contains("STUDENT")) {
            throw WebProblem.invalid("学生角色需要通过学生注册建立档案和申报学号。");
        }
    }

    /** 防止移除最后一个有效管理员的权限。 */
    private void protectAdmin(long id, Map<String, Object> account) {
        if ("ACTIVE".equals(account.get("status")) && mapper.roles(id).contains("ADMIN") && mapper.activeAdmins() <= 1) {
            throw WebProblem.conflict("至少保留一个有效的平台管理员。");
        }
    }

    /** 按核验依据绑定正式学号并记录决定。 */
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
        String studentNo = approved ? optional(values, "studentNo", 32) : null;
        if (approved && studentNo.isEmpty()) throw WebProblem.invalid("核验通过时必须填写正式学号。");
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

    /** 按对象、人员和时间窗口读取可读审计。 */
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

    /** 保存操作类型、对象和结构化摘要。 */
    public void audit(Long actorId, String action, String type, Object target, Map<String, Object> summary) {
        mapper.audit(fields("actorId", actorId, "action", action, "type", type, "target", target.toString(),
                "summary", JsonSupport.encode(json, summary), "now", now()));
    }

    /** 返回用于持久化的当前业务时刻。 */
    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
