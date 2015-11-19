<%@include file="/include/handler.jsp"%>

<%@ page import="org.archive.crawler.admin.CrawlJob,org.archive.crawler.admin.StatisticsTracker,java.util.*,java.io.*" %>
<%@ page import="org.archive.crawler.settings.ComplexType"%>
<%@ page import="org.archive.crawler.admin.ui.JobConfigureUtils"%>
<%
    String job = request.getParameter("job");
    CrawlJob cjob = null;

    if(job != null)
    {
        cjob = handler.getJob(job);
    }
    
    String title = "View seeds";
    int tab = 1;
%>

<%@include file="/include/head.jsp"%>
    
    <body>
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
        %>
                <fieldset style="width: 600px">
                    <legend>Seed file for '<%=cjob.getJobName()%>'</legend>
                    <pre><%
                    JobConfigureUtils.printOutSeeds(cjob.getSettingsHandler(),
                        out); 
        %></pre>
                </fieldset>
        <%
            } // End if(cjob==null)else clause
        %>
        <a href="javascript:history.back()">Back</a>

<%@include file="/include/foot.jsp"%>
