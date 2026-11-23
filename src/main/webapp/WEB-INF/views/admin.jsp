<%@ include file="shared/header.jspf" %><%-- 展示可管理的赛事及其进度，并提供创建赛事入口。 --%>
<div class="title-block between"><div><h1>赛事管理</h1><p class="subhead">创建赛事、处理资格审核和跟进办赛进度。</p></div>
    <a class="button" href="<c:url value='/admin/competitions/new'/>">创建赛事</a></div>
<section class="card">
    <h2>赛事列表</h2>
    <form method="get" class="actions">
        <label><span>搜索赛事</span><input type="search" name="q" placeholder="输入赛事名称" value="<c:out value='${param.q}'/>"></label>
        <label><span>赛事阶段</span><select name="phase">
            <option value="">全部阶段</option>
            <c:forEach var="phase" items="${['DRAFT','UPCOMING','REGISTRATION','QUALIFICATION','AWAITING_SUBMISSION','SUBMISSION','AWAITING_JUDGING','JUDGING','AWAITING_RESULTS','RESULTS','CANCELLED']}">
                <option value="${phase}" ${param.phase eq phase ? 'selected' : ''}>${labels[phase]}</option>
            </c:forEach>
        </select></label>
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
    <c:if test="${events.total gt events.size}">
        <nav class="actions page-pad" aria-label="管理赛事分页">
            <c:forEach var="direction" items="${[-1,1]}">
                <c:if test="${events.page + direction gt 0 and (events.page + direction - 1) * events.size lt events.total}">
                    <c:url var="pageLink" value="/admin">
                        <c:param name="q" value="${param.q}"/><c:param name="phase" value="${param.phase}"/>
                        <c:param name="mode" value="${param.mode}"/><c:param name="size" value="${events.size}"/>
                        <c:param name="page" value="${events.page + direction}"/>
                    </c:url>
                    <a class="button secondary" href="<c:out value='${pageLink}'/>">${direction lt 0 ? '上一页' : '下一页'}</a>
                </c:if>
            </c:forEach>
            <span class="muted">第 ${events.page} 页 · 共 ${events.total} 场赛事</span>
        </nav>
    </c:if>
</section>
<%@ include file="shared/footer.jspf" %>
