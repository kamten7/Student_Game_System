package org.jingxu.util;

public class WebProblem extends RuntimeException {
    private final int status;
    private final String code;

    public WebProblem(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int status() { return status; }
    public String code() { return code; }

    public static WebProblem invalid(String message) {
        return new WebProblem(400, "INVALID_REQUEST", message);
    }

    public static WebProblem unauthenticated() {
        return new WebProblem(401, "SIGN_IN_REQUIRED", "请先登录。");
    }

    public static WebProblem forbidden() {
        return new WebProblem(403, "FORBIDDEN", "该操作需要相应权限。");
    }

    public static WebProblem missing() {
        return new WebProblem(404, "NOT_FOUND", "该记录不存在或当前不可访问。");
    }

    public static WebProblem conflict(String message) {
        return new WebProblem(409, "STATE_CONFLICT", message);
    }
}
