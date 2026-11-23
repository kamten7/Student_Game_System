<%@ include file="shared/header.jspf" %><%-- 查看单份作品及其版本，管理评委分派和参评资格。 --%>
<div class="title-block between"><div><p class="muted"><a href="<c:url value='/admin/competitions/${event.id}/works'/>">作品与评委</a></p>
    <h1><c:out value="${work.anonymousCode}"/></h1><p class="subhead"><c:out value="${event.name}"/> · 每份作品需 ${event.requiredJudges} 份有效评分</p></div>
    <span class="tag">${labels[work.eligibilityStatus]}</span></div>
<div class="columns">
    <div class="stack">
        <section class="card"><h2>文件版本</h2><ul class="line-list">
            <c:forEach var="file" items="${work.versions}"><li class="between">
                <div><b>V${file.versionNo} · <c:out value="${file.title}"/></b><c:if test="${file.id eq work.lockedVersionId}"> <span class="tag">评审锁定</span></c:if>
                    <p class="muted"><time datetime="${file.submittedAt}">${file.submittedAt}</time></p></div>
                <a href="<c:url value='/api/v1/admin/submissions/${work.id}/versions/${file.versionNo}/file'/>">下载</a>
            </li></c:forEach>
        </ul></section>
        <section class="card"><h2>评委分派</h2>
            <c:if test="${empty assignments}"><p class="empty">还没有评委分派。</p></c:if>
            <ul class="line-list"><c:forEach var="task" items="${assignments}"><li>
                <div class="between"><strong><c:out value="${task.fullName}" default="${task.username}"/>
                    <c:if test="${not empty task.fullName}"><span class="muted">（<c:out value="${task.username}"/>）</span></c:if>
                </strong><span class="tag">${labels[task.status]}</span></div>
                <c:if test="${empty event.currentPublicationId and event.status eq 'PUBLISHED' and (task.status eq 'ASSIGNED' or task.status eq 'SUBMITTED')}">
                    <details><summary>${task.status eq 'ASSIGNED' ? '撤销分派' : '重开正式评分'}</summary>
                        <form class="form-stack" data-api="/admin/assignments/${task.id}/${task.status eq 'ASSIGNED' ? 'revoke' : 'reopen'}">
                            <label><span>操作原因</span><input name="reason" maxlength="400" required></label><div><button class="button secondary">确认</button></div>
                        </form>
                    </details>
                </c:if>
            </li></c:forEach></ul>
        </section>
    </div>
    <aside class="stack">
        <section class="card"><h2>分派评委</h2>
            <c:if test="${work.eligibilityStatus eq 'ELIGIBLE' and (event.phase eq 'AWAITING_JUDGING' or event.phase eq 'JUDGING')}">
                <form class="form-stack" data-api="/admin/submissions/${work.id}/assignments">
                    <label><span>候选评委</span><select name="judgeId" required><option value="">请选择</option>
                        <c:forEach var="judge" items="${candidates}"><option value="${judge.id}"><c:out value="${judge.fullName}" default="${judge.username}"/><c:if test="${not empty judge.fullName}">（<c:out value="${judge.username}"/>）</c:if></option></c:forEach>
                    </select></label><div><button class="button">确认分派</button></div>
                </form>
            </c:if>
            <p class="muted">作品提交截止后、评审截止前，可为符合参评条件的作品逐个分派评委。评委到评审开始后才能评分。</p>
        </section>
        <c:if test="${event.status eq 'PUBLISHED' and empty event.currentPublicationId}">
            <section class="card"><h2>参评决定</h2>
                <form class="form-stack" data-api="/admin/submissions/${work.id}/eligibility">
                    <input type="hidden" name="rowVersion" value="${work.rowVersion}">
                    <label><span>决定</span><select name="decision"><option value="EXCLUDE">取消参评</option><option value="RESTORE">恢复资格</option></select></label>
                    <label><span>规则依据及原因</span><textarea name="reason" maxlength="400" required></textarea></label>
                    <div><button class="button secondary">记录决定</button></div>
                </form>
            </section>
        </c:if>
    </aside>
</div>
<%@ include file="shared/footer.jspf" %>
