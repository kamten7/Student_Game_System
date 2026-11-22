<%@ include file="shared/header.jspf" %>
<div class="title-block between"><div><p class="muted"><a href="<c:url value='/admin/accounts'/>">账户与核验</a></p><h1><c:out value="${account.username}"/></h1></div><span class="tag">${labels[account.status]}</span></div>
<div class="columns">
    <c:if test="${not empty account.verificationStatus}"><section class="card"><h2>学生身份核验</h2>
        <p>申报学号 <strong><c:out value="${account.claimedStudentNo}"/></strong> · ${labels[account.verificationStatus]}</p>
        <form class="form-stack" data-api="/admin/accounts/${account.id}/student-verification">
            <p class="notice" data-message role="alert" hidden></p>
            <label><span>决定</span><select name="decision"><option value="APPROVE">核验通过</option><option value="REJECT">核验拒绝</option></select></label>
            <label><span>已核验姓名</span><input name="fullName" value="<c:out value='${account.fullName}'/>" maxlength="60" required></label>
            <label><span>正式学号</span><input name="studentNo" value="<c:out value='${account.studentNo}'/>" maxlength="32"></label>
            <label><span>依据摘要</span><textarea name="reason" maxlength="400" required></textarea></label>
            <button class="button">保存核验决定</button>
        </form>
    </section></c:if>
    <aside class="stack">
        <section class="card"><h2>授权角色</h2>
            <form class="form-stack" data-api="/admin/accounts/${account.id}/roles" data-method="PUT">
                <div class="actions"><c:forEach var="name" items="${['STUDENT','JUDGE','ORGANIZER','ADMIN']}">
                    <label><input type="checkbox" name="roles" value="${name}" data-array ${account.roles.contains(name) ? 'checked' : ''}>
                    ${name eq 'STUDENT' ? '学生' : name eq 'JUDGE' ? '评委' : name eq 'ORGANIZER' ? '负责人' : '平台管理员'}</label>
                </c:forEach></div>
                <button class="button secondary">保存角色</button>
            </form>
        </section>
        <section class="card"><h2>账户状态</h2>
            <form class="form-stack" data-api="/admin/accounts/${account.id}" data-method="PATCH">
                <label><span>状态</span><select name="status"><option value="ACTIVE">正常</option><option value="DISABLED" ${account.status eq 'DISABLED' ? 'selected' : ''}>停用</option></select></label>
                <label><span>操作原因</span><input name="reason" maxlength="400" required></label><button class="button secondary">更新状态</button>
            </form>
            <form data-api="/admin/accounts/${account.id}" data-method="DELETE" data-next="/admin/accounts">
                <button class="button danger" data-confirm="确定停用并逻辑删除这个账户？">删除账户</button>
            </form>
        </section>
    </aside>
</div>
<%@ include file="shared/footer.jspf" %>
