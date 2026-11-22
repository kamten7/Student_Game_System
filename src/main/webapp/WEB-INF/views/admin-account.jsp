<%@ include file="shared/header.jspf" %>
<div class="title-block between"><div><p class="muted"><a href="<c:url value='/admin/accounts'/>">账户与核验</a></p><h1><c:out value="${account.username}"/></h1></div><span class="tag">${labels[account.status]}</span></div>
<div class="columns">
    <c:if test="${not empty account.verificationStatus}"><section class="card"><h2>学生身份核验</h2>
        <p>申报学号 <strong><c:out value="${account.claimedStudentNo}"/></strong> · ${labels[account.verificationStatus]}</p>
        <form class="form-stack" data-api="/admin/accounts/${account.id}/student-verification" data-student-verification>
            <p class="notice" data-message role="alert" hidden></p>
            <label><span>决定</span><select name="decision"><option value="APPROVE">核验通过</option><option value="REJECT">核验拒绝</option></select></label>
            <label><span>已核验姓名</span><input name="fullName" value="<c:out value='${account.fullName}'/>" maxlength="60" required></label>
            <label><span>正式学号（核验通过时必填）</span><input name="studentNo" value="<c:out value='${account.studentNo}'/>" maxlength="32" required></label>
            <label><span>依据摘要</span><textarea name="reason" maxlength="400" required></textarea></label>
            <button class="button">保存核验决定</button>
        </form>
    </section></c:if>
    <c:if test="${empty account.verificationStatus}">
        <section class="card"><h2>账户姓名</h2>
            <c:choose>
                <c:when test="${account.status eq 'DELETED'}"><p><c:out value="${account.fullName}" default="未填写"/></p></c:when>
                <c:otherwise>
                    <form class="form-stack" data-api="/admin/accounts/${account.id}/name" data-method="PUT">
                        <p class="notice" data-message role="alert" hidden></p>
                        <label><span>姓名</span><input name="fullName" value="<c:out value='${account.fullName}'/>" maxlength="60" required></label>
                        <button class="button">保存姓名</button>
                    </form>
                </c:otherwise>
            </c:choose>
        </section>
    </c:if>
    <aside class="stack">
        <c:if test="${account.status ne 'DELETED'}">
            <section class="card"><h2>登录用户名</h2>
                <form class="form-stack" data-api="/admin/accounts/${account.id}/username" data-method="PUT">
                    <p class="notice" data-message role="alert" hidden></p>
                    <label><span>用户名</span><input name="username" value="<c:out value='${account.username}'/>" minlength="3" maxlength="48" required></label>
                    <button class="button secondary">保存用户名</button>
                </form>
            </section>
        </c:if>
        <section class="card"><h2>授权角色</h2>
            <form class="form-stack" data-api="/admin/accounts/${account.id}/roles" data-method="PUT">
                <p class="notice" data-message role="alert" hidden></p>
                <c:choose>
                    <c:when test="${not empty account.verificationStatus}">
                        <p><span class="tag">学生</span></p>
                        <p class="muted">学生账号固定为学生角色。</p>
                        <c:if test="${not account.roles.contains('STUDENT')}">
                            <input type="hidden" name="roles" value="STUDENT" data-array>
                            <button class="button secondary">恢复学生角色</button>
                        </c:if>
                    </c:when>
                    <c:otherwise>
                        <p class="muted">工作人员角色可兼任，至少选择一个。</p>
                        <div class="actions"><c:forEach var="name" items="${['JUDGE','ORGANIZER','ADMIN']}">
                            <label><input type="checkbox" name="roles" value="${name}" data-array ${account.roles.contains(name) ? 'checked' : ''}> ${labels[name]}</label>
                        </c:forEach></div>
                        <button class="button secondary">保存角色</button>
                    </c:otherwise>
                </c:choose>
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
