<%@ include file="shared/header.jspf" %><%-- 展示赛事管理详情，提供发布、赛程调整和报名审核入口。 --%>
<div class="title-block between">
    <div><p class="muted"><a href="<c:url value='/admin'/>">赛事管理</a> / <c:out value="${event.competitionNo}"/></p>
    <h1><c:out value="${event.name}"/></h1><p class="subhead"><c:out value="${event.edition}"/> · ${labels[event.entryMode]}</p></div>
    <span class="tag">${labels[event.phase]}</span>
</div>
<c:choose>
    <c:when test="${event.status eq 'DRAFT'}">
        <div class="actions page-pad">
            <a class="button secondary" href="<c:url value='/admin/competitions/${event.id}/edit'/>">编辑草稿</a>
            <form data-api="/admin/competitions/${event.id}/publish"><input type="hidden" name="rowVersion" value="${event.rowVersion}">
                <button class="button" data-confirm="发布后冻结赛制及评分规则。确定发布？">发布赛事</button>
            </form>
            <form data-api="/admin/competitions/${event.id}" data-method="DELETE" data-next="/admin">
                <button class="button danger" data-confirm="确定删除赛事草稿？">删除草稿</button>
            </form>
        </div>
    </c:when>
    <c:otherwise>
        <div class="actions page-pad">
            <a class="button secondary" href="<c:url value='/competitions/${event.id}'/>">查看公开页 ↗</a>
            <a class="button secondary" href="<c:url value='/admin/competitions/${event.id}/works'/>">作品与评委</a>
            <c:if test="${event.status eq 'PUBLISHED' or event.status eq 'ARCHIVED'}">
                <a class="button secondary" href="<c:url value='/admin/competitions/${event.id}/results'/>">结果发布</a>
            </c:if>
            <c:if test="${event.status eq 'PUBLISHED' and not empty event.currentPublicationId}">
                <form data-api="/admin/competitions/${event.id}/archive"><button class="button secondary" data-confirm="归档本届赛事？">归档</button></form>
            </c:if>
        </div>
    </c:otherwise>
</c:choose>
<div class="stat-grid">
    <div class="card"><span class="muted">正式报名</span><strong>${event.registrations}</strong></div>
    <div class="card"><span class="muted">审核通过</span><strong>${event.approved}</strong></div>
    <div class="card"><span class="muted">有效交件</span><strong>${event.submissions}</strong></div>
    <div class="card"><span class="muted">评分齐备</span><strong>${event.scored}</strong></div>
</div>
<c:if test="${event.status eq 'PUBLISHED' and empty event.currentPublicationId}">
    <details class="card page-pad"><summary>调整赛程或取消赛事</summary>
        <div class="columns">
            <form class="form-stack" data-api="/admin/competitions/${event.id}/schedule-extension">
                <input type="hidden" name="rowVersion" value="${event.rowVersion}">
                <label><span>延期阶段</span><select name="stage"><option value="registration">报名</option><option value="correction">补正</option><option value="qualification">资格审核</option><option value="submission">作品提交</option><option value="judging">评审</option></select></label>
                <label><span>新截止时间（UTC+08:00）</span><input type="datetime-local" name="newDeadline" data-utc required></label>
                <label><span>延期理由</span><input name="reason" maxlength="400" required></label>
                <div><button class="button secondary">保存延期</button></div>
            </form>
            <form class="form-stack" data-api="/admin/competitions/${event.id}/cancel">
                <label><span>取消原因</span><textarea name="reason" maxlength="400" required></textarea></label>
                <div><button class="button danger" data-confirm="取消后参赛业务将停止写入。确定取消？">取消赛事</button></div>
            </form>
        </div>
    </details>
</c:if>
<section class="card">
    <h2>报名资格审核</h2>
    <form class="actions" method="get">
        <label><span>报名编号</span><input name="registrationNo" value="<c:out value='${query.registrationNo}'/>" maxlength="32"></label>
        <label><span>状态</span><select name="status">
            <option value="">全部状态</option><c:forEach var="status" items="${['PENDING','CHANGES_REQUESTED','APPROVED','REJECTED','WITHDRAWN','EXPIRED']}">
                <option value="${status}" ${query.status eq status ? 'selected' : ''}>${labels[status]}</option>
            </c:forEach>
        </select></label>
        <button class="button secondary">查询</button>
    </form>
    <c:choose><c:when test="${empty registrations.items}"><p class="empty">当前没有符合条件的正式报名。</p></c:when>
    <c:otherwise><div class="table-wrap"><table><thead><tr><th>报名编号</th><th>队伍</th><th>状态</th><th>操作</th></tr></thead><tbody>
        <c:forEach var="row" items="${registrations.items}"><tr><td class="mono"><c:out value="${row.registrationNo}"/></td><td><c:out value="${row.teamDisplayName}" default="个人报名"/></td>
            <td><span class="tag">${labels[row.status]}</span></td><td><a href="<c:url value='/admin/registrations/${row.id}'/>">查看 ↗</a></td></tr></c:forEach>
    </tbody></table></div></c:otherwise></c:choose>
</section>
<%@ include file="shared/footer.jspf" %>
