<%@ include file="shared/header.jspf" %><%-- 展示分派的匿名作品和评分项，提供提交评分及回避入口。 --%>
<div class="title-block between">
    <div><p class="muted"><a href="<c:url value='/judge'/>">评审任务</a> / <c:out value="${assignment.competitionName}"/></p>
    <h1>匿名评审工作台</h1><p class="subhead"><span class="mono"><c:out value="${assignment.anonymousCode}"/></span> · 锁定版本 V${assignment.versionNo} · <time datetime="${assignment.judgingClosesAt}">${assignment.judgingClosesAt}</time> 截止</p></div>
    <span class="tag">${labels[assignment.status]}</span>
</div>
<div class="columns score-layout">
    <section class="card">
        <div class="between"><h2>匿名作品</h2>
            <a class="button secondary" href="<c:url value='/api/v1/judge/assignments/${assignment.id}/file'/>">下载 PDF</a></div>
        <p data-pdf-message class="muted" hidden>此浏览器暂时无法预览，请下载 PDF。</p>
        <iframe data-pdf="/judge/assignments/${assignment.id}/file" title="匿名作品 PDF 预览"></iframe>
    </section>
    <aside class="stack">
        <section class="card"><div class="between"><h2>评分表</h2><span class="muted">每项 0–100 分</span></div>
            <form class="form-stack" data-api="/judge/assignments/${assignment.id}/score" data-method="PUT">
                <p class="notice" data-message role="alert" hidden></p>
                <input type="hidden" name="rowVersion" value="${empty assignment.score ? 0 : assignment.score.rowVersion}">
                <div class="form-grid">
                    <c:forEach var="criterion" items="${assignment.scoringRubric}">
                        <label><span><c:out value="${criterion.label}"/> · ${criterion.weight}%</span>
                            <input type="number" min="0" max="100" step="0.01" data-score="${criterion.id}"
                                value="<c:out value='${assignment.score.criterionScores[criterion.id]}'/>" ${assignment.canScore ? '' : 'readonly'}>
                        </label>
                    </c:forEach>
                </div>
                <label><span>评语</span><textarea name="feedback" maxlength="1200" ${assignment.canScore ? '' : 'readonly'}><c:out value="${assignment.score.feedback}"/></textarea></label>
                <c:if test="${assignment.canScore}"><div><button class="button secondary">保存评分草稿</button></div></c:if>
            </form>
            <c:if test="${assignment.canScore and not empty assignment.score}">
                <form data-api="/judge/assignments/${assignment.id}/submit">
                    <input type="hidden" name="rowVersion" value="${assignment.score.rowVersion}">
                    <button class="button" data-confirm="提交当前已保存的评分？">提交已保存评分</button>
                </form>
            </c:if>
            <c:if test="${assignment.status eq 'SUBMITTED'}"><p class="notice ok">评分已正式提交。</p></c:if>
        </section>
        <c:if test="${assignment.status eq 'ASSIGNED'}">
            <details class="card"><summary>申请回避</summary>
                <form class="form-stack" data-api="/judge/assignments/${assignment.id}/recuse" data-next="/judge">
                    <label><span>回避原因</span><textarea name="reason" maxlength="400" required></textarea></label>
                    <div><button class="button danger" data-confirm="回避后将无法查看作品或提交评分。">确认回避</button></div>
                </form>
            </details>
        </c:if>
    </aside>
</div>
<%@ include file="shared/footer.jspf" %>
