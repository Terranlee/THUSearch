<%@include file="/include/handler.jsp"%>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder,org.archive.crawler.admin.CrawlJob,java.util.List" %>

<%
    String sAction = request.getParameter("action");
    if(sAction != null){
        // Need to handle an action    
        if(sAction.equalsIgnoreCase("delete")){
            handler.deleteJob(request.getParameter("job"));
        }
    }    

    String title = "Crawl jobs";
    int tab = 1;
%>

<%@include file="/include/head.jsp"%>

<% 
    if(request.getParameter("message") != null &&
        request.getParameter("message").length() > 0) {
%>
    <p>
        <span class="flashMessage"><b><%=request.getParameter("message")%></b></span>
<% } %>

<% if(handler.isCrawling()){ %>
    <h2>Active Job - <i><%=handler.getCurrentJob().getJobName()%></i></h2>
    <ul>
        <li><a href="<%=request.getContextPath()%>/jobs/configure.jsp?job=<%=handler.getCurrentJob().getUID()%>">
        Edit configuration</a>


        <li><a href="<%=request.getContextPath()%>/jobs/clearperhost.jsp">
        Clear cached per-host settings</a>
        
        <li><a href="<%=request.getContextPath()%>/jobs/journal.jsp?job=<%=handler.getCurrentJob().getUID()%>">
        Journal</a>
        
        <li>View:<br> 
        <ul>
        <li><a href="<%=request.getContextPath()%>/reports/crawljob.jsp">
            Crawl report</a></li>
        <li><a href="<%=request.getContextPath()%>/reports/seeds.jsp">
            Seeds report</a></li>
        <li><a target="_blank" href="<%=request.getContextPath()%>/jobs/vieworder.jsp?job=<%=handler.getCurrentJob().getUID()%>">
            Crawl order (raw xml)</a></li>
        </ul>
    </ul>
<% } %>

<h2>Create New Job</h2>
    <ul>
	<li><a href="<%=request.getContextPath()%>/jobs/basedon.jsp?type=jobs">
	Based on existing job</a></li>
	<li><a href="<%=request.getContextPath()%>/jobs/recovery.jsp">
	Based on a recovery</a></li>
	<li><a href="<%=request.getContextPath()%>/jobs/basedon.jsp">
	Based on a profile</a></li>
	<li><a href="<%=request.getContextPath()%>/jobs/new.jsp">
	With defaults</a></li>
    </ul>
	
<h2><a name="pending" />Pending
Jobs (<%=handler.getPendingJobs().size()%>)</h2>

<%  
    List jobs = handler.getPendingJobs();
    boolean alt = true;
    // If no pending jobs, don't show table headings.
    if (jobs.size() > 0) {
%>
        <table border="0" cellspacing="0" cellpadding="1">
            <tr>
                <th>
                    Name
                </th>
                <th>
                    Status
                </th>
                <th>
                    Options
                </th>
            </tr>
            <%
                alt = true;
                for(int i=0 ; i    < jobs.size() ; i++)
                {
                    CrawlJob job = (CrawlJob)jobs.get(i);
            %>        
                    <tr bgcolor='<%=alt?"#DDDDFF":"#EEEEFF"%>'>
                        <td>
                            <%=job.getJobName()%>&nbsp;&nbsp;
                        </td>
                        <td>
                            <i><%=job.getStatus()%></i>&nbsp;&nbsp;
                        </td>
                        <td>
                            <a target="_blank" href="<%=request.getContextPath()%>/jobs/vieworder.jsp?job=<%=job.getUID()%>">View order</a>
                            &nbsp;
                            <a href="<%=request.getContextPath()%>/jobs/configure.jsp?job=<%=job.getUID()%>">Edit configuration</a>
                            &nbsp;
                            <a href="<%=request.getContextPath()%>/jobs/journal.jsp?job=<%=job.getUID()%>">Journal</a>
                            &nbsp;
                            <a href="?action=delete&job=<%=job.getUID()%>">Delete</a>
                            &nbsp;
                        </td>
                    </tr>
            <%
                    alt = !alt;
                }
            %>
        </table>


<%
    // End of if block that tests that there are pending jobs to show.
    }
%>


<h2><a name="completed" />Completed
Jobs(<%=handler.getCompletedJobs().size()%>)</h2>

<%  
    jobs = handler.getCompletedJobs();
    // If no completed jobs, don't show table headings.
    if (jobs.size() > 0) {
%>
        <table border="0" cellspacing="0" cellpadding="1"> 
            <tr>
                <th>
                    UID
                </th>
                <th>
                    Name
                </th>
                <th>
                    Status
                </th>
                <th>
                    Options
                </th>
            </tr>
            <%
                alt = true;
                for(int i=jobs.size()-1 ; i >= 0  ; i--)
                {
                    CrawlJob job = (CrawlJob)jobs.get(i);
            %>        
                    <tr bgcolor='<%=alt?"#DDDDFF":"#EEEEFF"%>'>
                        <td>
                            <code><%=job.getUID()%></code>&nbsp;&nbsp;
                        </td>
                        <td>
                            <%=job.getJobName()%>&nbsp;&nbsp;
                        </td>
                        <td>
                            <i><%=job.getStatus()%></i>&nbsp;&nbsp;&nbsp;
                        </td>
                        <td>
                            <a style="color: #003399;" target="_blank" href="<%=request.getContextPath()%>/jobs/vieworder.jsp?job=<%=job.getUID()%>">Crawl order</a>
                            &nbsp;
                            <a style="color: #003399;" href="<%=request.getContextPath()%>/reports/crawljob.jsp?job=<%=job.getUID()%>&nav=3">Crawl report</a>
                            &nbsp;
                            <a style="color: #003399;" href="<%=request.getContextPath()%>/reports/seeds.jsp?job=<%=job.getUID()%>&nav=3">Seeds report</a>
                            &nbsp;
                            <a style="color: #003399;" href="<%=request.getContextPath()%>/jobs/viewseeds.jsp?job=<%=job.getUID()%>">Seed file</a>
                            &nbsp;
                            <a style="color: #003399;" href="<%=request.getContextPath()%>/logs.jsp?job=<%=job.getUID()%>&nav=3">Logs</a>
                            &nbsp;
                            <a style="color: #003399;" href="<%=request.getContextPath()%>/jobs/journal.jsp?job=<%=job.getUID()%>">Journal</a>
                            &nbsp;
                            <a style="color: #003399;" href="?action=delete&job=<%=job.getUID()%>&nav=3">Delete</a>
                            &nbsp;
                        </td>
                    </tr>
                    <% if(job.getErrorMessage()!=null){ %>
                    <tr bgcolor='<%=alt?"#DDDDFF":"#EEEEFF"%>'>
                        <td></td>
                        <td colspan="3">
                            <pre><<span class="error"><%=job.getErrorMessage()%></span></pre>
                        </td>
                    </tr>
                    <% } %>
            <%
                    alt = !alt;
                }
            %>
        </table>
<%
    // End of if block that tests if there are completed jobs to show.
    }
%>


<%@include file="/include/foot.jsp"%>
