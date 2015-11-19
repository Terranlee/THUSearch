<%
  /**
   * This pages allows the user to select all submodules which appear
   * in collections inside other modules 
   *
   * @author Kristinn Sigurdsson
   */
%>
<%@include file="/include/handler.jsp"%>
<%@include file="/include/modules.jsp"%>

<%@ page import="org.archive.crawler.admin.ui.JobConfigureUtils" %>
<%@ page import="org.archive.crawler.admin.CrawlJob" %>
<%@ page import="org.archive.crawler.url.canonicalize.BaseRule" %>

<%
    CrawlJob theJob = JobConfigureUtils.handleJobAction(handler, request,
            response, request.getContextPath() + "/jobs.jsp", null, null);
    int tab = theJob.isProfile()?2:1;
%>

<%
    // Set page header.
    String title = "Submodules";
    int jobtab = 7;
%>

<%@include file="/include/head.jsp"%>
<%@include file="/include/filters_js.jsp"%>
    <p>
        <%@include file="/include/jobnav.jsp"%>
    <p>
        <p>
            <b>Add/Remove/Order Submodules</b>
        <p>
        <p>Use this page to add/remove/order submodules. Go to the
        <a href="javascript:doGoto('<%=request.getContextPath()%>/jobs/configure.jsp?job=<%=theJob.getUID()%>')">Settings</a>
        page to complete configuration of added submodules (e.g. To
        add the particular regex to an added canonicalization RegexRule
        or to fill in the authentication information into an added
        RFC2617 credential).</p>

    <form name="frmFilters" method="post" 
            action="submodules.jsp">
        <input type="hidden" name="job" value="<%=theJob.getUID()%>">
        <input type="hidden" name="action" value="done">
        <input type="hidden" name="subaction" value="">
        <input type="hidden" name="map" value="">
        <input type="hidden" name="filter" value="">
        <%=printAllMaps(theJob.getSettingsHandler().getOrder(), null, false, true, null)%>
    </form>
    <p>
<%@include file="/include/jobnav.jsp"%>
<%@include file="/include/foot.jsp"%>


