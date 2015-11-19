<%@include file="/include/handler.jsp"%>

<%@ page import="org.archive.crawler.datamodel.CrawlOrder" %>
<%@ page import="org.archive.crawler.admin.ui.JobConfigureUtils" %>
<%@ page import="org.archive.crawler.settings.ComplexType" %>
<%@ page import="org.archive.crawler.settings.CrawlerSettings" %>
<%@ page import="org.archive.crawler.settings.XMLSettingsHandler" %>

<%@ page import="java.io.BufferedReader" %>
<%@ page import="java.io.FileReader" %>
<%@ page import="java.util.regex.Pattern" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.io.File" %>

<%
    /**
     * Create a new job
     */
     if(request.getCharacterEncoding() == null) {
     	request.setCharacterEncoding("UTF-8");
     }
    CrawlJob theJob = handler.getJob(request.getParameter("job"));
    boolean isProfile = "true".equals(request.getParameter("profile"));
    // Recover can have a value of 'true' if we are to do a recover-log
    // based recovery else it has the name of the checkpoint we're to recover
    // from.
    String recovery = request.getParameter("recover");
    
    if (theJob == null) {
        // Ok, use default profile then.
        theJob = handler.getDefaultProfile();
        if(theJob == null){
            // ERROR - This should never happen. There must always be at least
            // one (default) profile.
            out.println("ERROR: NO PROFILE FOUND");
            return;
        }
    } 

    XMLSettingsHandler settingsHandler = theJob.getSettingsHandler();
    CrawlOrder crawlOrder = settingsHandler.getOrder();
    CrawlerSettings orderfile = settingsHandler.getSettingsObject(null);
    
    String error = null;
    String metaName = request.getParameter("meta/name");
    String jobDescription = request.getParameter("meta/description");
    
    if(request.getParameter("action") != null) {
        // Make new job.
        CrawlJob newJob = null;

        // Ensure we got a valid name. ([a-zA-Z][0-9][-_])
        Pattern p = Pattern.compile("[a-zA-Z_\\-0-9\\.,]*");
        if (p.matcher(metaName).matches()==false) {
            // Illegal name!
            error = "Name can only contain letters, digits, and dash, "
                   +"underscore, period, or comma ( - _ . , ).<br> "
                   +"No spaces are allowed";
        }
        
        if(error == null) {
            if(isProfile) {
                // Ensure unique name
                CrawlJob test = handler.getJob(metaName);
                if(test == null) {
                    // unique name
                    newJob = handler.newProfile(theJob, metaName,
                        jobDescription,
                        request.getParameter("seeds"));
                } else {
                    // Need a unique name!
                    error = "Profile name must be unique!";
                }
            } else {
                newJob = handler.newJob(
                    theJob, 
                    recovery,
                    metaName, 
                    jobDescription,
                    request.getParameter("seeds"),
                    CrawlJob.PRIORITY_AVERAGE);
            }
        }
        
        if(error == null && newJob != null) {
            // Ensure order file with new name/desc is written
            // [ 1066573 ] sometimes job based-on other job uses older job name
            handler.ensureNewJobWritten(newJob, metaName, jobDescription);
            if(request.getParameter("action").equals("configure")){
                response.sendRedirect(request.getContextPath() +
                    "/jobs/configure.jsp?job="+newJob.getUID());
            } else if(request.getParameter("action").equals("modules")){
                response.sendRedirect(request.getContextPath() +
                   "/jobs/modules.jsp?job="+newJob.getUID());
            } else if(request.getParameter("action").equals("submodules")){
                response.sendRedirect(request.getContextPath() +
                   "/jobs/submodules.jsp?job="+newJob.getUID());
            } else if(request.getParameter("action").equals("override")){
                response.sendRedirect(request.getContextPath() +
                   "/jobs/per/overview.jsp?job="+newJob.getUID());
            } else {
                handler.addJob(newJob);
                response.sendRedirect(request.getContextPath() +
                   "/jobs.jsp?message=Job created");
            }
            return;
        }
    }
    
    String title = isProfile?"New profile":"New crawl job";
    int tab = isProfile?2:1;
    // TODO: Offer setting of priority.
%>

<%@include file="/include/head.jsp"%>

        <form name="frmNew" method="post" action="new.jsp">
            <input type="hidden" name="action" value="new">
            <input type="hidden" name="profile" value="<%=isProfile%>">
            <input type="hidden" name="job" value="<%=theJob.getUID()%>">
            <%     if(recovery != null && recovery.length() > 0) { %>
            <input type="hidden" name="recover" value="<%=recovery%>">
            <%    }  %>
            <b>
                Create new 
            <%     if(isProfile){ %>
                profile
            <%    } else { %>
                crawl job 
            <%    }    %>
                based on
            <%     if(recovery != null && recovery.length() > 0) { %>
                recovery of 
            <%    }  %>
            <%     if(request.getParameter("job")==null){%>
                default profile
            <% 
                }else{ 
                    if(theJob.isProfile()){
                        out.println("profile ");                    
                    } else {
                        out.println("job ");
                    }
                    out.println("'"+theJob.getJobName()+"'"); 
                }
            %>    
            </b>
            <p>            
            <table>
                <tr>
                    <td>
                        Name of new <%= isProfile ? "profile" : "job" %>:
                    </td>
                    <td>
                        <input maxlength="38" name="meta/name" value="<%=error==null?orderfile.getName():metaName%>" style="width: 440px">
                    </td>
                </tr>
                <% if(error != null){ %>
                    <tr>
                        <td>
                        </td>
                        <td>
                            <span class="error"><%=error%></span>
                        </td>
                    </tr>
                <% } %>
                <tr>
                    <td>
                        Description:
                    </td>
                    <td>
                        <input name="meta/description" value="<%=error==null?orderfile.getDescription():request.getParameter("meta/description")%>" style="width: 440px">
                    </td>
                </tr>
                <tr>
                    <td valign="top">
                        Seeds:
                    </td>
                    <td><font size="-1">Fill in seed URIs below, one per line.
                    Comment lines begin with '#'.</font></br>
                        <textarea name="seeds" style="width: 440px" rows="8"><%
                            if(error == null) {
                                JobConfigureUtils.
                                    printOutSeeds(settingsHandler, out);
                            } else {
                                out.println(request.getParameter("seeds"));
                            }
                        %></textarea>
                    </td>
                </tr>
                <tr>
                <td colspan="2" align="right">
<input type="button" value="Modules"
   onClick="document.frmNew.action.value='modules';document.frmNew.submit()">
<input type="button" value="Submodules"
   onClick="document.frmNew.action.value='submodules';document.frmNew.submit()">
<input type="button" value="Settings"
    onClick="document.frmNew.action.value='configure';document.frmNew.submit()">
<input type="button" value="Overrides"
    onClick="document.frmNew.action.value='override';document.frmNew.submit()">
<% if(isProfile == false){ %>
    <input type="submit" value="Submit job">
<% } %>
                </td>
                </tr>
            </table>
        </form>
        
<%@include file="/include/foot.jsp"%>
