<%@ include file="shared/header.jspf" %><%-- 按业务对象和操作筛选后台审计记录。 --%>
<div class="title-block"><h1>操作审计</h1><p class="subhead">按业务对象查询关键操作记录。</p></div>
<section class="card">
<form class="actions" method="get">
    <label><span>对象类型</span><select name="targetType">
        <option value="">全部对象</option>
        <option value="COMPETITION" ${param.targetType eq 'COMPETITION' ? 'selected' : ''}>赛事</option>
        <option value="ACCOUNT" ${param.targetType eq 'ACCOUNT' ? 'selected' : ''}>账号</option>
    </select></label>
    <label><span>操作类型</span><select name="action">
        <option value="">全部操作</option>
        <c:forEach var="item" items="${auditActions}">
            <option value="<c:out value='${item.key}'/>" ${param.action eq item.key ? 'selected' : ''}><c:out value="${item.value}"/></option>
        </c:forEach>
    </select></label>
    <label><span>对象编号</span><input name="targetKey" value="<c:out value='${param.targetKey}'/>"></label>
    <button class="button secondary">查询</button>
</form>
<c:choose>
    <c:when test="${empty records.items}"><p class="empty">没有符合条件的操作记录。</p></c:when>
    <c:otherwise>
        <p class="muted">时间按中国标准时间（UTC+08:00）显示。</p>
        <div class="table-wrap"><table class="audit-table">
            <thead><tr><th>时间</th><th>操作</th><th>操作人</th><th>对象</th><th>摘要</th></tr></thead>
            <tbody>
                <c:forEach var="record" items="${records.items}"><tr>
                    <td class="audit-time"><c:out value="${record.displayTime}"/></td>
                    <td><c:out value="${record.displayAction}"/></td>
                    <td><c:out value="${record.displayActor}"/></td>
                    <td>
                        <span class="audit-object"><c:out value="${record.displayTarget}"/></span>
                        <span class="audit-meta muted"><c:out value="${record.displayTargetMeta}"/></span>
                    </td>
                    <td><c:out value="${record.displaySummary}"/></td>
                </tr></c:forEach>
            </tbody>
        </table></div>
    </c:otherwise>
</c:choose>
</section>
<%@ include file="shared/footer.jspf" %>
