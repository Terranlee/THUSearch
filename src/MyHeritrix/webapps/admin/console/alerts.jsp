<%@include file="/include/handler.jsp"%>
<%@ page import="java.util.Vector,java.util.HashMap" %>
<%@ page import="java.util.logging.Level"%>
<%@ page import="org.archive.io.SinkHandlerLogRecord"%>

<%
    HashMap levelColors = new HashMap(7);
    levelColors.put(Level.SEVERE,"#da9090");
    levelColors.put(Level.WARNING,"#daaaaa");
    levelColors.put(Level.INFO,"#dababa");
    levelColors.put(Level.CONFIG,"#dac0c0");
    levelColors.put(Level.FINE,"#dacaca");
    levelColors.put(Level.FINER,"#dad0d0");
    levelColors.put(Level.FINEST,"#dadada");

    String action = request.getParameter("action");
    if(action != null) {
        String alertIDs[] = request.getParameterValues("alerts");
        if (alertIDs != null) {    
            for(int i = 0; i < alertIDs.length; i++) {        
                if(action.equals("markasread")) {
                    heritrix.readAlert(alertIDs[i]);                
                } else if(action.equals("delete")) {
                    heritrix.removeAlert(alertIDs[i]);
                }
            }
        }
    }
    Vector alerts = heritrix.getAlerts();
    String title = "Alerts";
    int tab = 0;
%>

<%@include file="/include/head.jsp"%>

<script type="text/javascript">
    function doDeleteAll() {
        document.frmAlerts.action.value="delete";
        document.frmAlerts.submit();
    }
    
    function doMarkAllAsRead() {
        document.frmAlerts.action.value="markasread";
        document.frmAlerts.submit();
    }
    
    function setAll(val) {
        boxes = document.frmAlerts.elements;
        for( i = 0 ; i < boxes.length ; i++){
            boxes[i].checked = val;
        }
    }
</script>

<p>

<% if(alerts.size() == 0) { %>
    There are no alerts at this time.
<% } else { %>
    <form name="frmAlerts" method="post" action="alerts.jsp">
    <input type="hidden" name="action">
    <table cellspacing="1" cellpadding="0" border="0" >
        <THEAD>
        <tr height="18">
            <th bgcolor="#003399" style="color:#FFFFFF" colspan="2">
                &nbsp;Time of alert&nbsp;
            </th>
            <th bgcolor="#003399" style="color:#FFFFFF">
                &nbsp;Level&nbsp;
            <th bgcolor="#003399" style="color:#FFFFFF">
                &nbsp;Alert title&nbsp;
            </th>
        </tr>
        </THEAD>
        <TBODY>
        <%
            for(int i = alerts.size()-1 ; i >= 0 ; i--)
            {
                SinkHandlerLogRecord alert =
                    (SinkHandlerLogRecord)alerts.get(i);
        %>
                <tr bgcolor="<%=levelColors.get(alert.getLevel())%>" <%=!alert.isRead()?"style='font-weight: bold'":""%> valign="middle">
                    <td nowrap>
                        &nbsp;<input name="alerts" value="<%=alert.getSequenceNumber()%>" type="checkbox">&nbsp;
                    </td>
                    <td nowrap>
                        &nbsp;<code><%=sdf.format(alert.getCreationTime())%> GMT</code>&nbsp;
                    </td>
                    <td nowrap>
                        &nbsp;<code><%=alert.getLevel().getName()%></code>&nbsp;
                    </td>
                    <td nowrap>
                        &nbsp;<code><a style="color: #003399;" class="underLineOnHover" href="<%=request.getContextPath()%>/console/readalert.jsp?alert=<%=alert.getSequenceNumber()%>"><%=alert.getShortMessage()%></a></code>&nbsp;
                    </td>
                </tr>
        <%
            }
        %>
        </TBODY>
    </table>
    </form>
    <p style="font-size: 10px;">
       <a style="color: #003399; text-decoration: none" href="javascript:setAll(true)">Check All</a> - <a style="color: #003399; text-decoration: none" href="javascript:setAll(false)">Clear All</a>
    <p>
        <input type="button" value="Mark selected as read" onClick="doMarkAllAsRead()">
        <input type="button" value="Delete selected" onClick="doDeleteAll()">
<% } %>
<%@include file="/include/foot.jsp"%>
