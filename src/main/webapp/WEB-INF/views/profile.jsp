<%@ include file="shared/header.jspf" %>
<div class="title-block"><h1>个人资料</h1><p class="subhead">管理报名所用的资料和组队邀请码。</p></div>
<div class="columns">
    <section class="card"><h2>基本资料</h2>
        <form class="form-stack" data-api="/me/profile" data-method="PATCH">
            <p class="notice" data-message role="alert" hidden></p>
            <label><span>姓名</span><input name="fullName" value="<c:out value='${profile.fullName}'/>" maxlength="60" required ${profile.verificationStatus eq 'VERIFIED' ? 'readonly' : ''}></label>
            <label><span>院系</span><input name="department" value="<c:out value='${profile.department}'/>" maxlength="80"></label>
            <label><span>专业</span><input name="major" value="<c:out value='${profile.major}'/>" maxlength="80"></label>
            <div><button class="button">保存资料</button></div>
        </form>
    </section>
    <aside class="stack">
        <section class="card"><h2>校内身份</h2><span class="tag">${labels[profile.verificationStatus]}</span>
            <div>申报学号 <c:out value="${profile.claimedStudentNo}"/></div>
            <c:if test="${profile.verificationStatus eq 'VERIFIED'}">
                <div>正式学号 <c:out value="${profile.studentNo}"/></div>
            </c:if>
        </section>
        <section class="card form-stack" id="invite-code">
            <h2>组队邀请码</h2>
            <c:choose>
                <c:when test="${profile.verificationStatus eq 'VERIFIED'}">
                    <c:if test="${not empty profile.inviteCode}">
                        <p class="muted">把你的邀请码发给队长。对方发送后，到「组队邀请」接受。</p>
                        <label><span>我的邀请码</span>
                            <input class="mono" id="my-invite-code" value="<c:out value='${profile.inviteCode}'/>" readonly>
                        </label>
                    </c:if>
                    <div class="actions">
                        <c:if test="${not empty profile.inviteCode}">
                            <button type="button" class="button" data-copy-invite="my-invite-code">复制邀请码</button>
                        </c:if>
                        <form data-api="/me/invite-code/rotate">
                            <button class="button secondary" data-confirm="${empty profile.inviteCode ? '' : '更新后旧邀请码将失效。确定更新？'}">${empty profile.inviteCode ? '生成邀请码' : '更新邀请码'}</button>
                        </form>
                    </div>
                    <p class="muted" data-copy-invite-status role="status" hidden></p>
                    <a href="<c:url value='/student#invitations'/>">查看组队邀请</a>
                </c:when>
                <c:otherwise><p class="muted">完成校内身份核验后，即可使用组队邀请码。</p></c:otherwise>
            </c:choose>
        </section>
    </aside>
</div>
<%@ include file="shared/footer.jspf" %>
