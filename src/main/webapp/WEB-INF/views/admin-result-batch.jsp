<%@ include file="shared/header.jspf" %>
<div class="title-block"><p class="muted"><a href="<c:url value='/admin/competitions/${batch.competitionId}/results'/>">返回结果管理</a></p>
    <h1>第 ${batch.versionNo} 版结果快照</h1><p class="subhead"><time datetime="${batch.publishedAt}">${batch.publishedAt}</time> · ${batch.publicationType eq 'FIRST' ? '首次发布' : '发布更正'}</p></div>
<div class="stack">
    <c:if test="${not empty batch.correctionReason}"><p class="notice"><c:out value="${batch.correctionReason}"/></p></c:if>
    <section class="card"><h2>批次明细</h2>
        <div class="table-wrap"><table><thead><tr><th>名次</th><th>作品</th><th>参赛展示名</th><th>公开获奖人员</th><th>状态</th><th>得分</th><th>奖项</th></tr></thead><tbody>
            <c:forEach var="item" items="${batch.items}"><tr>
                <td>${item.rankNo}</td><td><c:out value="${item.workTitle}"/></td><td><c:out value="${item.entryDisplay}"/></td>
                <td><c:forEach var="name" items="${item.recipientNames}" varStatus="position"><c:if test="${not position.first}">、</c:if><c:out value="${name}"/></c:forEach></td>
                <td>${labels[item.outcomeStatus]}</td><td>${item.totalScore}</td><td><c:out value="${item.awardName}"/></td>
            </tr></c:forEach>
        </tbody></table></div>
    </section>
    <details class="card"><summary>查看评分依据快照</summary><p class="mono rule-text"><c:out value="${batch.scoringBasis}"/></p></details>
</div>
<%@ include file="shared/footer.jspf" %>
