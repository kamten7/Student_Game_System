package org.jingxu.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.jingxu.filter.AccessInterceptor;
import org.jingxu.pojo.Actor;
import org.jingxu.service.AccountService;
import org.jingxu.util.WebProblem;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static org.jingxu.filter.AccessInterceptor.actor;
import static org.jingxu.util.Data.*;

/** 处理账号认证、学生资料和平台账号管理请求。 */
@RestController
@RequestMapping("/api/v1")
public class AccountApiController {
    private final AccountService accounts;

    /** 接入账户服务处理登录和管理操作。 */
    public AccountApiController(AccountService accounts) {
        this.accounts = accounts;
    }

    /** 返回当前会话的表单校验令牌。 */
    @GetMapping("/auth/csrf")
    public Map<String, Object> csrf(HttpServletRequest request) {
        return fields("csrfToken", AccessInterceptor.token(request));
    }

    /** 为公开注册请求创建待核验的学生账号。 */
    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> register(@RequestBody Map<String, Object> values) {
        return fields("id", accounts.register(values), "verificationStatus", "PENDING");
    }

    /** 验证凭据后建立会话并返回各端入口。 */
    @PostMapping("/auth/login")
    public Map<String, Object> login(HttpServletRequest request, @RequestBody Map<String, Object> values) {
        Actor user = accounts.login(values);
        AccessInterceptor.signIn(request, user);
        return fields("id", user.id(), "roles", user.roles(), "home", user.home(), "csrfToken", AccessInterceptor.token(request));
    }

    /** 结束当前登录会话。 */
    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        AccessInterceptor.signOut(request);
    }

    /** 读取当前登录账号及关联资料。 */
    @GetMapping("/me")
    public Map<String, Object> profile(HttpServletRequest request) {
        return accounts.profile(actor(request));
    }

    /** 保存学生本人允许修改的资料。 */
    @PatchMapping("/me/profile")
    public Map<String, Object> update(HttpServletRequest request, @RequestBody Map<String, Object> values) {
        return accounts.updateProfile(student(request), values);
    }

    /** 为已核验学生重置组队邀请码。 */
    @PostMapping("/me/invite-code/rotate")
    public Map<String, Object> rotate(HttpServletRequest request) {
        return fields("inviteCode", accounts.rotateInvite(student(request)));
    }

    /** 按用户名、角色或核验状态分页查询账号。 */
    @GetMapping("/admin/accounts")
    public Map<String, Object> accounts(@RequestParam Map<String, String> query) {
        return accounts.list(query);
    }

    /** 读取平台管理员有权查看的账号详情。 */
    @GetMapping("/admin/accounts/{id}")
    public Map<String, Object> account(@PathVariable long id) {
        return accounts.adminAccount(id);
    }

    /** 创建不需要学号的工作人员账号。 */
    @PostMapping("/admin/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createStaff(HttpServletRequest request, @RequestBody Map<String, Object> values) {
        return accounts.createStaff(actor(request), values);
    }

    /** 按指定原因启用或停用账号。 */
    @PatchMapping("/admin/accounts/{id}")
    public Map<String, Object> updateAccount(HttpServletRequest request, @PathVariable long id,
                                             @RequestBody Map<String, Object> values) {
        return accounts.state(actor(request), id, required(values, "status", 12), required(values, "reason", 400));
    }

    /** 逻辑删除账号并保留其历史关联。 */
    @DeleteMapping("/admin/accounts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(HttpServletRequest request, @PathVariable long id) {
        accounts.state(actor(request), id, "DELETED", "平台管理员删除账户");
    }

    /** 按照账号身份校验并保存角色组合。 */
    @PutMapping("/admin/accounts/{id}/roles")
    public Map<String, Object> roles(HttpServletRequest request, @PathVariable long id,
                                     @RequestBody Map<String, Object> values) {
        return accounts.changeRoles(actor(request), id, values.get("roles"));
    }

    /** 保存工作人员的姓名。 */
    @PutMapping("/admin/accounts/{id}/name")
    public Map<String, Object> name(HttpServletRequest request, @PathVariable long id,
                                    @RequestBody Map<String, Object> values) {
        return accounts.changeStaffName(actor(request), id, values);
    }

    /** 更新账号的登录用户名。 */
    @PutMapping("/admin/accounts/{id}/username")
    public Map<String, Object> username(HttpServletRequest request, @PathVariable long id,
                                        @RequestBody Map<String, Object> values) {
        return accounts.changeUsername(actor(request), id, values);
    }

    /** 记录学生身份核验决定及依据。 */
    @PostMapping("/admin/accounts/{id}/student-verification")
    public Map<String, Object> verify(HttpServletRequest request, @PathVariable long id,
                                      @RequestBody Map<String, Object> values) {
        return accounts.verify(actor(request), id, values);
    }

    /** 分页查询关键操作审计记录。 */
    @GetMapping("/admin/audit-logs")
    public Map<String, Object> audit(@RequestParam Map<String, String> query) {
        return accounts.audits(query);
    }

    /** 确认当前请求由学生账号发起。 */
    private static Actor student(HttpServletRequest request) {
        Actor actor = actor(request);
        if (!actor.has("STUDENT")) throw WebProblem.forbidden();
        return actor;
    }
}
