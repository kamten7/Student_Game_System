<%@ include file="shared/header.jspf" %>
<section class="hero home-hero">
    <span class="tag">校内作品竞赛</span>
    <h1>从校园问题出发<br><span class="hero-accent">给出你的方案</span></h1>
    <p>发现校内赛事，选择感兴趣的挑战，在这里组队报名、提交作品和查看结果。</p>
    <div class="actions">
        <a class="button light" href="#events">浏览赛事 ↗</a>
        <a class="button hero-secondary" href="<c:url value='/announcements'/>">查看获奖公告</a>
    </div>
</section>
<section id="events" class="page-pad">
    <h2>发现赛事</h2>
    <form class="search" method="get" action="<c:url value='/'/>">
        <input type="search" name="q" value="<c:out value='${query.q}'/>" placeholder="搜索赛事名称" aria-label="赛事名称">
        <select name="phase" aria-label="赛事阶段">
            <option value="">全部阶段</option>
            <c:forEach var="phase" items="${['UPCOMING','REGISTRATION','SUBMISSION','JUDGING','RESULTS','CANCELLED']}">
                <option value="${phase}" ${query.phase eq phase ? 'selected' : ''}>${labels[phase]}</option>
            </c:forEach>
        </select>
        <select name="mode" aria-label="参赛方式">
            <option value="">全部参赛方式</option>
            <option value="INDIVIDUAL" ${query.mode eq 'INDIVIDUAL' ? 'selected' : ''}>个人赛</option>
            <option value="TEAM" ${query.mode eq 'TEAM' ? 'selected' : ''}>团队赛</option>
        </select>
        <button class="button">搜索</button>
    </form>
    <c:if test="${empty events.items}"><p class="empty">当前没有符合条件的已发布赛事。</p></c:if>
    <div class="cards">
        <c:forEach var="event" items="${events.items}">
            <article class="card">
                <span class="tag">${labels[event.phase]}</span>
                <h3><c:out value="${event.name}"/></h3>
                <p class="muted"><c:out value="${event.edition}"/> · ${labels[event.entryMode]}</p>
                <div class="between">
                    <c:choose>
                        <c:when test="${event.hasAnnouncement}">
                            <a href="<c:url value='/competitions/${event.id}'/>">赛事详情</a>
                            <a href="<c:url value='/competitions/${event.id}/announcement'/>">查获奖名单 ↗</a>
                        </c:when>
                        <c:otherwise>
                            <span>报名截止 <time datetime="${event.registrationClosesAt}">${event.registrationClosesAt}</time></span>
                            <a href="<c:url value='/competitions/${event.id}'/>">查看 ↗</a>
                        </c:otherwise>
                    </c:choose>
                </div>
            </article>
        </c:forEach>
    </div>
    <c:if test="${events.total gt events.size}">
        <nav class="actions page-pad" aria-label="赛事分页">
            <c:forEach var="direction" items="${[-1,1]}">
                <c:if test="${events.page + direction gt 0 and (events.page + direction - 1) * events.size lt events.total}">
                    <c:url var="pageLink" value="/">
                        <c:param name="q" value="${query.q}"/><c:param name="mode" value="${query.mode}"/>
                        <c:param name="phase" value="${query.phase}"/><c:param name="page" value="${events.page + direction}"/>
                    </c:url>
                    <a class="button secondary" href="<c:out value='${pageLink}'/>">${direction lt 0 ? '上一页' : '下一页'}</a>
                </c:if>
            </c:forEach>
        </nav>
    </c:if>
    <p class="muted page-pad">赛事时间按中国标准时间（UTC+08:00）展示。</p>
</section>
<%@ include file="shared/footer.jspf" %>
