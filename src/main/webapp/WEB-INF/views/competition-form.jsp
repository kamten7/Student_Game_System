<%@ include file="shared/header.jspf" %><%-- 创建或编辑赛事草稿，填写赛程、组队人数及评分规则。 --%>
<div class="title-block"><h1>${empty event ? '创建赛事' : '编辑赛事草稿'}</h1><p class="subhead">赛程按中国标准时间（UTC+08:00）填写，发布后冻结赛制和评分规则。</p></div>
<form class="card form-stack" data-api="/admin/competitions${empty event ? '' : '/'}${event.id}" data-method="${empty event ? 'POST' : 'PATCH'}" data-next="/admin/competitions/{id}">
    <p class="notice" data-message role="alert" hidden></p>
    <c:if test="${not empty event}"><input type="hidden" name="rowVersion" value="${event.rowVersion}"></c:if>
    <div class="form-grid">
        <label><span>赛事名称</span><input name="name" value="<c:out value='${event.name}'/>" maxlength="120" required></label>
        <label><span>届次</span><input name="edition" value="<c:out value='${event.edition}'/>" maxlength="40" required placeholder="例如 2026 年度"></label>
        <label class="wide"><span>公开规则</span><textarea name="rulesText" maxlength="8000" required><c:out value="${event.rulesText}"/></textarea></label>
        <label><span>参赛方式</span><select name="entryMode"><option value="INDIVIDUAL">个人赛</option><option value="TEAM" ${event.entryMode eq 'TEAM' ? 'selected' : ''}>团队赛</option></select></label>
        <label><span>每件所需评委</span><input name="requiredJudges" type="number" min="1" max="20" value="${empty event ? 3 : event.requiredJudges}" required></label>
        <label><span>团队最少人数</span><input name="minTeamSize" type="number" min="1" max="20" value="${event.entryMode eq 'TEAM' ? event.minTeamSize : 2}" required></label>
        <label><span>团队最多人数</span><input name="maxTeamSize" type="number" min="1" max="20" value="${event.entryMode eq 'TEAM' ? event.maxTeamSize : 4}" required></label>
        <c:forEach var="key" items="${dateKeys}" varStatus="step">
            <label><span>${dateFields[step.index]}</span><input type="datetime-local" name="${key}" data-utc="${event[key]}" required></label>
        </c:forEach>
    </div>
    <fieldset class="rubric" data-rubric>
        <legend>评分维度</legend>
        <div class="form-stack" data-rubric-list>
            <c:forEach var="criterion" items="${criteria}">
                <div class="rubric-row" data-rubric-id="<c:out value='${criterion.id}'/>">
                    <label><span>维度名称</span><input data-rubric-label maxlength="40" value="<c:out value='${criterion.label}'/>" required></label>
                    <label><span>权重 %</span><input type="number" min="0.01" max="100" step="0.01" data-rubric-weight value="${criterion.weight}" required></label>
                    <button type="button" class="button secondary" data-rubric-remove>移除</button>
                </div>
            </c:forEach>
        </div>
        <template data-rubric-template>
            <div class="rubric-row" data-rubric-id>
                <label><span>维度名称</span><input data-rubric-label maxlength="40" required></label>
                <label><span>权重 %</span><input type="number" min="0.01" max="100" step="0.01" data-rubric-weight required></label>
                <button type="button" class="button secondary" data-rubric-remove>移除</button>
            </div>
        </template>
        <div class="between">
            <button type="button" class="button secondary" data-rubric-add>添加维度</button>
            <span class="muted" role="status" data-rubric-total></span>
        </div>
    </fieldset>
    <div class="actions"><button class="button">保存草稿</button><a href="<c:url value='/admin'/>">返回赛事列表</a></div>
</form>
<%@ include file="shared/footer.jspf" %>
