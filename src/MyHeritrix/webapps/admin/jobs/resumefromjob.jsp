<%@include file="/include/handler.jsp"%>

<%@ page import="org.archive.crawler.admin.CrawlJob" %>
<%@ page import="java.util.Iterator" %>

<%
    String title = "Resume from an existing job";
    int tab = 1;
%>

<%@include file="/include/head.jsp"%>
<p>
    <b>Select job with a checkpoint to resume:</b>
<p>
    <ul>
<%
    Iterator iter = handler.getCompletedJobs().iterator();
    while(iter.hasNext()) {
        CrawlJob job = (CrawlJob)iter.next();
        out.println("<li><a href=\"");
        out.println(request.getContextPath());
        out.println("/jobs/resumefromcheckpoint.jsp?job=" + job.getUID() +
            "\">" + job.getDisplayName());
    }
%>    
    </ul>

        
<%@include file="/include/foot.jsp"%>
