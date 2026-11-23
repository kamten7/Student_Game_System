<%@ include file="shared/header.jspf" %><%-- 展示赛事规则和赛程，并按当前阶段提供报名入口。 --%>
<section class="hero">
    <span class="tag">${labels[event.phase]} · ${labels[event.entryMode]}</span>
    <h1><c:out value="${event.name}"/></h1>
    <p><c:out value="${event.edition}"/>
        <c:if test="${not event.hasAnnouncement}"> · 报名截止 <time datetime="${event.registrationClosesAt}">${event.registrationClosesAt}</time></c:if>
    </p>
    <div class="actions">
        <c:if test="${event.hasAnnouncement}"><a class="button light" href="<c:url value='/competitions/${event.id}/announcement'/>">查看获奖名单 ↗</a></c:if>
        <c:if test="${event.phase eq 'REGISTRATION' and empty actor}"><a class="button light" href="<c:url value='/login'/>">登录后报名</a></c:if>
        <c:if test="${event.phase eq 'REGISTRATION' and not empty actor and actor.has('STUDENT')}"><a class="button light" href="#register">开始报名</a></c:if>
    </div>
</section>
<div class="columns page-pad">
    <section class="card">
        <h2>赛事规则</h2><p class="rule-text"><c:out value="${event.rulesText}"/></p>
        <c:if test="${event.status eq 'CANCELLED'}"><p class="notice"><c:out value="${event.cancellationReason}"/></p></c:if>
    </section>
    <aside class="stack">
        <section class="card"><h2>赛程</h2>
            <ul class="line-list">
                <li class="between"><span>报名截止</span><time datetime="${event.registrationClosesAt}">${event.registrationClosesAt}</time></li>
                <li class="between"><span>资格审核截止</span><time datetime="${event.qualificationClosesAt}">${event.qualificationClosesAt}</time></li>
                <li class="between"><span>作品截止</span><time datetime="${event.submissionClosesAt}">${event.submissionClosesAt}</time></li>
                <li class="between"><span>评审截止</span><time datetime="${event.judgingClosesAt}">${event.judgingClosesAt}</time></li>
            </ul>
            <p class="muted">UTC+08:00</p>
        </section>
        <c:if test="${event.phase eq 'REGISTRATION' and not empty actor and actor.has('STUDENT')}">
            <section class="card" id="register"><h2>创建报名</h2>
                <form class="form-stack" data-api="/student/competitions/${event.id}/registrations" data-next="/student/registrations/{id}">
                    <p class="notice" data-message role="alert" hidden></p>
                    <c:if test="${event.entryMode eq 'TEAM'}"><label><span>队伍展示名（${event.minTeamSize}–${event.maxTeamSize} 人）</span><input name="teamDisplayName" maxlength="80" required></label></c:if>
                    <button class="button">创建报名草稿</button>
                </form>
            </section>
        </c:if>
    </aside>
</div>
<%@ include file="shared/footer.jspf" %>
