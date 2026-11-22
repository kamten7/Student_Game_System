<%@ include file="shared/header.jspf" %>
<div class="title-block"><h1>账户与校内核验</h1><p class="subhead">以授权校内依据核验学生身份，管理赛事角色。</p></div>
<section class="card"><h2>账户列表</h2>
    <form class="actions" method="get">
        <label><span>用户名</span><input name="q" type="search" value="<c:out value='${query.q}'/>"></label>
        <label><span>核验状态</span><select name="verificationStatus"><option value="">全部</option>
            <c:forEach var="status" items="${['PENDING','VERIFIED','REJECTED']}"><option value="${status}" ${query.verificationStatus eq status ? 'selected' : ''}>${labels[status]}</option></c:forEach>
        </select></label><button class="button secondary">查询</button>
    </form>
    <div class="table-wrap"><table><thead><tr><th>用户名</th><th>姓名</th><th>申报学号</th><th>核验</th><th>操作</th></tr></thead><tbody>
        <c:forEach var="row" items="${accounts.items}"><tr><td><c:out value="${row.username}"/></td><td><c:out value="${row.fullName}"/></td><td><c:out value="${row.claimedStudentNo}"/></td>
            <td><c:if test="${not empty row.verificationStatus}"><span class="tag">${labels[row.verificationStatus]}</span></c:if></td>
            <td><a href="<c:url value='/admin/accounts/${row.id}'/>">管理 ↗</a></td></tr></c:forEach>
    </tbody></table></div>
    <c:if test="${empty accounts.items}"><p class="empty">当前没有符合条件的账户。</p></c:if>
</section>
<%@ include file="shared/footer.jspf" %>
