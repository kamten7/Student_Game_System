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

@RestController
@RequestMapping("/api/v1")
public class AccountApiController {
    private final AccountService accounts;

    public AccountApiController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/auth/csrf")
    public Map<String, Object> csrf(HttpServletRequest request) {
        return fields("csrfToken", AccessInterceptor.token(request));
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> register(@RequestBody Map<String, Object> values) {
        return fields("id", accounts.register(values), "verificationStatus", "PENDING");
    }

    @PostMapping("/auth/login")
    public Map<String, Object> login(HttpServletRequest request, @RequestBody Map<String, Object> values) {
        Actor user = accounts.login(values);
        AccessInterceptor.signIn(request, user);
        return fields("id", user.id(), "roles", user.roles(), "home", user.home(), "csrfToken", AccessInterceptor.token(request));
    }

    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        AccessInterceptor.signOut(request);
    }

    @GetMapping("/me")
    public Map<String, Object> profile(HttpServletRequest request) {
        return accounts.profile(actor(request));
    }

    @PatchMapping("/me/profile")
    public Map<String, Object> update(HttpServletRequest request, @RequestBody Map<String, Object> values) {
        return accounts.updateProfile(student(request), values);
    }

    @PostMapping("/me/invite-code/rotate")
    public Map<String, Object> rotate(HttpServletRequest request) {
        return fields("inviteCode", accounts.rotateInvite(student(request)));
    }

    @GetMapping("/admin/accounts")
    public Map<String, Object> accounts(@RequestParam Map<String, String> query) {
        return accounts.list(query);
    }

    @GetMapping("/admin/accounts/{id}")
    public Map<String, Object> account(@PathVariable long id) {
        return accounts.adminAccount(id);
    }

    @PatchMapping("/admin/accounts/{id}")
    public Map<String, Object> updateAccount(HttpServletRequest request, @PathVariable long id,
                                             @RequestBody Map<String, Object> values) {
        return accounts.state(actor(request), id, required(values, "status", 12), required(values, "reason", 400));
    }

    @DeleteMapping("/admin/accounts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(HttpServletRequest request, @PathVariable long id) {
        accounts.state(actor(request), id, "DELETED", "平台管理员删除账户");
    }

    @PutMapping("/admin/accounts/{id}/roles")
    public Map<String, Object> roles(HttpServletRequest request, @PathVariable long id,
                                     @RequestBody Map<String, Object> values) {
        return accounts.changeRoles(actor(request), id, values.get("roles"));
    }

    @PostMapping("/admin/accounts/{id}/student-verification")
    public Map<String, Object> verify(HttpServletRequest request, @PathVariable long id,
                                      @RequestBody Map<String, Object> values) {
        return accounts.verify(actor(request), id, values);
    }

    @GetMapping("/admin/audit-logs")
    public Map<String, Object> audit(@RequestParam Map<String, String> query) {
        return accounts.audits(query);
    }

    private static Actor student(HttpServletRequest request) {
        Actor actor = actor(request);
        if (!actor.has("STUDENT")) throw WebProblem.forbidden();
        return actor;
    }
}
