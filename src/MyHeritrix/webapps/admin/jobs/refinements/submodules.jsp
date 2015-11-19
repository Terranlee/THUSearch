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
<%@ page import="org.archive.crawler.settings.XMLSettingsHandler" %>
<%@ page import="org.archive.crawler.settings.refinements.*"%>

<%
    String currDomain = request.getParameter("currDomain");
    if(currDomain !=null && currDomain.length() == 0) {
        currDomain = null;
    }
    String reference = request.getParameter("reference");
    if(reference != null && reference.length() == 0) {
        reference = null;
    }
    CrawlJob theJob = JobConfigureUtils.handleJobAction(handler, request,
        response, request.getContextPath() + "/jobs/refinements/overview.jsp", 
        currDomain, reference);
    XMLSettingsHandler settingsHandler = 
        (XMLSettingsHandler)theJob.getSettingsHandler();
    CrawlerSettings localSettings = 
        settingsHandler.getSettingsObject(currDomain);
    Refinement refinement = localSettings.getRefinement(reference);
    CrawlerSettings refinementSettings = refinement.getSettings();
    int tab = theJob.isProfile()?2:1;
%>

<%
    // Set page header.
    String title = "Submodules for Refinement";
    int jobtab = 7;
%>

<%@include file="/include/head.jsp"%>
<%@include file="/include/filters_js.jsp"%>
    <p>
        <b>Refinement '<%=refinement.getReference()%>' on '<%=(currDomain==null)?"global settings":currDomain%>' of
        <%=theJob.isProfile()?"profile":"job"%>
        <%=theJob.getJobName()%>:</b>
        <%@include file="/include/jobrefinementnav.jsp"%>
    <p>
        <p>
            <b>Add/Remove/Order Submodules</b>
        <p>

    <form name="frmFilters" method="post" 
            action="submodules.jsp">
        <input type="hidden" name="job" value="<%=theJob.getUID()%>">
        <input type="hidden" name="currDomain" value="<%=currDomain%>">
        <input type="hidden" name="reference" value="<%=reference%>">
        <input type="hidden" name="action" value="done">
        <input type="hidden" name="subaction" value="continue">
        <input type="hidden" name="map" value="">
        <input type="hidden" name="filter" value="">
        <%=printAllMaps(theJob.getSettingsHandler().getOrder(), 
            refinementSettings, false, true, currDomain)%>
    </form>
    <p>
<%@include file="/include/jobrefinementnav.jsp"%>
<%@include file="/include/foot.jsp"%>


