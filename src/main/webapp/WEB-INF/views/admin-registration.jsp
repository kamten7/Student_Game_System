<%@ include file="shared/header.jspf" %>
<div class="title-block between">
    <div><p class="muted"><a href="<c:url value='/admin/competitions/${event.id}'/>"><c:out value="${event.name}"/></a> / 资格审核</p>
    <h1>审核参赛报名</h1><p class="subhead mono"><c:out value="${registration.registrationNo}"/></p></div>
    <span class="tag">${labels[registration.status]}</span>
</div>
<div class="columns">
    <section class="card"><h2>参赛名单</h2>
        <ul class="line-list"><c:forEach var="member" items="${registration.members}"><li class="between">
            <span><c:out value="${member.fullName}"/> · ${member.isCaptain ? '队长' : '成员'}</span><span class="tag">${labels[member.invitationStatus]}</span>
        </li></c:forEach></ul>
        <h2>正式提交及审核记录</h2>
        <ul class="line-list"><c:forEach var="item" items="${registration.history}"><li>
            <strong>${labels[item.toStatus]}</strong> · <time datetime="${item.createdAt}">${item.createdAt}</time>
            <p><c:out value="${item.reason}"/></p>
            <c:if test="${not empty item.rosterSnapshot}"><details><summary>正式名单快照</summary><ul><c:forEach var="record" items="${item.rosterSnapshot}"><li><c:out value="${record.fullName}"/></li></c:forEach></ul></details></c:if>
        </li></c:forEach></ul>
    </section>
    <section class="card"><h2>审核决定</h2>
        <c:choose><c:when test="${registration.status eq 'PENDING' and event.status eq 'PUBLISHED'}">
            <form class="form-stack" data-api="/admin/registrations/${registration.id}/decision">
                <p class="notice" data-message role="alert" hidden></p>
                <input type="hidden" name="rowVersion" value="${registration.rowVersion}">
                <label><span>审核决定</span><select name="decision"><option value="APPROVE">通过</option><option value="RETURN">退回补正</option><option value="REJECT">拒绝</option></select></label>
                <label><span>处理说明</span><textarea name="reason" maxlength="400" placeholder="退回或拒绝时须填写理由"></textarea></label>
                <button class="button">确认审核</button>
            </form>
        </c:when><c:otherwise><p class="muted">当前没有可执行的审核决定。</p></c:otherwise></c:choose>
    </section>
</div>
<%@ include file="shared/footer.jspf" %>
