<%@include file="/include/handler.jsp"%>

<%@ page import="java.util.*" %>
<%@ page import="java.util.concurrent.atomic.AtomicLong"%>
<%@ page import="org.archive.crawler.datamodel.CrawlURI"%>
<%@ page import="org.archive.util.ArchiveUtils" %>
<%@ page import="org.archive.crawler.admin.*" %>
<%
     /**
     * Page allows user to view the information on seeds in the
     * StatisticsTracker for a completed job.
     * Parameter: job - UID for the job.
     */

    // Pixel width of widest bar
    final int MAX_BAR_WIDTH = 450;

    // Indicates if we are viewing a completed job report or the
    // current job report
    boolean viewingCurrentJob = false;

    String job = request.getParameter("job");
    //CrawlJob cjob = (job != null)? handler.getJob(job): handler.getCurrentJob();
    CrawlJob cjob = null;
    if (job == null) {
    	// Get job that is currently running or paused
    	cjob = handler.getCurrentJob();
    	viewingCurrentJob = true;
    }
    else {
    	// Get job indicated in query string
    	cjob = handler.getJob(job);
    }     
    
    String title = "Crawl job report"; 
    int tab = 4;
%>

<%@include file="/include/head.jsp"%>

<style>
	tr.headerrow {background-color: #ccccff }
	tr.totalrow {background-color: #ccccff }
	.percent {font-size: 70%}
</style>

<%
    // Do this uptop here. Needed before I go into the if/else that follows.
    StatisticsSummary summary = null;
    if (cjob != null) {
        summary = new StatisticsSummary(cjob);
    }

    if(cjob == null)
    {
    	viewingCurrentJob = false;
    	
        // NO JOB SELECTED - ERROR
%>
        <p>&nbsp;<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>Invalid job selected.</b>
<%
		} else if(!viewingCurrentJob && summary != null && summary.isStats()) {
		// If the job selected is not the current job, then show stats
        // for completed job
		java.text.DecimalFormat percentFormat =
            new java.text.DecimalFormat("#####.#");
%>
        <table border="0">
            <tr>
                <td valign="top">
                    <table border="0" cellspacing="0" cellpadding="0" >
                        <tr>
                            <td>
                                <b>Job name:</b>&nbsp;
                            </td>
                            <td>
                                <%=cjob.getJobName()%>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <b>Status:</b>&nbsp;
                            </td>
                            <td>
                                <%=cjob.getStatus()%>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <b>Time:</b>&nbsp;
                            </td>
                            <td>
                               <%=summary.getDurationTime()%>
                            </td>
                        </tr>
                    </table>
                </td>
                <td>
                    &nbsp;&nbsp;&nbsp;
                </td>
                <td valign="top">
                    <table border="0" cellspacing="0" cellpadding="0" >
                        <tr>
                            <td>
                                <b>Processed docs/sec:</b>&nbsp;
                            </td>
                            <td>                           
                               <%=summary.getProcessedDocsPerSec()%>                              
                            </td>
                            </tr>
                        <tr>
                        <td>
                                <b>Processed KB/sec:</b>&nbsp;
                            </td>
                            <td>
                               <%=summary.getBandwidthKbytesPerSec()%>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <b>Total data written:</b>&nbsp;
                            </td>
                            <td>
                                <%=summary.getTotalDataWritten()%>
                            </td>
                        </tr>
                    </table>
                </td>
            </tr>
        </table>                       
    
    <p>
    </p>
          
          
    <table width=750>
	    <tr>
	        <td valign="center" ><img 
	        src="<%=request.getContextPath()%>/images/blue.jpg" height="1" width="40"></td>
	        <td align="center"><i>HTTP</i></td>
	        <td valign="center" ><img 
	        src="<%=request.getContextPath()%>/images/blue.jpg" height="1" width="660"></td>
	    </tr>
	</table>
	
	<table cellspacing="0" width=750>
	        
        	<tr class="headerrow">
                <th>
                    Status code
                </th>
                <th width="200" colspan="2">
                    Documents
                </th>
            </tr>
            <%
                            boolean alt = true;
                            TreeMap scd = summary.getReverseSortedCopy(summary.getStatusCodeDistribution());
                            for (Iterator i = scd.keySet().iterator(); i.hasNext();) {
                                Object key = i.next();
                                long count = ((AtomicLong)scd.get(key)).get();
                                long displaybarwidth = 0;
                                long barwidthadjust = 5;
                                double per = ((double)count) / summary.getTotalStatusCodeDocuments();
                                if(summary.getTotalStatusCodeDocuments() > 0){
                                   displaybarwidth = (long)(per * MAX_BAR_WIDTH);
                                } 
                                if(displaybarwidth==0){
                                   displaybarwidth=1;
                                }
                                
                                String percent = percentFormat.format(100 * per);
            %>
                    <tr <%=alt?"bgcolor=#EEEEFF":""%>>
                        <td nowrap>
                            <a style="text-decoration: none;" href="<%=request.getContextPath()%>/logs.jsp?job=<%=cjob.getUID()%>&log=crawl.log&mode=regexpr&regexpr=^.{24}\s*<%=(String)key%>&grep=true">
                                <%=CrawlURI.fetchStatusCodesToString(Integer.parseInt((String)key))%>
                            </a>&nbsp;
                        </td>
                        <td colspan="2" nowrap>
                            <img src="<%=request.getContextPath()%>/images/blue.jpg" height="10" width="<%=displaybarwidth%>"> <%=count%> &nbsp;
                            	<span class=percent>(<%=percent%>%)</span>
                        </td>
                    </tr>
            <%
                            alt = !alt;
                            }
            %>                
            <tr class="totalrow">
            	<td><b>Total:</b></td>
            	<td><%=summary.getTotalStatusCodeDocuments()%> &nbsp; </td>
            	<td>&nbsp;</td>
            </tr>
            <tr>
                <td>&nbsp;</td>
            </tr>
            <tr class="headerrow">
                <th width="100">
                    MIME type
                </th>
                <th width="200">
                    Documents
                </th>
                <th>
                    Data
                </th>
            </tr>
            <%
                            alt = true;
                            TreeMap fd = summary.getReverseSortedCopy(summary.getMimeDistribution());
                            for (Iterator i = fd.keySet().iterator(); i.hasNext();) {
                                Object key = i.next();
                                long count = ((AtomicLong)fd.get(key)).get();
                                long displaybarwidth = 0;
                                double per = ((double)count) / summary.getTotalMimeTypeDocuments();
                                if(summary.getTotalMimeTypeDocuments()>1){
                                   displaybarwidth = (long)(per * MAX_BAR_WIDTH);
                                } 
                                if(displaybarwidth==0){
                                   displaybarwidth=1;
                                }
                                
                                String percent = percentFormat.format(100 * per);
            %>
                    <tr <%=alt?"bgcolor=#EEEEFF":""%>>
                        <td nowrap>
                            <a style="text-decoration: none;" href="<%=request.getContextPath()%>/logs.jsp?job=<%=cjob.getUID()%>&log=crawl.log&mode=regexpr&regexpr=^[^ ].*<%=(String)key%>&grep=true"><%=key%></a>&nbsp;&nbsp;
                        </td>
                        <td nowrap>
                            <img src="<%=request.getContextPath()%>/images/blue.jpg" height="10" width="<%=displaybarwidth%>"> <%=count%> &nbsp;
                            	<span class=percent>(<%=percent%>%)</span>
                        </td>
                        <td align="right" nowrap>
                            <%=ArchiveUtils.formatBytesForDisplay(summary.getBytesPerMimeType((String)key))%>&nbsp;
                        </td>
                    </tr>
            <%
                            alt = !alt;
                            }
            %>   
           
            <tr class="totalrow">
  	        	<td><b>Total</b></td>
  	        	<td><%=summary.getTotalMimeTypeDocuments()%> &nbsp; </td> 
  	        	<td align="right" nowrap>
        	      		<%=ArchiveUtils.formatBytesForDisplay(summary.getTotalMimeSize())%>&nbsp;
        	    </td>
            </tr>
            	    
        	<tr>
        		<td>&nbsp;</td>
        	</tr>
            <tr class="headerrow">
                <th>
                    Hosts
                </th>
                <th>
                    Documents
                </th>
                <th>
                    Data
                </th>
            </tr>
            <%
                            alt = true;
                            SortedMap hd = summary.getReverseSortedHostsDistribution();
                            for (Iterator i = hd.keySet().iterator(); i.hasNext();) {
                                Object key = i.next();
                                AtomicLong lw = (AtomicLong)hd.get(key);
                                long count = lw == null ? 0 : lw.get();
                                long displaybarwidth = 0;
                                double per = ((double)count) / summary.getTotalHostDocuments();
                                if(summary.getTotalHostDocuments() > 1) {
                                   displaybarwidth = (long)(per * MAX_BAR_WIDTH);
                                } 
                                if(displaybarwidth==0){
                                   displaybarwidth=1;
                                }
                                
                                String percent = percentFormat.format(100 * per);
            %>
                    <tr <%=alt?"bgcolor=#EEEEFF":""%>>
                        <td nowrap>
                            <a style="text-decoration: none;" href="<%=request.getContextPath()%>/logs.jsp?job=<%=cjob.getUID()%>&log=crawl.log&mode=regexpr&regexpr=^[^ ].*<%=(String)key%>&grep=true"><%=(String)key%></a>&nbsp;
                        </td>
                        <td nowrap>
                            <img src="<%=request.getContextPath()%>/images/blue.jpg" height="10" width="<%=displaybarwidth%>"> <%=count%> &nbsp;
                            	<span class=percent>(<%=percent%>%)</span>
                        </td>
                        <td align="right" nowrap>
                            <%=ArchiveUtils.formatBytesForDisplay(summary.getBytesPerHost((String)key))%>&nbsp;
                        </td>                      
                    </tr>
            <%
                            alt = !alt;
                            }
            %>                
            <tr class="totalrow">
  	        	<td><b>Total</b></td>
  	        	<td><%=summary.getTotalHostDocuments()%> &nbsp; </td> 
  	        	<td align="right" nowrap>
        	      		<%=ArchiveUtils.formatBytesForDisplay(summary.getTotalHostSize())%>&nbsp;
        	    </td>
            </tr>
            
            <tr>
            	<td>&nbsp;</td>
            </tr>
            
            </table>
            
            
            
            <table cellspacing="0" width=750>	   
                
            <tr class="headerrow">
                <th width=70>
                    TLD
                </th>
                <th width=100>
                	Hosts
                </th>
                <th>
                    Documents
                </th>
                <th>
                    Data
                </th>
            </tr>
            <%
                        	alt = true;
                        	scd = summary.getReverseSortedCopy(summary.getTldDistribution());
                            for (Iterator i = scd.keySet().iterator(); i.hasNext();) {
                                Object key = i.next();
                                AtomicLong lw = (AtomicLong)scd.get(key);
                                long count = lw == null ? 0 : lw.get();
                                long displaybarwidth = 0;
                                double per = ((double)count) / summary.getTotalTldDocuments();
                                
                                long hostsPerTld = summary.getHostsPerTld((String)key);
                                double perHost = ((double)hostsPerTld) / summary.getTotalHosts();
                                
                                if (summary.getTotalTldDocuments() > 1) {
                                   displaybarwidth = (long)(per * MAX_BAR_WIDTH);
                                } 
                                if (displaybarwidth == 0){
                                   displaybarwidth = 1;
                                }
                                
                                String percent = percentFormat.format(100 * per);
                                String percentHost = percentFormat.format(100 * perHost);
            %>
                    <tr <%=alt?"bgcolor=#EEEEFF":""%>>
                        <td nowrap>
                            <a style="text-decoration: none;" href="<%=request.getContextPath()%>/logs.jsp?job=<%=cjob.getUID()%>&log=crawl.log&mode=regexpr&regexpr=^[^ ].*<%=(String)key%>&grep=true"><%=(String)key%></a>&nbsp;
                        </td>
                        <td>
                        	<%=hostsPerTld%> &nbsp; <span class=percent>(<%=percentHost%>%)</span>
                        </td>
                        <td nowrap>
                            <img src="<%=request.getContextPath()%>/images/blue.jpg" height="10" width="<%=displaybarwidth%>"> <%=count%> &nbsp;
                            	<span class=percent>(<%=percent%>%)</span>
                        </td>
                        <td align="right" nowrap>
                            <%=ArchiveUtils.formatBytesForDisplay(summary.getBytesPerTld((String)key))%>&nbsp;
                        </td>                      
                    </tr>
            <%
                            alt = !alt;
                            }
            %>                
            <tr class="totalrow">
  	        	<td><b>Total</b></td>
  	        	<td><%=summary.getTotalHosts()%></td>
  	        	<td><%=summary.getTotalTldDocuments()%> &nbsp; </td> 
  	        	<td align="right" nowrap>
        	      		<%=ArchiveUtils.formatBytesForDisplay(summary.getTotalTldSize())%>&nbsp;
        	    </td>
            </tr>                   
            
        </table>
        
         <p>
		<br>
		</p>
		
	<table width=750>
	    <tr>
	        <td valign="center" ><img 
	        src="<%=request.getContextPath()%>/images/blue.jpg" height="1" width="40"></td>
	        <td align="center"><i>DNS</i></td>
	        <td valign="center" ><img 
	        src="<%=request.getContextPath()%>/images/blue.jpg" height="1" width="660"></td>
	    </tr>
	</table>
	
         <table cellspacing="0" width=750>
	        
        	<tr class="headerrow">
                <th>
                    Status code
                </th>
                <th width="200" colspan="2">
                    Documents
                </th>
            </tr>
            <%
                            alt = true;
                            scd = summary.getReverseSortedCopy(summary.getDnsStatusCodeDistribution());
                            for (Iterator i = scd.keySet().iterator(); i.hasNext();) {
                                Object key = i.next();
                                long count = ((AtomicLong)scd.get(key)).get();
                                long displaybarwidth = 0;
                                long barwidthadjust = 3;
                                double per = ((double)count) / summary.getTotalDnsStatusCodeDocuments(); 
                                if(summary.getTotalDnsStatusCodeDocuments()/barwidthadjust>0){
                                   displaybarwidth = (long)(per * MAX_BAR_WIDTH);
                                } 
                                if(displaybarwidth==0){
                                   displaybarwidth=1;
                                }
                                             
                                String percent = percentFormat.format(100 * per);
            %>
                    <tr <%=alt?"bgcolor=#EEEEFF":""%>>
                        <td nowrap>
                            <a style="text-decoration: none;" href="<%=request.getContextPath()%>/logs.jsp?job=<%=cjob.getUID()%>&log=crawl.log&mode=regexpr&regexpr=^.{24}\s*<%=(String)key%>&grep=true">
                                <%=CrawlURI.fetchStatusCodesToString(Integer.parseInt((String)key))%>
                            </a>&nbsp;
                        </td>
                        <td colspan="2" nowrap>
                            <img src="<%=request.getContextPath()%>/images/blue.jpg" height="10" width="<%=displaybarwidth%>"> <%=count%> &nbsp;
                            	<span class=percent>(<%=percent%>%)</span>
                        </td>
                    </tr>
            <%
                            alt = !alt;
                            }
            %>                
            <tr class="totalrow">
            	<td><b>Total:</b></td>
            	<td><%=summary.getTotalDnsStatusCodeDocuments()%> &nbsp; </td>
            	<td>&nbsp;</td>
            </tr>
            <tr>
                <td>&nbsp;</td>
            </tr>
            <tr class="headerrow">
                <th width="100">
                    MIME type
                </th>
                <th width="200">
                    Documents
                </th>
                <th>
                    Data
                </th>
            </tr>
            <%
                            alt = true;
                            fd = summary.getReverseSortedCopy(summary.getDnsMimeDistribution());
                            for (Iterator i = fd.keySet().iterator(); i.hasNext();) {
                                Object key = i.next();
                                long count = ((AtomicLong)fd.get(key)).get();
                                long displaybarwidth = 0;
                                double per = ((double)count)/summary.getTotalDnsMimeTypeDocuments();
                                if(summary.getTotalMimeTypeDocuments()/6>0){
                                   displaybarwidth = (long)(per * MAX_BAR_WIDTH);
                                } 
                                if(displaybarwidth==0){
                                   displaybarwidth=1;
                                }
                                
                                String percent = percentFormat.format(100 * per);
            %>
                    <tr <%=alt?"bgcolor=#EEEEFF":""%>>
                        <td nowrap>
                            <a style="text-decoration: none;" href="<%=request.getContextPath()%>/logs.jsp?job=<%=cjob.getUID()%>&log=crawl.log&mode=regexpr&regexpr=^[^ ].*<%=(String)key%>&grep=true"><%=key%></a>&nbsp;&nbsp;
                        </td>
                        <td nowrap>
                            <img src="<%=request.getContextPath()%>/images/blue.jpg" height="10" width="<%=displaybarwidth%>"> <%=count%> &nbsp;
                             <span class=percent>(<%=percent%>%)</span>
                        </td>
                        <td align="right" nowrap>
                            <%=ArchiveUtils.formatBytesForDisplay(summary.getBytesPerMimeType((String)key))%>&nbsp;                           
                        </td>
                    </tr>
            <%
                            alt = !alt;
                            }
            %>   
		            <tr class="totalrow">
        	        	<td><b>Total</b></td>
        	        	<td>
        	        		<%=summary.getTotalDnsMimeTypeDocuments()%> &nbsp; 
        	        	</td> 
        	        	<td align="right" nowrap>
        	        		<%=ArchiveUtils.formatBytesForDisplay(summary.getTotalDnsMimeSize())%>&nbsp;
        	        	</td>
            	    </tr>
            	    
            	    <tr>
            	<td>&nbsp;</td>
            </tr>
            
            	    <tr class="headerrow">
                <th>
                    Hosts
                </th>
                <th>
                    Documents
                </th>
                <th>
                    Data
                </th>
            </tr>
            <%
                            alt = true;
                            hd = summary.getReverseSortedCopy(summary.getHostsDnsDistribution());
                            for (Iterator i = hd.keySet().iterator(); i.hasNext();) {
                                Object key = i.next();
                                AtomicLong lw = (AtomicLong)hd.get(key);
                                long count = lw == null ? 0 : lw.get();
                                long displaybarwidth = 0;
                                double per = ((double)count) / summary.getTotalHostDnsDocuments();
                                if(summary.getTotalHostDnsDocuments() > 1) {
                                   displaybarwidth = (long)(per * MAX_BAR_WIDTH);
                                } 
                                if(displaybarwidth==0){
                                   displaybarwidth=1;
                                }
                                
                                String percent = percentFormat.format(100 * per);
            %>
                    <tr <%=alt?"bgcolor=#EEEEFF":""%>>
                        <td nowrap>
                            <a style="text-decoration: none;" href="<%=request.getContextPath()%>/logs.jsp?job=<%=cjob.getUID()%>&log=crawl.log&mode=regexpr&regexpr=^[^ ].*<%=(String)key%>&grep=true"><%=(String)key%></a>
                        </td>
                        <td nowrap>
                            <img src="<%=request.getContextPath()%>/images/blue.jpg" height="10" width="<%=displaybarwidth%>"> <%=count%> &nbsp;
                            	<span class=percent>(<%=percent%>%)</span>
                        </td>
                        <td align="right" nowrap>
                            <%=ArchiveUtils.formatBytesForDisplay(summary.getBytesPerHost((String)key))%>&nbsp;
                        </td>                      
                    </tr>
            <%
                            alt = !alt;
                            }
            %>                
            <tr class="totalrow">
  	        	<td><b>Total</b></td>
  	        	<td><%=summary.getTotalDnsHostDocuments()%> &nbsp; </td> 
  	        	<td align="right" nowrap>
        	      		<%=ArchiveUtils.formatBytesForDisplay(summary.getTotalDnsHostSize())%>&nbsp;
        	    </td>
            </tr>
            
            <tr>
            	<td>&nbsp;</td>
            </tr>
	</table>
 <%
         } else if(stats == null) {
         out.println("<b>No statistics associated with job.</b><p><b>Job status:</b> " + cjob.getStatus());            
         if(cjob.getErrorMessage()!=null){
             out.println("<p><pre><font color='red'>"+cjob.getErrorMessage()+"</font></pre>");
         }
     } else {
 %>
        <table border="0">
            <tr>
                <td valign="top">
                    <table border="0" cellspacing="0" cellpadding="0" >
                        <tr>
                            <td>
                                <b>Job name:</b>&nbsp;
                            </td>
                            <td>
                                <%=cjob.getJobName()%>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <b>Status:</b>&nbsp;
                            </td>
                            <td>
                                <%=cjob.getStatus()%>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <b>Time:</b>&nbsp;
                            </td>
                            <td>
                                <%
                                                                    long time = (stats.getCrawlerTotalElapsedTime())/1000;
                                                                    if(time>3600)
                                                                    {
                                                                        //got hours.
                                                                        out.println(time/3600 + " h., ");
                                                                        time = time % 3600;
                                                                    }
                                                                    
                                                                    if(time > 60)
                                                                    {
                                                                        out.println(time/60 + " min. and ");
                                                                        time = time % 60;
                                                                    }

                                                                    out.println(time + " sec.");
                                %>
                            </td>
                        </tr>
                    </table>
                </td>
                <td>
                    &nbsp;&nbsp;&nbsp;
                </td>
                <td valign="top">
                    <table border="0" cellspacing="0" cellpadding="0" >
                        <tr>
                            <td>
                                <b>Processed docs/sec:</b>&nbsp;
                            </td>
                            <td>
                                <%
                                                                    if(cjob.getStatus().equalsIgnoreCase(CrawlJob.STATUS_RUNNING))
                                                                    {
                                                                        // Show current and overall stats.
                                %>
                                        <%=ArchiveUtils.doubleToString(stats.currentProcessedDocsPerSec(),2)%> (<%=ArchiveUtils.doubleToString(stats.processedDocsPerSec(),2)%>)
                                <%
                                                                            }
                                                                            else
                                                                            {
                                                                                // Only show overall stats.
                                        %>
                                        <%=ArchiveUtils.doubleToString(stats.processedDocsPerSec(),2)%>
                                <%
                                }
                                %>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <b>Processed KB/sec:</b>&nbsp;
                            </td>
                            <td>
                                <%
                                                                    if(cjob.getStatus().equalsIgnoreCase(CrawlJob.STATUS_RUNNING))
                                                                    {
                                                                        // Show current and overall stats.
                                %>
                                        <%=ArchiveUtils.doubleToString(stats.currentProcessedKBPerSec(),2)%> (<%=ArchiveUtils.doubleToString(stats.processedKBPerSec(),2)%>)
                                <%
                                                                            }
                                                                            else
                                                                            {
                                                                                // Only show overall stats.
                                        %>
                                        <%=ArchiveUtils.doubleToString(stats.processedKBPerSec(),2)%>
                                <%
                                }
                                %>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <b>Total data written:</b>&nbsp;
                            </td>
                            <td>
                                <%=ArchiveUtils.formatBytesForDisplay(stats.totalBytesWritten())%>
                            </td>
                        </tr>
                    </table>
                </td>
            </tr>
        </table>
        
        <p>
        
        <table width="400">
            <tr>
                <td colspan="6">
                    <table>
                        <tr>
                            <td valign="center" ><img 
                            src="<%=request.getContextPath()%>/images/blue.jpg" height="1" width="40"></td>
                            <td align="center"><i>URIs</i></td>
                            <td valign="center" ><img 
                            src="<%=request.getContextPath()%>/images/blue.jpg" height="1" width="300"></td>
                        </tr>
                    </table>
                </td>
            </tr>
            <tr>
                <td nowrap>
                    <b>Discovered:</b>
                </td>
                <td align="right">
                    <%=stats.discoveredUriCount()%>
                </td>
                <td colspan="3">
                    &nbsp;<a class='help' href="javascript:alert('URIs that the crawler has discovered and confirmed to be within scope. \nNOTE: Because the same URI can be fetched multiple times this number may be lower then the number of queued, in process and finished URIs.')">?</a>
                </td>
            </tr>
            <tr>
                <td nowrap>
                    &nbsp;&nbsp;<b>Queued:</b>
                </td>
                <td align="right">
                    <%=stats.queuedUriCount()%>
                </td>
                <td colspan="3">
                    &nbsp;<a class='help' href="javascript:alert('URIs that are waiting to be processed. \nThat is all URI that have been discovered (or should be revisited) that are waiting for processing.')">?</a>
                </td>
            </tr>
            <tr>
                <td nowrap>
                    &nbsp;&nbsp;<b>In progress:</b>
                </td>
                <td align="right">
                    <%=stats.activeThreadCount()%>
                </td>
                <td colspan="3">
                    &nbsp;<a class='help' href="javascript:alert('Number of URIs being processed at the moment. \nThis is based on the number of active threads.')">?</a>
                </td>
            </tr>
            <tr>
                <td>
                </td>
                <td align="right" width="1" nowrap>
                    &nbsp;&nbsp;<i>Total</i>
                </td>
                <td align="right" width="1" nowrap>
                    &nbsp;&nbsp;<i>Successfully</i>
                </td>
                <td align="right" width="1" nowrap>
                    &nbsp;&nbsp;<i>Failed</i>
                </td>
                <td align="right" width="1" nowrap>
                    &nbsp;&nbsp;<i>Disregarded</i>
                </td>
            </tr>
            <tr>
                <td nowrap>
                    &nbsp;&nbsp;<b>Finished:</b>
                </td>
                <td align="right">
                    <%=stats.finishedUriCount()%>
                </td>
                <td align="right">
                    <%=stats.successfullyFetchedCount()%>
                </td>
                <td align="right">
                    <%=stats.failedFetchAttempts()%>
                </td>
                <td align="right">
                    <%=stats.disregardedFetchAttempts()%>
                </td>
            </tr>
        </table>
        
        <p>

        <table cellspacing="0">
            <tr>
                <th>
                    Status code
                </th>
                <th width="200" colspan="2">
                    Documents
                </th>
            </tr>
            <%
                            boolean alt = true;
                            TreeMap scd = stats.getReverseSortedCopy(stats.
                                getStatusCodeDistribution());
                            for (Iterator i = scd.keySet().iterator();
                                    i.hasNext();) {
                                Object key = i.next();
                                long count = ((AtomicLong)scd.get(key)).get();
                                long displaybarwidth = 0;
                                if(stats.successfullyFetchedCount()/6>0){
                                   displaybarwidth = count*100/(stats.successfullyFetchedCount()/6);
                                } 
                                if(displaybarwidth==0){
                                   displaybarwidth=1;
                                }
            %>
                    <tr <%=alt?"bgcolor=#EEEEFF":""%>>
                        <td nowrap>
                            <a style="text-decoration: none;" href="<%=request.getContextPath()%>/logs.jsp?job=<%=cjob.getUID()%>&log=crawl.log&mode=regexpr&regexpr=^.{24}\s*<%=(String)key%>&grep=true">
                                <%=CrawlURI.fetchStatusCodesToString(Integer.parseInt((String)key))%>
                            </a>&nbsp;
                        </td>
                        <td colspan="2" nowrap>
                            <img src="<%=request.getContextPath()%>/images/blue.jpg" height="10" width="<%=displaybarwidth%>"> <%=count%> &nbsp;
                        </td>
                    </tr>
            <%
                    alt = !alt;
                }
            %>                
            <tr>
                <td>&nbsp;</td>
            </tr>
            <tr>
                <th width="100">
                    File type
                </th>
                <th width="200">
                    Documents
                </th>
                <th>
                    Data
                </th>
            </tr>
            <%
                alt=true;
                TreeMap fd = stats.getReverseSortedCopy(stats.
                        getFileDistribution());
                for (Iterator i = fd.keySet().iterator(); i.hasNext();) {
                    Object key = i.next();
                    long count = ((AtomicLong)fd.get(key)).get();
                    long displaybarwidth = 0;
                    if(stats.successfullyFetchedCount()/6>0){
                       displaybarwidth = count*100/(stats.successfullyFetchedCount()/6);
                    } 
                    if(displaybarwidth==0){
                       displaybarwidth=1;
                    }
            %>
                    <tr <%=alt?"bgcolor=#EEEEFF":""%>>
                        <td nowrap>
                            <a style="text-decoration: none;" href="<%=request.getContextPath()%>/logs.jsp?job=<%=cjob.getUID()%>&log=crawl.log&mode=regexpr&regexpr=^[^ ].*<%=(String)key%>&grep=true"><%=key%></a>&nbsp;&nbsp;
                        </td>
                        <td nowrap>
                            <img src="<%=request.getContextPath()%>/images/blue.jpg" height="10" width="<%=displaybarwidth%>"> <%=count%> &nbsp;&nbsp;
                        </td>
                        <td align="right" nowrap>
                            <%=ArchiveUtils.formatBytesForDisplay(stats.getBytesPerFileType((String)key))%>
                        </td>
                    </tr>
            <%
                    alt = !alt;
                }
            %>                
        </table>
        
        <p>
        
        <table cellspacing="0">
            <tr>
                <th>
                    Hosts&nbsp;
                </th>
                <th>
                    Documents&nbsp;
                </th>
                <th>
                    Data&nbsp;
                </th>
                <% if (cjob.getStatus().equals(CrawlJob.STATUS_RUNNING) ||
                         cjob.getStatus().equals(CrawlJob.STATUS_PAUSED) ||
                         cjob.getStatus().equals(CrawlJob.STATUS_WAITING_FOR_PAUSE)){ %>
                    <th>
                        Time since last URI finished
                    </th>
                <% } %>
            </tr>
            <%
                            alt = true;
                            SortedMap hd = stats.getReverseSortedHostsDistribution();
                            for (Iterator i = hd.keySet().iterator(); i.hasNext();) {
                                Object key = i.next();
            %>
                    <tr <%=alt?"bgcolor=#EEEEFF":""%>>
                        <td nowrap>
                            <a style="text-decoration: none;" href="<%=request.getContextPath()%>/logs.jsp?job=<%=cjob.getUID()%>&log=crawl.log&mode=regexpr&regexpr=^[^ ].*<%=(String)key%>&grep=true"><%=(String)key%></a>&nbsp;
                        </td>
                        <td nowrap>
                            <% AtomicLong lw = ((AtomicLong)hd.get(key)); %>
                            <%=(lw == null) ?
                                "null": Long.toString(lw.get())%>&nbsp;
                        </td>
                        <td align="right" nowrap>
                            <%=ArchiveUtils.formatBytesForDisplay(stats.getBytesPerHost((String)key))%>&nbsp;
                        </td>
                        <% if (cjob.getStatus().equalsIgnoreCase(CrawlJob.STATUS_RUNNING) ||
                                 cjob.getStatus().equals(CrawlJob.STATUS_PAUSED) ||
                                 cjob.getStatus().equals(CrawlJob.STATUS_WAITING_FOR_PAUSE)){ %>
                            <td align="right">
                                <%=ArchiveUtils.formatMillisecondsToConventional(System.currentTimeMillis()-stats.getHostLastFinished((String)key).longValue())%>
                            </td>
                        <% } %>
                    </tr>
            <%
                    alt = !alt;
                }
            %>                
        </table>
<%
    } // End if(cjob==null)else clause
%>
<%@include file="/include/foot.jsp"%>
