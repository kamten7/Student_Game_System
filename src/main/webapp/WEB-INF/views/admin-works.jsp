<%@ include file="shared/header.jspf" %>
<div class="title-block"><p class="muted"><a href="<c:url value='/admin/competitions/${event.id}'/>"><c:out value="${event.name}"/></a></p><h1>作品与评委</h1></div>
<section class="card"><h2>作品管理</h2>
    <form class="actions" method="get">
        <label><span>报名编号或匿名编号</span><input name="q" type="search" value="<c:out value='${param.q}'/>"></label>
        <label><span>参评状态</span><select name="state"><option value="">全部</option>
            <c:forEach var="state" items="${['NO_WORK','ELIGIBLE','EXCLUDED']}"><option value="${state}" ${param.state eq state ? 'selected' : ''}>${labels[state]}</option></c:forEach>
        </select></label><button class="button secondary">查询</button>
    </form>
    <c:if test="${empty works.items}"><p class="empty">当前没有符合条件的通过报名。</p></c:if>
    <div class="table-wrap"><table><thead><tr><th>报名编号</th><th>作品</th><th>参评状态</th><th>正式评分</th><th>操作</th></tr></thead><tbody>
        <c:forEach var="work" items="${works.items}"><tr>
            <td class="mono"><c:out value="${work.registrationNo}"/></td><td><c:out value="${work.latestTitle}" default="尚未交件"/></td>
            <td><span class="tag">${empty work.id ? '尚未交件' : labels[work.eligibilityStatus]}</span></td><td>${work.scored} / ${event.requiredJudges}</td>
            <td><c:if test="${not empty work.id}"><a href="<c:url value='/admin/submissions/${work.id}'/>">管理作品 ↗</a></c:if></td>
        </tr></c:forEach>
    </tbody></table></div>
</section>
<%@ include file="shared/footer.jspf" %>
