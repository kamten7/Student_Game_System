package org.jingxu.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jingxu.pojo.Actor;
import org.jingxu.service.AccountService;
import org.jingxu.util.WebProblem;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import java.util.UUID;

public class AccessInterceptor implements HandlerInterceptor {
    private static final String ACCOUNT = "accountId";
    private static final String TOKEN = "csrfToken";
    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");
    private final AccountService accounts;

    public AccessInterceptor(AccountService accounts) {
        this.accounts = accounts;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Object mapping = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String path = mapping == null ? request.getServletPath() : mapping.toString();
        boolean api = path.startsWith("/api/v1");
        String resource = api ? path.substring("/api/v1".length()) : path;
        HttpSession session = request.getSession(false);
        Actor actor = session != null && session.getAttribute(ACCOUNT) instanceof Long id ? accounts.resolve(id) : null;
        if (session != null && session.getAttribute(ACCOUNT) != null && actor == null) {
            session.invalidate();
        }

        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "same-origin");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Content-Security-Policy", "default-src 'self'; frame-src 'self' blob:; "
                + "frame-ancestors 'none'; object-src 'none'; base-uri 'self'; form-action 'self'");
        if (actor == null && protectedRoute(resource)) {
            if (api) throw WebProblem.unauthenticated();
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }
        if (actor != null) {
            request.setAttribute("actor", actor);
            if ((within(resource, "/student") && !actor.has("STUDENT"))
                    || (within(resource, "/judge") && !actor.has("JUDGE"))
                    || (within(resource, "/admin") && !actor.managesEvents())
                    || (within(resource, "/admin/accounts") || within(resource, "/admin/audit")
                        || within(resource, "/admin/audit-logs")) && !actor.has("ADMIN")) {
                throw WebProblem.forbidden();
            }
        }
        if (!READ_METHODS.contains(request.getMethod())) {
            String supplied = request.getHeader("X-CSRF-Token");
            if (supplied == null) supplied = request.getParameter("_csrf");
            HttpSession current = request.getSession(false);
            String expected = current == null ? null : (String) current.getAttribute(TOKEN);
            if (supplied == null || expected == null || !MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
                throw new WebProblem(403, "INVALID_CSRF", "表单已过期，请刷新页面后重试。");
            }
        }
        return true;
    }

    private static boolean protectedRoute(String resource) {
        return within(resource, "/student") || within(resource, "/judge") || within(resource, "/admin")
                || within(resource, "/me") || resource.equals("/auth/logout");
    }

    private static boolean within(String path, String base) {
        return path.equals(base) || path.startsWith(base + "/");
    }

    public static Actor actor(HttpServletRequest request) {
        Actor actor = (Actor) request.getAttribute("actor");
        if (actor == null) throw WebProblem.unauthenticated();
        return actor;
    }

    public static String token(HttpServletRequest request) {
        HttpSession session = request.getSession();
        synchronized (session) {
            String token = (String) session.getAttribute(TOKEN);
            if (token != null) return token;
            token = newToken();
            session.setAttribute(TOKEN, token);
            return token;
        }
    }

    public static void signIn(HttpServletRequest request, Actor actor) {
        HttpSession session = request.getSession();
        request.changeSessionId();
        session.setAttribute(ACCOUNT, actor.id());
        session.setAttribute(TOKEN, newToken());
    }

    public static void signOut(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
    }

    private static String newToken() {
        return UUID.randomUUID().toString() + UUID.randomUUID();
    }
}
