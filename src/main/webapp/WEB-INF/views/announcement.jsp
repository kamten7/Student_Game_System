<%@ include file="shared/header.jspf" %><%-- 展示一届赛事当前公布的结果和获奖名单。 --%>
<section class="publication-masthead">
    <nav class="publication-path" aria-label="面包屑"><a href="<c:url value='/announcements'/>">获奖公告</a><span> / ${labels[event.entryMode]}</span></nav>
    <h1><c:out value="${event.name}"/></h1>
    <p>第 ${announcement.versionNo} 版 · <time datetime="${announcement.publishedAt}">${announcement.publishedAt}</time> 发布</p>
</section>
<section class="card publication-card page-pad">
    <div class="between publication-card-head"><h2>获奖名单</h2><a href="<c:url value='/competitions/${event.id}'/>">赛事详情 ↗</a></div>
    <c:set var="awards" value="${announcement.awards}"/>
    <%@ include file="shared/award-list.jspf" %>
</section>
<%@ include file="shared/footer.jspf" %>
