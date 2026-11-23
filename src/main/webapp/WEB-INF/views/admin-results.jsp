<%@ include file="shared/header.jspf" %><%-- 核对评分覆盖与获奖结果，确认首次发布或更正。 --%>
<div class="title-block between">
    <div><p class="muted"><a href="<c:url value='/admin/competitions/${event.id}'/>"><c:out value="${event.name}"/></a> / 结果</p>
        <h1>${empty event.currentPublicationId ? '核对并发布结果' : '核对并发布更正'}</h1>
        <p class="subhead">检查评审覆盖、名次、奖项和公开展示字段。</p></div>
    <span class="tag">${labels[event.phase]}</span>
</div>
<form class="stack" data-results="${event.id}">
    <input type="hidden" name="requestId" value="${requestId}">
    <input type="hidden" name="currentPublicationId" value="${event.currentPublicationId}">
    <p class="notice" data-result-message role="status" hidden></p>
    <section class="card"><h2>结果与奖项</h2>
        <c:if test="${empty preview.items}"><p class="empty">本届赛事没有可生成成绩的报名记录。</p></c:if>
        <div class="table-wrap">
            <table class="result-editor"><thead><tr><th>名次及得分</th><th>公开作品名</th><th>公开参赛名</th><th>公开获奖人员</th><th>拟奖项</th></tr></thead><tbody>
            <c:forEach var="item" items="${preview.items}"><tr data-result-row="${item.registrationId}">
                <td><c:choose><c:when test="${not empty item.rankNo}"><strong>第 ${item.rankNo} 名</strong><br>${item.totalScoreText} 分</c:when>
                    <c:otherwise><span class="muted">${labels[item.outcomeStatus]}</span></c:otherwise></c:choose></td>
                <td><input aria-label="作品名" data-edit="workTitle" maxlength="120" value="<c:out value='${item.workTitle}'/>" ${item.outcomeStatus eq 'NO_WORK' ? 'disabled' : ''}></td>
                <td><input aria-label="参赛展示名" data-edit="entryDisplay" maxlength="80" value="<c:out value='${item.entryDisplay}'/>"></td>
                <td>
                    <c:set var="namesText" value=""/>
                    <c:forEach var="name" items="${item.recipientNames}" varStatus="position">
                        <c:set var="namesText" value="${namesText}${position.first ? '' : '、'}${name}"/>
                    </c:forEach>
                    <input aria-label="公开获奖人员" data-edit="recipientNames" maxlength="1220"
                           placeholder="选填，用顿号分隔" value="<c:out value='${namesText}'/>" ${item.outcomeStatus eq 'RANKED' ? '' : 'disabled'}>
                </td>
                <td><input aria-label="奖项" data-edit="awardName" maxlength="60" value="<c:out value='${item.awardName}'/>" ${item.outcomeStatus eq 'RANKED' ? '' : 'disabled'}>
                    <c:if test="${not empty event.currentPublicationId and item.outcomeStatus ne 'NO_WORK'}">
                        <details><summary>按规则调整参评范围</summary>
                            <label><span>取消参评依据</span><input data-edit="excludeReason" maxlength="400"></label>
                            <c:if test="${item.outcomeStatus eq 'EXCLUDED'}"><label><input type="checkbox" data-edit="restore"> 恢复参评</label></c:if>
                        </details>
                    </c:if></td>
            </tr></c:forEach>
            </tbody></table>
        </div>
        <p class="muted">仅填写已确认可公开的展示姓名。获奖名单会和本次结果一并发布。</p>
        <label><input type="checkbox" name="noAwards"> 确认本批次无获奖条目</label>
        <c:if test="${not empty event.currentPublicationId}"><label><span>更正理由</span><textarea name="reason" maxlength="400" required></textarea></label></c:if>
        <div class="actions">
            <button type="submit" class="button secondary">生成发布预览</button>
            <button type="button" class="button" data-result-confirm disabled>确认发布</button>
        </div>
    </section>
    <section class="card" data-result-preview hidden><h2>当前预览</h2><ul class="line-list" data-result-entries></ul></section>
</form>
<section class="card page-pad"><h2>历史批次</h2>
    <c:if test="${empty batches}"><p class="muted">还没有已发布批次。</p></c:if>
    <ul class="line-list"><c:forEach var="batch" items="${batches}"><li class="between">
        <span>第 ${batch.versionNo} 版 · <time datetime="${batch.publishedAt}">${batch.publishedAt}</time>
            <c:if test="${batch.currentBatch}"><span class="tag">当前批次</span></c:if></span>
        <a href="<c:url value='/admin/competitions/${event.id}/results/${batch.id}'/>">查看快照 ↗</a>
    </li></c:forEach></ul>
</section>
<%@ include file="shared/footer.jspf" %>
