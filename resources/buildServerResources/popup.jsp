<%@ page import="com.paulsh.rmtrack.RMTrackIssueFetcher" %>
<%@ include file="/include.jsp" %>
<jsp:useBean id="issue" scope="request" type="jetbrains.buildServer.issueTracker.IssueEx"/>
<c:set var="issueData" value="${issue.issueDataOrNull}"/>
<c:set var="fields" value="${issueData.allFields}"/>
<c:set var="assignedToField"><%=RMTrackIssueFetcher.ASSIGNED_TO_FIELD%></c:set>
<c:set var="customerField"><%=RMTrackIssueFetcher.CUSTOMER_FIELD%></c:set>
<bs:issueDetailsPopup issue="${issue}"
                       popupClass="rmtrack"
                       priorityClass="${fn:toLowerCase(issue.priority)}">
  <jsp:attribute name="otherFields">
    <c:set var="assignedTo" value="${fields[assignedToField]}"/>
    <c:set var="customer" value="${fields[customerField]}"/>
    <c:if test="${not empty assignedTo}">
      <td title="Assigned To">${assignedTo}</td>
    </c:if>
    <c:if test="${not empty customer}">
      <td title="Customer">${customer}</td>
    </c:if>
  </jsp:attribute>
</bs:issueDetailsPopup>
