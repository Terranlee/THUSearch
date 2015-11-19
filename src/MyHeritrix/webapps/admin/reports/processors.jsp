<%@include file="/include/handler.jsp"%>

<%
    String title = "Processors report";
    int tab = 4;
%>

<%@include file="/include/head.jsp"%>

    <pre><%=handler.getCurrentJob() != null?
    handler.getCurrentJob().getProcessorsReport().replaceAll(" ","&nbsp;"):
    ""%></pre>

<%@include file="/include/foot.jsp"%>
