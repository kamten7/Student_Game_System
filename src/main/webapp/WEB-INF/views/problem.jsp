<%@ include file="shared/header.jspf" %><%-- 显示请求失败原因并提供返回赛事大厅的入口。 --%>
<section class="card centered">
    <h1>无法继续此操作</h1>
    <p><c:out value="${message}"/></p>
    <a class="button" href="<c:url value='/'/>">返回赛事大厅</a>
</section>
<%@ include file="shared/footer.jspf" %>
