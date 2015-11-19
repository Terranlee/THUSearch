<%@include file="/include/handler.jsp"%>
<%@ page import="org.archive.io.SinkHandlerLogRecord" %>
<%@ page import="java.util.logging.Level" %>
<%
    SinkHandlerLogRecord alert =
        heritrix.getAlert(request.getParameter("alert"));
    String title = "Read alert";
    int tab = 0;
%>

<%@include file="/include/head.jsp"%>
<p>
<% if(alert == null) { %>
    <b> No matching alert found </b>
<% } else { 
    alert.setRead();
%>
    <table>
        <tr>
            <td>
                <b>Time:</b>&nbsp;
            </td>
            <td>
                <%=sdf.format(alert.getCreationTime())%> GMT
            </td>
        </tr>
        <tr>
            <td>
                <b>Level:</b>&nbsp;
            </td>
            <td>
                <%=alert.getLevel().getName()%>
            </td>
        </tr>
        <tr>
            <td valign="top">
                <b>Message:</b>&nbsp;
            </td>
            <td>
                <pre><%=alert.getMessage()%></pre>
            </td>
        </tr>
        <tr>
            <td valign="top">
                <b>Exception:</b>&nbsp;
            </td>
            <td>
                <pre><%=alert.getThrownToString()%></pre>
            </td>
        </tr>
    </table>
<% } %>
    <p>
        <a href="<%=request.getContextPath()%>/console/alerts.jsp">Back to alerts</a>
        &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
        <% if(alert != null) { %>
             <a href="<%=request.getContextPath()%>/console/alerts.jsp?alerts=<%=alert.getSequenceNumber()%>&action=delete">Delete this alert</a>
        <% } %>
<%@include file="/include/foot.jsp"%>
