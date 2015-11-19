<%@ page import="org.archive.crawler.Heritrix" %>
<%@include file="/include/handler.jsp"%>

<%
    String title = "Web UI Prefs";
    int tab = 5;
    String setFavicon = request.getParameter("favicon");
    if(setFavicon!=null) {
        System.getProperties().setProperty("heritrix.favicon",setFavicon);
    }
%>

<%@include file="/include/head.jsp"%>

<div class="margined">

    <h1>Web UI Preferences</h1>

<fieldset>
    <legend>Reset Favicon</legend>

    To help distinguish multiple crawler web interfaces, you may choose this 
    web interface's 'favicon' by clicking any of the following: 

<p style="text-align:center">
    <a href="<%=request.getContextPath()%>?favicon=h.ico"><img src="<%=request.getContextPath()%>/images/h.ico"/></a>
    <a href="<%=request.getContextPath()%>?favicon=h-blue.ico"><img src="<%=request.getContextPath()%>/images/h-blue.ico"/></a>
    <a href="<%=request.getContextPath()%>?favicon=h-purple.ico"><img src="<%=request.getContextPath()%>/images/h-purple.ico"/></a>
    <a href="<%=request.getContextPath()%>?favicon=h-red.ico"><img src="<%=request.getContextPath()%>/images/h-red.ico"/></a>
    <a href="<%=request.getContextPath()%>?favicon=h-orange.ico"><img src="<%=request.getContextPath()%>/images/h-orange.ico"/></a>
    <a href="<%=request.getContextPath()%>?favicon=h-yellow.ico"><img src="<%=request.getContextPath()%>/images/h-yellow.ico"/></a>
    <a href="<%=request.getContextPath()%>?favicon=h-green.ico"><img src="<%=request.getContextPath()%>/images/h-green.ico"/></a>
    <a href="<%=request.getContextPath()%>?favicon=h-teal.ico"><img src="<%=request.getContextPath()%>/images/h-teal.ico"/></a>
</p>
</fieldset>
<br/>
<fieldset>
    <legend>Change Operator Login</legend>
    
<%
	String newUsername = request.getParameter("newUsername");
	String newPassword = request.getParameter("newPassword");
	if(newUsername!=null && newPassword != null && newUsername.length()>0 
	        && newPassword.length()>0) {
	    Heritrix.resetAuthentication(newUsername,newPassword); 
	    %>
	    <span class="flashMessage">administrative login changed to 
	    username '<%=newUsername%>' password '<%=newPassword %>' </span>
	    <%
	}
%>
    <form method="POST">
    <table border="0" cellspacing="0" cellpadding="0">
    <tr>
        <td align="right">
            new username&nbsp;
        </td>
        <td>
            <input size="15" name="newUsername" value="">
        </td>
    </tr>
    <tr>
        <td align="right">
            new password&nbsp;
        </td>
        <td>
            <input size="15" name="newPassword" value="">
        </td>
    </tr>
    <tr><td colspan="2"><input type="submit" value="change login info"></td></tr>
    </table>
    </form>
</fieldset>
</div>
<%@include file="/include/foot.jsp"%>
