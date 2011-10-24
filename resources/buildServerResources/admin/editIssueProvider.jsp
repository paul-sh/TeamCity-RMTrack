<%@ include file="/include.jsp"%>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>

<%@ include file="/include.jsp"%>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>

<div>
  <table class="editProviderTable">
    <c:if test="${showType}">
      <tr>
        <th><label class="shortLabel">Connection Type:</label></th>
        <td>RMTrack</td>
      </tr>
    </c:if>
    <tr>
      <th><label for="name" class="shortLabel">Connection Name: <l:star/></label></th>
      <td>
        <props:textProperty name="name" maxlength="100" style="width: 16em;"/>
        <span id="error_name" class="error"></span>
      </td>
    </tr>
    <tr>
      <th><label for="repository" class="shortLabel">Repository: <l:star/></label></th>
      <td>
        <props:textProperty name="repository" maxlength="100" style="width: 16em;"/>
        <span id="error_repository" class="error"></span>
         <span class="fieldExplanation">Your customer ID. Like in rmtrack.com/YOURREPOSITORY</span>
      </td>
    </tr>
    <tr>
      <th><label for="secure:accesskey" class="shortLabel">Access Key: <l:star/></label></th>
      <td>
        <props:textProperty name="secure:accesskey" maxlength="100" style="width: 16em;"/>
        <span id="error_secure:accesskey" class="error"></span>
      </td>
    </tr>
    <tr>
      <th><label for="secure:secretkey" class="shortLabel">Secret Key: <l:star/></label></th>
      <td>
        <props:textProperty name="secure:secretkey" maxlength="100" style="width: 16em;"/>
        <span id="error_secure:secretkey" class="error"></span>
      </td>
    </tr>
    <tr>
       <th><label for="pattern" class="shortLabel">Ticket Pattern: <l:star/></label></th>
       <td>
         <props:textProperty name="pattern" maxlength="100" style="width: 16em;"/>
         <span id="error_idPrefix" class="error"></span>
         <span class="fieldExplanation">Use regexp, e.g. RMT(\d+)<bs:help file="Issue+Tracker+Tab"/></span>
        </td>
    </tr>
  </table>
</div>
