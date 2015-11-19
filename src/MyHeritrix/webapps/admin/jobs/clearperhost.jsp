<%@include file="/include/handler.jsp"%>

<%@ page import="org.archive.crawler.admin.CrawlJob,org.archive.crawler.admin.StatisticsTracker,java.util.*" %>
<%@ page import="org.archive.crawler.settings.*,java.io.File" %>


<%
 
    CrawlJob cjob = null;

    // Assume current job.
    cjob = handler.getCurrentJob();

    String title = "Clear cached per-host settings";
    int tab = 1;
%>

<%@include file="/include/head.jsp"%>

        <%
            if(cjob == null)
            {
                // NO JOB SELECTED - ERROR
        %>
                <b>Invalid job selected</b>
        <%
            }
            else
            {
                cjob.getController().getSettingsHandler().clearPerHostSettingsCache();
        %>
            <b class="flashMessage">Cleared cached per-host settings for current job 
            <%= cjob.getDisplayName() %></b>
            <p/>
            Any on-disk changes will take effect at next read.
            <a href="javascript:history.back()">Return to Jobs.</a>
        <%
            } // End if(cjob==null)else clause
        %>

<%@include file="/include/foot.jsp"%>