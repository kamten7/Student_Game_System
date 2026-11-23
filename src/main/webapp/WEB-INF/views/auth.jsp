<%@ include file="shared/header.jspf" %><%-- 共用登录和学生注册页面，按入口显示对应表单。 --%>
<section class="auth">
    <div class="hero"><h1>${register ? '参赛从这里开始' : '回到你的工作台'}</h1><p>集中处理校内赛事、报名、作品和评审任务。</p></div>
    <div class="auth-form">
        <h2>${register ? '创建学生账户' : '登录竞序'}</h2>
        <form class="form-stack" data-api="/auth/${register ? 'register' : 'login'}" data-next="${register ? '/login?registered=1' : '@home'}">
            <p class="notice" data-message role="alert" hidden></p>
            <c:if test="${param.registered eq '1'}"><p class="notice ok">注册成功。登录后可以查看核验状态。</p></c:if>
            <label><span>用户名</span><input name="username" autocomplete="username" minlength="3" maxlength="48" required></label>
            <c:if test="${register}">
                <label><span>姓名</span><input name="fullName" autocomplete="name" maxlength="60" required></label>
                <label><span>申报学号</span><input name="claimedStudentNo" maxlength="32" required></label>
            </c:if>
            <label><span>密码</span><input type="password" name="password" autocomplete="${register ? 'new-password' : 'current-password'}"
                  minlength="${register ? 10 : 1}" maxlength="72" required></label>
            <button class="button">${register ? '创建账户' : '登录'}</button>
        </form>
        <c:choose>
            <c:when test="${register}">
                <p class="notice ok page-pad">提交申请后，由平台管理员核验身份并绑定正式学号。</p>
                <a href="<c:url value='/login'/>">已有账户，去登录</a>
            </c:when>
            <c:otherwise><p class="page-pad"><a href="<c:url value='/register'/>">创建学生账户</a></p></c:otherwise>
        </c:choose>
    </div>
</section>
<%@ include file="shared/footer.jspf" %>
