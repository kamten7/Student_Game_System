<%@ include file="shared/header.jspf" %>
<div class="title-block"><h1>评审任务</h1><p class="subhead">查看匿名作品、评审窗口和评分进度。</p></div>
<section class="card"><h2>我的分派</h2>
    <form method="get" class="actions"><label><span>任务状态</span><select name="status">
        <option value="">全部</option><option value="ASSIGNED" ${param.status eq 'ASSIGNED' ? 'selected' : ''}>待评分</option>
        <option value="SUBMITTED" ${param.status eq 'SUBMITTED' ? 'selected' : ''}>已提交</option>
        <option value="RECUSED" ${param.status eq 'RECUSED' ? 'selected' : ''}>已回避</option>
    </select></label><button class="button secondary">查询</button></form>
    <c:if test="${empty assignments.items}"><p class="empty">当前没有评审任务。</p></c:if>
    <div class="table-wrap"><table><thead><tr><th>赛事</th><th>匿名编号</th><th>版本</th><th>截止时间</th><th>状态</th><th>操作</th></tr></thead><tbody>
        <c:forEach var="row" items="${assignments.items}"><tr>
            <td><c:out value="${row.competitionName}"/></td><td class="mono"><c:out value="${row.anonymousCode}"/></td><td>V${row.versionNo}</td>
            <td><time datetime="${row.judgingClosesAt}">${row.judgingClosesAt}</time></td><td><span class="tag">${labels[row.status]}</span></td>
            <td><c:if test="${row.eligibilityStatus eq 'ELIGIBLE' and (row.status eq 'ASSIGNED' or row.status eq 'SUBMITTED')}">
                <a href="<c:url value='/judge/assignments/${row.id}'/>">打开评审 ↗</a></c:if></td>
        </tr></c:forEach>
    </tbody></table></div>
</section>
<%@ include file="shared/footer.jspf" %>
