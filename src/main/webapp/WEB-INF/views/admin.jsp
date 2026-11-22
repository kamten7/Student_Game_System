<%@ include file="shared/header.jspf" %>
<div class="title-block between"><div><h1>赛事管理</h1><p class="subhead">创建赛事、处理资格审核和跟进办赛进度。</p></div>
    <a class="button" href="<c:url value='/admin/competitions/new'/>">创建赛事</a></div>
<section class="card">
    <h2>赛事列表</h2>
    <form method="get" class="actions">
        <label><span>搜索赛事</span><input type="search" name="q" placeholder="输入赛事名称" value="<c:out value='${param.q}'/>"></label>
        <button class="button secondary">查询</button>
    </form>
    <c:choose><c:when test="${empty events.items}"><p class="empty">当前没有符合条件的赛事。</p></c:when>
    <c:otherwise><div class="table-wrap"><table><thead><tr><th>赛事</th><th>编号</th><th>类型</th><th>阶段</th><th>操作</th></tr></thead><tbody>
    <c:forEach var="event" items="${events.items}"><tr>
        <td><c:out value="${event.name}"/></td><td class="mono"><c:out value="${event.competitionNo}"/></td>
        <td>${labels[event.entryMode]}</td><td><span class="tag">${labels[event.phase]}</span></td>
        <td><a href="<c:url value='/admin/competitions/${event.id}'/>">进入赛事 ↗</a></td>
    </tr></c:forEach>
    </tbody></table></div></c:otherwise></c:choose>
</section>
<%@ include file="shared/footer.jspf" %>
