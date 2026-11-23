<%@ include file="shared/header.jspf" %><%-- 列出已公开的获奖公告及其查看入口。 --%>
<section class="publication-masthead">
    <h1>获奖公告</h1>
    <p>查阅各届赛事已公布的获奖作品、参赛团队和获奖人员。</p>
</section>
<div class="publication-list">
    <c:if test="${empty announcements.items}"><p class="empty">这一页暂无获奖公告。</p></c:if>
    <c:forEach var="notice" items="${announcements.items}">
        <article class="card publication-card">
            <header class="between publication-card-head">
                <div>
                    <h2><a href="<c:url value='/competitions/${notice.competitionId}/announcement'/>"><c:out value="${notice.name}"/></a></h2>
                    <p class="muted">${labels[notice.entryMode]} · <time datetime="${notice.publishedAt}">${notice.publishedAt}</time> 发布</p>
                </div>
                <a class="button secondary" href="<c:url value='/competitions/${notice.competitionId}/announcement'/>">查看完整公告 ↗</a>
            </header>
            <c:set var="awards" value="${notice.awards}"/>
            <%@ include file="shared/award-list.jspf" %>
        </article>
    </c:forEach>
</div>
<c:if test="${announcements.total gt announcements.size}">
    <nav class="actions page-pad" aria-label="公告分页">
        <c:if test="${announcements.page gt 1}">
            <a class="button secondary" href="<c:url value='/announcements?page=${announcements.page - 1}'/>">上一页</a>
        </c:if>
        <c:if test="${announcements.page * announcements.size lt announcements.total}">
            <a class="button secondary" href="<c:url value='/announcements?page=${announcements.page + 1}'/>">下一页</a>
        </c:if>
    </nav>
</c:if>
<%@ include file="shared/footer.jspf" %>
