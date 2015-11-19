<%@include file="/include/handler.jsp"%>

<%
    String title = "Setup";
    int tab = 5;
%>

<%@include file="/include/head.jsp"%>

<div class="margined">
    <h1>Heritrix Setup</h1>
<p>
    <b><a href="<%=request.getContextPath()%>/local-instances.jsp">Local Instances</a></b></br>
    Choose an instance of Heritrix to manage, or create new instances.
</p>
<p>
    <b><a href="<%=request.getContextPath()%>/help/webui-prefs.jsp">Web UI Preferences</a></b></br>
    Change admin password, or change the site's icon.
</p>
</div>
<%@include file="/include/foot.jsp"%>
