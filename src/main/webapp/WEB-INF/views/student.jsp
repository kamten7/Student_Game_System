<%@ include file="shared/header.jspf" %>
<div class="title-block between">
    <div><h1>学生工作台</h1><p class="subhead">查看报名状态、组队邀请及下一步任务。</p></div>
    <a class="button" href="<c:url value='/'/>">发现赛事</a>
</div>
<c:if test="${profile.verificationStatus ne 'VERIFIED'}">
    <p class="notice page-pad">当前身份状态为 ${labels[profile.verificationStatus]}。<a href="<c:url value='/student/profile'/>">查看核验进度</a></p>
</c:if>
<section class="card">
    <h2>我的报名</h2>
    <c:choose>
        <c:when test="${empty registrations.items}"><p class="empty">还没有报名记录。</p></c:when>
        <c:otherwise><div class="table-wrap"><table><thead><tr><th>赛事</th><th>报名编号</th><th>状态</th><th>操作</th></tr></thead><tbody>
            <c:forEach var="row" items="${registrations.items}"><tr>
                <td><c:out value="${row.competitionName}"/></td><td class="mono"><c:out value="${row.registrationNo}"/></td>
                <td><span class="tag">${labels[row.status]}</span></td><td><a href="<c:url value='/student/registrations/${row.id}'/>">打开报名 ↗</a></td>
            </tr></c:forEach>
        </tbody></table></div></c:otherwise>
    </c:choose>
</section>
<section class="card page-pad" id="invitations">
    <div class="between">
        <h2>组队邀请</h2>
        <a href="<c:url value='/student/profile#invite-code'/>">我的邀请码</a>
    </div>
    <c:if test="${empty invitations}"><p class="empty">当前没有组队邀请。</p></c:if>
    <ul class="line-list">
        <c:forEach var="invite" items="${invitations}"><li class="between">
            <div><strong><c:out value="${invite.teamDisplayName}"/></strong><p class="muted"><c:out value="${invite.competitionName}"/></p></div>
            <c:choose>
                <c:when test="${invite.invitationStatus eq 'INVITED'}">
                    <form class="actions" data-api="/student/invitations/${invite.registrationId}/decision">
                        <button class="button secondary" name="decision" value="DECLINE">拒绝</button>
                        <button class="button" name="decision" value="ACCEPT">接受</button>
                    </form>
                </c:when>
                <c:otherwise><span class="tag">${labels[invite.invitationStatus]}</span></c:otherwise>
            </c:choose>
        </li></c:forEach>
    </ul>
</section>
<%@ include file="shared/footer.jspf" %>
