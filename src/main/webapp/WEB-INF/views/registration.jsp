<%@ include file="shared/header.jspf" %><%-- 展示报名名单及审核记录，提供组队、提交和交件入口。 --%>
<div class="title-block between">
    <div><p class="muted"><a href="<c:url value='/student'/>">我的报名</a> / <c:out value="${registration.registrationNo}"/></p>
    <h1><c:out value="${event.name}"/></h1><p class="subhead"><c:out value="${registration.teamDisplayName}"/> · ${labels[event.entryMode]}</p></div>
    <span class="tag">${labels[registration.status]}</span>
</div>
<c:set var="editable" value="${captain and event.status eq 'PUBLISHED' and ((registration.status eq 'DRAFT' and event.phase eq 'REGISTRATION') or registration.status eq 'CHANGES_REQUESTED')}"/>
<c:if test="${registration.status eq 'CHANGES_REQUESTED'}"><p class="notice title-block">审核退回。补正截止 <time datetime="${event.correctionClosesAt}">${event.correctionClosesAt}</time>。</p></c:if>
<div class="columns">
    <section class="card">
        <h2>参赛名单</h2>
        <ul class="line-list">
            <c:forEach var="member" items="${registration.members}"><li class="between">
                <span><c:out value="${member.fullName}"/> <small class="muted">${member.isCaptain ? '队长' : '成员'}</small></span>
                <div class="actions"><span class="tag">${labels[member.invitationStatus]}</span>
                <c:if test="${editable and not member.isCaptain}"><form data-api="/student/registrations/${registration.id}/members/${member.studentId}" data-method="DELETE"><button class="button secondary">移除</button></form></c:if>
                </div>
            </li></c:forEach>
        </ul>
        <c:if test="${editable and event.entryMode eq 'TEAM'}">
            <form class="form-stack" data-api="/student/registrations/${registration.id}/invitations">
                <p class="notice" data-message role="alert" hidden></p>
                <label><span>队友的邀请码</span><input name="inviteCode" maxlength="36" required autocomplete="off"
                    autocapitalize="off" spellcheck="false" aria-describedby="invite-help" placeholder="粘贴队友发给你的邀请码"></label>
                <p class="muted" id="invite-help">请队友用自己的学生账号，在「个人资料 → 组队邀请码」复制并发给你。发送后，队友需要在「组队邀请」接受。</p>
                <div><button class="button secondary">发送邀请</button></div>
            </form>
            <details><summary>修改队伍展示名</summary><form class="form-stack" data-api="/student/registrations/${registration.id}" data-method="PATCH">
                <input type="hidden" name="rowVersion" value="${registration.rowVersion}">
                <label><span>队名</span><input name="teamDisplayName" value="<c:out value='${registration.teamDisplayName}'/>" maxlength="80" required></label>
                <div><button class="button secondary">保存队名</button></div>
            </form></details>
        </c:if>
        <c:if test="${editable}">
            <div class="actions">
                <form data-api="/student/registrations/${registration.id}/${registration.status eq 'DRAFT' ? 'submit' : 'resubmit'}">
                    <input type="hidden" name="rowVersion" value="${registration.rowVersion}">
                    <button class="button">${registration.status eq 'DRAFT' ? '正式提交报名' : '重新提交报名'}</button>
                </form>
                <c:if test="${registration.status eq 'DRAFT'}">
                    <form data-api="/student/registrations/${registration.id}" data-method="DELETE" data-next="/student">
                        <button class="button danger" data-confirm="删除这条报名草稿？">删除草稿</button>
                    </form>
                </c:if>
            </div>
        </c:if>
        <c:if test="${captain and event.phase eq 'REGISTRATION' and (registration.status eq 'PENDING' or registration.status eq 'CHANGES_REQUESTED')}">
            <form data-api="/student/registrations/${registration.id}/withdraw">
                <input type="hidden" name="rowVersion" value="${registration.rowVersion}">
                <button class="button danger" data-confirm="撤回后将释放本报名的资格占用。">撤回报名</button>
            </form>
        </c:if>
    </section>
    <aside class="card">
        <h2>报名记录</h2>
        <c:if test="${empty registration.history}"><p class="muted">正式提交后显示审核进度。</p></c:if>
        <ul class="line-list"><c:forEach var="item" items="${registration.history}"><li>
            <strong>${labels[item.toStatus]}</strong> · <time datetime="${item.createdAt}">${item.createdAt}</time>
            <p><c:out value="${item.reason}"/></p>
        </li></c:forEach></ul>
    </aside>
</div>
<c:if test="${registration.status eq 'APPROVED'}">
    <div class="columns page-pad">
        <section class="card"><h2>作品与版本</h2>
            <c:if test="${captain and event.phase eq 'SUBMISSION'}">
                <p class="notice ok">作品截止 <time datetime="${event.submissionClosesAt}">${event.submissionClosesAt}</time>。评审采用截止时最后一个有效版本。</p>
                <form class="form-stack" enctype="multipart/form-data" data-api="/student/registrations/${registration.id}/submission/versions">
                    <p class="notice" data-message role="alert" hidden></p>
                    <input type="hidden" name="requestId" value="${requestId}">
                    <label><span>作品名称</span><input name="title" maxlength="120" value="<c:out value='${submission.versions[0].title}'/>" required></label>
                    <label><span>选择匿名 PDF（最大 20 MiB）</span><input type="file" name="file" accept="application/pdf,.pdf" required></label>
                    <div><button class="button">提交作品</button></div>
                </form>
            </c:if>
            <c:if test="${empty submission.versions}"><p class="muted">尚未接收有效作品。</p></c:if>
            <ul class="line-list">
                <c:forEach var="file" items="${submission.versions}" varStatus="step"><li class="between">
                    <div><strong><c:out value="${file.title}"/></strong><p class="mono muted"><c:out value="${file.receipt}"/> · <time datetime="${file.submittedAt}">${file.submittedAt}</time></p></div>
                    <a href="<c:url value='/api/v1/student/registrations/${registration.id}/submission/versions/${file.versionNo}/file'/>">${step.first ? '下载有效版本' : '下载旧版本'}</a>
                </li></c:forEach>
            </ul>
        </section>
        <aside class="card"><h2>我的结果</h2>
            <c:choose><c:when test="${empty result}"><p class="muted">公布后可在这里查看本人报名的结果。</p></c:when><c:otherwise>
                <span class="tag">${labels[result.outcomeStatus]}</span>
                <c:if test="${not empty result.rankNo}"><p>第 ${result.rankNo} 名 · ${result.totalScore} 分</p></c:if>
                <p><c:out value="${result.awardName}"/></p><p><c:out value="${result.outcomeReason}"/></p>
            </c:otherwise></c:choose>
        </aside>
    </div>
</c:if>
<%@ include file="shared/footer.jspf" %>
