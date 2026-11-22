package org.jingxu.util;

/** 保存可安全返回给客户端的错误状态、代码和消息。 */
public class WebProblem extends RuntimeException {
    private final int status;
    private final String code;

    /** 构造带 HTTP 状态和业务错误代码的异常。 */
    public WebProblem(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    /** 返回应发送给客户端的 HTTP 状态码。 */
    public int status() { return status; }
    /** 返回用于区分失败类型的业务错误代码。 */
    public String code() { return code; }

    /** 构造输入不符合规则的错误。 */
    public static WebProblem invalid(String message) {
        return new WebProblem(400, "INVALID_REQUEST", message);
    }

    /** 构造需要先登录的访问错误。 */
    public static WebProblem unauthenticated() {
        return new WebProblem(401, "SIGN_IN_REQUIRED", "请先登录。");
    }

    /** 构造当前账号权限不足的错误。 */
    public static WebProblem forbidden() {
        return new WebProblem(403, "FORBIDDEN", "该操作需要相应权限。");
    }

    /** 构造资源不存在或不可见的错误。 */
    public static WebProblem missing() {
        return new WebProblem(404, "NOT_FOUND", "该记录不存在或当前不可访问。");
    }

    /** 构造与当前业务状态冲突的错误。 */
    public static WebProblem conflict(String message) {
        return new WebProblem(409, "STATE_CONFLICT", message);
    }
}
