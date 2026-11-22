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
    <aside class="card"><h2>校内身份</h2><span class="tag">${labels[profile.verificationStatus]}</span>
        <div>申报学号 <c:out value="${profile.claimedStudentNo}"/></div>
        <c:if test="${profile.verificationStatus eq 'VERIFIED'}">
            <div>正式学号 <c:out value="${profile.studentNo}"/></div>
            <p class="notice ok mono"><c:out value="${profile.inviteCode}"/></p>
            <p class="muted">将这个邀请码分享给要组队的学生。</p>
            <form data-api="/me/invite-code/rotate"><button class="button secondary">更新邀请码</button></form>
        </c:if>
    </aside>
</div>
<%@ include file="shared/footer.jspf" %>
