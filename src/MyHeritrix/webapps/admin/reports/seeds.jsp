<%@include file="/include/handler.jsp"%> 

<%@ page import="org.archive.crawler.admin.CrawlJob" %>
<%@ page import="org.archive.crawler.admin.StatisticsTracker" %>
<%@ page import="org.archive.crawler.admin.StatisticsSummary" %>
<%@ page import="java.util.*" %>
<%@ page import="org.archive.crawler.admin.SeedRecord" %>
<%@ page import="org.archive.crawler.datamodel.CrawlURI"%>

<%
    /**
     * Page allows user to view the information on seeds in the
     * StatisticsTracker for a completed job.
     * Parameter: job - UID for the job.
     */
     
    StatisticsSummary summary = null;

    boolean viewingCurrentJob = false;
    String job = request.getParameter("job");
    
    CrawlJob cjob = null;
    if (job == null) {
        // Get job that is currently running or paused
    	cjob = handler.getCurrentJob();
    	viewingCurrentJob = true;
    } else {
		// Get job indicated in query string
    	cjob = handler.getJob(job);
    }
    
    String title = "Seeds report";
    int tab = 4;
%>

<%@include file="/include/head.jsp"%>

<%
    if(cjob == null) {
        // NO JOB SELECTED - ERROR
%>
        <p>&nbsp;<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>Invalid job selected</b>
<%
    } else if (stats == null) {
    	if (!viewingCurrentJob) {
    		summary = new StatisticsSummary(cjob);
    	}
        if (viewingCurrentJob || !summary.isStats()) {
	        out.println("<b>No statistics associated with job.</b><p>" +
	            "<b>Job status:</b> " + cjob.getStatus());            
	        if (cjob.getErrorMessage()!=null) {
	            out.println("<p><pre><font color='red'>" +
	                cjob.getErrorMessage() + "</font></pre>");
	        }
    	}
    }
    
	if ((summary != null && summary.isStats()) || stats != null) {
        String ignoredSeeds = cjob.getIgnoredSeeds(); 
        if (ignoredSeeds != null && ignoredSeeds.length() > 0) {
%>
	<b style="color:red">Items in seed specification were ignored. 
	<a href="#ignored">See below</a> for details.</b><p>
<%    
    }
%>
        <table cellspacing=0>
            <tr>
                <th style="border-bottom:solid 1px #666666;">
                    Status code
                    <br> and Disposition
                </th>
                <th style="border-bottom:solid 1px #666666;" align="left">
                    Seeds for job '<%=cjob.getJobName()%>'
                </th>
            </tr>
            <%
            	Iterator seeds = summary == null ? stats.getSeedRecordsSortedByStatusCode() :
            		summary.getSeedRecordsSortedByStatusCode();
                //Iterator seeds = stats.getSeedRecordsSortedByStatusCode();
                while (seeds.hasNext()) {
                    SeedRecord sr = (SeedRecord)seeds.next();
                    int code = sr.getStatusCode();
                    String statusCode = code==0?
                        "" : CrawlURI.fetchStatusCodesToString(code);
                    String statusColor = "black";
                    if (code<0 || code >= 400) {
                        statusColor = "red";
                    } else if(code == 200) {
                        statusColor = "green";
                    }
            %>
                    <tr >
                        <td style="border-bottom:solid 1px #666666;"
                            align="left">
             <%
                 if(code!=0) {
              %>
                            &nbsp;<font color="<%=statusColor%>"><%=statusCode%></font>&nbsp;<br>
             <%
                 }
             %>
                            <a href="<%=request.getContextPath()%>/logs.jsp?job=<%=cjob.getUID()%>&log=crawl.log&mode=regexpr&regexpr=^[^ ].*<%=sr.getUri()%>&grep=true" style="text-decoration: none;">
                            <%=sr.getDisposition()%></a>
                        </td>
                        <td style="border-bottom:solid 1px #666666;" nowrap>
                            <%=sr.getUri()%>
             <%
                if(sr.getRedirectUri()!=null) {
             %>
                        <br>&rarr; <a href="<%=sr.getRedirectUri()%>"><%=sr.getRedirectUri()%></a>
             <%
                }
             %>
                        </td>


                    </tr>
            <%
                }
            %>
        </table>

<%
    if(ignoredSeeds!=null&&ignoredSeeds.length()>0) {
%>
	<p>
	<a name="ignored"></a>
	Some items in seed specification were ignored. This may not indicate any 
	problem, but the ignored items are displayed here for reference:<p>
	
	<div style="border:2px solid pink;margin-right:50px;margin-left:50px;padding:25px">
<pre>
<%=ignoredSeeds%>
</pre>
	</div>
<%    
    }

    }
%>

<%@include file="/include/foot.jsp"%>
