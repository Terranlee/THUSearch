<%@include file="/include/handler.jsp"%>
<%@ page import="org.archive.crawler.admin.CrawlJob" %>
<%@ page import="org.archive.crawler.Heritrix" %>
<%@ page import="org.archive.crawler.admin.StatisticsTracker" %>
<%@ page import="org.archive.util.ArchiveUtils" %>
<%@ page import="org.archive.util.TextUtils" %>
<%@ page import="javax.servlet.jsp.JspWriter" %>
<%!
	private void printTime(final JspWriter out,long time)
    throws java.io.IOException {
	    out.println(ArchiveUtils.formatMillisecondsToConventional(time,false));
	}
%>
<%
    String sAction = request.getParameter("action");
    if(sAction != null) {
        if(sAction.equalsIgnoreCase("logout")) {
            // Logging out.
            session = request.getSession();
            if (session != null) {
                session.invalidate();
                // Redirect back to here and we'll get thrown to the login
                // page.
                response.sendRedirect(request.getContextPath() + "/index.jsp"); 
            }
        }
    }

    String title = "Admin Console";
    int tab = 0;
%>

<%@include file="/include/head.jsp"%>
    
    <script type="text/javascript">
        function doTerminateCurrentJob(){
            if(confirm("Are you sure you wish to terminate the job currently being crawled?")){
                document.location = '<%out.print(request.getContextPath());%>/console/action.jsp?action=terminate';
            }
        }    
    </script>
    
    <table border="0" cellspacing="0" cellpadding="0"><tr><td>
    <fieldset style="width: 750px">
        <legend> 
        <b><span class="legendTitle">Crawler Status:</span> 
        <%= handler.isRunning() 
            ? "<span class='status crawling'>CRAWLING JOBS</span></b> | "
              +"<a href='"+request.getContextPath()+"/console/action.jsp?action=stop'>Hold</a>"
            : "<span class='status holding'>HOLDING JOBS</span></b> | "
              +"<a href='"+request.getContextPath()+"/console/action.jsp?action=start'>Start</a>"
        %> </b>
        </legend>
        <div style="float:right;padding-right:50px;">
	        <b>Memory</b><br>
	        <div style="padding-left:20px">
		        <%=(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024%> KB 
		        used<br>
		        <%=(Runtime.getRuntime().totalMemory())/1024%> KB
		        current heap<br>
		        <%=(Runtime.getRuntime().maxMemory())/1024%> KB
		        max heap
	        </div>
	    </div>
        <b>Jobs</b>
        <div style="padding-left:20px">
			<%= handler.getCurrentJob()!=null
			    ? shortJobStatus+": <i>"
			      +handler.getCurrentJob().getJobName()+"</i>"
			    : ((handler.isRunning()) ? "None available" : "None running")
			 %><br>
	        <%= handler.getPendingJobs().size() %> pending,
	        <%= handler.getCompletedJobs().size() %> completed
        </div>

        <b>Alerts:</b>
	        <a style="color: #000000" 
	            href="<%=request.getContextPath()%>/console/alerts.jsp">
	            <%=heritrix.getAlertsCount()%> (<%=heritrix.getNewAlertsCount()%> new)
	        </a>
	        
         </fieldset>
            <%
            	long begin, end;
	            if(stats != null) {
	                begin = stats.successfullyFetchedCount();
	                end = stats.totalCount();
	                if(end < 1) {
	                    end = 1;
	                }
	            } else {
                    begin = 0;
                    end = 1;
	            }
                
                if(handler.getCurrentJob() != null)
                {
                    final long timeElapsed, timeRemain;
                    if(stats == null) {
                        timeElapsed= 0;
                        timeRemain = -1;
                    } else {
	                    timeElapsed = (stats.getCrawlerTotalElapsedTime());
	                    if(begin == 0) {
	                        timeRemain = -1;
	                    } else {
	                        timeRemain = ((long)(timeElapsed*end/(double)begin))-timeElapsed;
	                    }
                    }
            %>
            <fieldset style="width: 750px">
               <legend>
               <b><span class="legendTitle">Job Status:</span>
               <%= 
               "<span class='status "
               +shortJobStatus+"'>"
               +shortJobStatus+"</span>"
               %>
               </b> 
<%      
    if(handler.isCrawling()) {
	    if ((handler.getCurrentJob().getStatus().
                equals(CrawlJob.STATUS_PAUSED) ||
            handler.getCurrentJob().getStatus().
			    equals(CrawlJob.STATUS_WAITING_FOR_PAUSE))) {
            out.println("| <a href='/console/action.jsp?action=resume'>" +
                "Resume</a>");
            out.println(" | ");
            out.println("<a href=\"");
            out.println(request.getContextPath());
            out.println("/console/action.jsp?action=checkpoint\">" +
                "Checkpoint</a>");
        } else if (!handler.getCurrentJob().isCheckpointing()) {
            out.println("| <a href=\"");
            out.println(request.getContextPath());
            out.println("/console/action.jsp?action=pause\">Pause</a> ");
            if (!handler.getCurrentJob().getStatus().
                   equals(CrawlJob.STATUS_PENDING)) {
                out.println(" | ");
                out.println("<a href=\"");
                out.println(request.getContextPath());
                out.println("/console/action.jsp?action=checkpoint\">" +
                    "Checkpoint</a>");
            }
        }
        out.println(" | <a href='javascript:doTerminateCurrentJob()'>" +
            "Terminate</a>");
    }
%>
               </legend>

                <%
                  if(handler.isCrawling() && stats != null)
                  {
                %>
                	<div style="float:right; padding-right:50px;">
                	    <b>Load</b>
            			<div style="padding-left:20px">
			            	<%=stats.activeThreadCount()%> active of <%=stats.threadCount()%> threads
			            	<br>
			            	<%=ArchiveUtils.doubleToString((double)stats.congestionRatio(),2)%>
			            	congestion ratio
			            	<br>
			            	<%=stats.deepestUri()%> deepest queue
			            	<br>
			            	<%=stats.averageDepth()%> average depth
						</div>
					</div>
	                <b>Rates</b>
	                <div style="padding-left:20px">
		                <%=ArchiveUtils.doubleToString(stats.currentProcessedDocsPerSec(),2)%> 		                
		                URIs/sec
		                (<%=ArchiveUtils.doubleToString(stats.processedDocsPerSec(),2)%> avg)
		                <br>
		                <%=stats.currentProcessedKBPerSec()%>
						KB/sec
						(<%=stats.processedKBPerSec()%> avg)
					</div>

                    <b>Time</b>
                    <div class='indent'>
	                    <%= ArchiveUtils.formatMillisecondsToConventional(timeElapsed,false) %>
						elapsed
						<br>
	                    <%
	                       if(timeRemain != -1) {
	                    %>
		                    <%= ArchiveUtils.formatMillisecondsToConventional(timeRemain,false) %>
		                    remaining (estimated)
		               	<%
	                       }
                   		%>
					</div>
                    <b>Totals</b>
                	<%
                          }
                }
                if(stats != null)
                {
	                int ratio = (int) (100 * begin / end);
            %>
                            <center>
                            <table border="0" cellpadding="0" cellspacing= "0" width="600"> 
                                <tr>
                                    <td align='right' width="25%">downloaded <%= begin %>&nbsp;</td>
                                    <td class='completedBar' width="<%= (int)ratio/2 %>%" align="right">
                                    <%= ratio > 50 ? "<b>"+ratio+"</b>%&nbsp;" : "" %>
                                    </td>
                                    <td class='queuedBar' align="left" width="<%= (int) ((100-ratio)/2) %>%">
                                    <%= ratio <= 50 ? "&nbsp;<b>"+ratio+"</b>%" : "" %>
                                    </td>
                                    <td width="25%" nowrap>&nbsp;<%= stats.queuedUriCount() %> queued</td>
                                </tr>
                            </table>
                            <%= end %> total downloaded and queued<br>      
                    		<%=stats.crawledBytesSummary()%>
                            </center>
            <%
                }
                if (handler.getCurrentJob() != null &&
                	handler.getCurrentJob().getStatus().equals(CrawlJob.STATUS_PAUSED)) {
            %>
            		<b>Paused Operations</b>
            		<div class='indent'>
	                	<a href='<%= request.getContextPath() %>/console/frontier.jsp'>View or Edit Frontier URIs</a>
	                </div>
	        <%
            	}
            %>
    </fieldset>
    </td></tr>
    <tr><td>
    
	<a href="<%=request.getContextPath()%>/">Refresh</a>
    </td></tr>
    <tr><td>
        <p>
            &nbsp;
        <p>
            &nbsp;
    </td></tr>
    <tr><td>
        <% if (heritrix.isCommandLine()) {  
            // Print the shutdown only if we were started from command line.
            // It makes no sense when in webcontainer mode.
         %>
        <a href="<%=request.getContextPath()%>/console/shutdown.jsp">Shut down Heritrix software</a> |
        <% } %>
        <a href="<%=request.getContextPath()%>/index.jsp?action=logout">Logout</a>
    </td></tr></table>
<%@include file="/include/foot.jsp"%>
