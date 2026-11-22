<%@ include file="shared/header.jspf" %>
<div class="title-block"><h1>账户与校内核验</h1><p class="subhead">管理账户和赛事角色，按校内依据核验学生身份。</p></div>
<details class="card page-pad">
    <summary>新增工作人员账号</summary>
    <form class="form-stack page-pad" data-api="/admin/accounts" data-next="/admin/accounts/{id}">
        <p class="notice" data-message role="alert" hidden></p>
        <div class="form-grid">
            <label><span>用户名</span><input name="username" minlength="3" maxlength="48" required></label>
            <label><span>姓名</span><input name="fullName" maxlength="60" required></label>
            <label><span>初始密码</span><input name="password" type="password" autocomplete="new-password" minlength="10" maxlength="72" required></label>
        </div>
        <fieldset>
            <legend>工作人员角色</legend>
            <div class="actions"><c:forEach var="role" items="${['JUDGE','ORGANIZER','ADMIN']}">
                <label><input type="checkbox" name="roles" value="${role}" data-array> ${labels[role]}</label>
            </c:forEach></div>
        </fieldset>
        <div><button class="button">创建账号</button></div>
    </form>
</details>
<section class="card"><h2>账户列表</h2>
    <form class="actions" method="get">
        <label><span>用户名</span><input name="q" type="search" value="<c:out value='${query.q}'/>"></label>
        <label><span>角色</span><select name="role"><option value="">全部角色</option>
            <c:forEach var="role" items="${['STUDENT','JUDGE','ORGANIZER','ADMIN']}">
                <option value="${role}" ${query.role eq role ? 'selected' : ''}>${labels[role]}</option>
            </c:forEach>
        </select></label>
        <label><span>核验状态</span><select name="verificationStatus"><option value="">全部</option>
            <c:forEach var="status" items="${['PENDING','VERIFIED','REJECTED']}"><option value="${status}" ${query.verificationStatus eq status ? 'selected' : ''}>${labels[status]}</option></c:forEach>
        </select></label><button class="button secondary">查询</button>
    </form>
    <div class="table-wrap"><table><thead><tr><th>用户名</th><th>姓名</th><th>角色</th><th>申报学号</th><th>学生核验</th><th>操作</th></tr></thead><tbody>
        <c:forEach var="row" items="${accounts.items}"><tr>
            <td><c:out value="${row.username}"/></td>
            <td><c:choose>
                <c:when test="${empty row.fullName}"><span class="muted">未填写</span></c:when>
                <c:otherwise><c:out value="${row.fullName}"/></c:otherwise>
            </c:choose></td>
            <td><div class="actions">
                <c:forEach var="role" items="${row.roles}"><span class="tag">${labels[role]}</span></c:forEach>
                <c:if test="${empty row.roles}"><span class="muted">未分配</span></c:if>
            </div></td>
            <td><c:out value="${row.claimedStudentNo}" default="—"/></td>
            <td><c:choose>
                <c:when test="${empty row.verificationStatus}"><span class="muted">不适用</span></c:when>
                <c:otherwise><span class="tag">${labels[row.verificationStatus]}</span></c:otherwise>
            </c:choose></td>
            <td><a href="<c:url value='/admin/accounts/${row.id}'/>">管理 ↗</a></td></tr></c:forEach>
    </tbody></table></div>
    <c:if test="${empty accounts.items}"><p class="empty">当前没有符合条件的账户。</p></c:if>
</section>
<%@ include file="shared/footer.jspf" %>
