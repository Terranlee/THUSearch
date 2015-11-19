<%@include file="/include/handler.jsp"%>

<%@ page import="org.archive.crawler.admin.CrawlJob" %>
<%@ page import="java.util.List" %>
<%@ page import="javax.servlet.http.HttpServletRequest" %>

<%!
    public String printJobList(List jobs, boolean isJobs,
            HttpServletRequest request) {
        if(jobs==null) {
            return null;
        }
        StringBuffer ret = new StringBuffer();
        for(int i = jobs.size()-1 ; i >= 0 ; i--) {
            CrawlJob tmp = (CrawlJob)jobs.get(i);
            ret.append("<li><a href=\"");
            ret.append(request.getContextPath());
            ret.append("/jobs/new.jsp?job=" + tmp.getUID() + "\">" +
                tmp.getJobName());
            if(isJobs){
                ret.append(" ["+tmp.getUID()+"]");
            }
            ret.append("</a>");
        }
        return ret.toString();
    }
%>
<%
    boolean isJobs = request.getParameter("type")!=null&&request.getParameter("type").equals("jobs");
    String title = "New via "+(isJobs?"an existing job":"a profile");
    int tab = 1;
%>

<%@include file="/include/head.jsp"%>
<p>
    <b>Select <%=isJobs?"job":"profile"%> to base new job on:</b>
<p>
    <ul>
<%
    if(isJobs){
        out.println(printJobList(handler.getPendingJobs(), true, request));
        if(handler.getCurrentJob()!=null){
            out.println("<li><a href=\"");
            out.println(request.getContextPath());
            out.println("/jobs/new.jsp?job=" +
                handler.getCurrentJob().getUID() + "\">" +
                handler.getCurrentJob().getJobName() +
                " [" + handler.getCurrentJob().getUID() + "]</a>");
        }
        out.println(printJobList(handler.getCompletedJobs(),true, request));
    } else {
        out.println(printJobList(handler.getProfiles(),false, request));
    }
%>    
    </ul>

        
<%@include file="/include/foot.jsp"%>
