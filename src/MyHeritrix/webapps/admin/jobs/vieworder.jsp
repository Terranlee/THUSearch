<%@include file="/include/handler.jsp"%>

<%@ page import="org.archive.crawler.admin.CrawlJob,org.archive.crawler.admin.StatisticsTracker,java.util.*" %>
<%@ page import="org.archive.crawler.settings.*,java.io.File" %>


<%
 
    String job = request.getParameter("job");
    CrawlJob cjob = null;

    StatisticsTracker stats = null;
    
    if(job != null)
    {
        cjob = handler.getJob(job);
        stats = (StatisticsTracker)cjob.getStatisticsTracking();
        
    }
    else
    {
        // Assume current job.
        cjob = handler.getCurrentJob();
        stats = (StatisticsTracker)cjob.getStatisticsTracking();
    }
    
    
%>
<html>
    <head>
        <title>Heritrix: View crawl order</title>
        <link rel="stylesheet" href="<%=request.getContextPath()%>/css/heritrix.css">
    </head>
    
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
            <iframe name="frmStatus" src="<%=request.getContextPath()%>/iframes/xml.jsp?file=<%=((XMLSettingsHandler)cjob.getSettingsHandler()).getOrderFile().getAbsolutePath()%>" width="100%" height="100%" frameborder="0" ></iframe>
        <%
            } // End if(cjob==null)else clause
        %>
    </body>
</html>
