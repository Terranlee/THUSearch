<%
  /**
   * This pages displays existing refinements on a particular override (or global settings).
   * Allows user to delete them and creat new ones.
   *
   * @author Kristinn Sigurdsson
   */
%>
<%@include file="/include/handler.jsp"%>

<%@page import="java.util.ListIterator"%>
<%@page import="java.util.regex.Pattern" %>

<%@page import="org.archive.crawler.settings.CrawlerSettings"%>
<%@page import="org.archive.crawler.admin.ui.JobConfigureUtils"%>
<%@page import="org.archive.crawler.settings.XMLSettingsHandler"%>
<%@page import="org.archive.crawler.settings.refinements.Refinement"%>

<%
    // Load display level
    String currDomain = request.getParameter("currDomain");
    // Load the job to manipulate   
    CrawlJob theJob = JobConfigureUtils.checkCrawlJob(
        handler.getJob(request.getParameter("job")), response,
        request.getContextPath() + "/jobs.jsp", currDomain);
    if (theJob == null) {
        return;
    }

    String message = request.getParameter("message");

    // Get the settings objects.
    XMLSettingsHandler settingsHandler = theJob.getSettingsHandler();
    boolean global = currDomain == null || currDomain.length() == 0;


    CrawlerSettings localSettings;
    
    if(global){
        localSettings = settingsHandler.getSettingsObject(null);
    } else {
        localSettings = settingsHandler.getSettingsObject(currDomain);
    }
    
    // Check for actions
    String action = request.getParameter("action");
    if(action != null){
        // Need to do something!
        if(action.equals("done")){
            // Ok, done editing.
            if(global){
                if(theJob.isNew()){         
                    handler.addJob(theJob);
                    response.sendRedirect(request.getContextPath () +
                        "/jobs.jsp?message=Job created");
                }else{
                    if(theJob.isRunning()){
                        handler.kickUpdate();
                    }
                    if(theJob.isProfile()){
                        response.sendRedirect(request.getContextPath () +
                            "/profiles.jsp?message=Profile modified");
                    }else{
                        response.sendRedirect(request.getContextPath () +
                           "/jobs.jsp?message=Job modified");
                    }
                }
            } else {
                if(theJob.isRunning()){
                    handler.kickUpdate();
                }
                response.sendRedirect(request.getContextPath () +
                    "/jobs/per/overview.jsp?job=" + theJob.getUID() +
                    "&currDomain=" + currDomain +
                    "&message=Override changes saved");
            }
            return;
        } else if(action.equals("new")){
            // Add new refinement
            String reference = request.getParameter("newReference");
            String description = request.getParameter("newDescription");
            // Ensure we got a valid name. ([a-zA-Z][0-9][-_])
            Pattern p = Pattern.compile("[a-zA-Z_\\-0-9]*");
            if(p.matcher(reference).matches()==false){
                // Illegal name!
                message = "Name can only contain alphanumeric chars, dash and underscore.<br>No spaces are allowed";
            } else {
                // Got a valid name create the sucker
                Refinement newRef = new Refinement(localSettings, reference, description);
                settingsHandler.writeSettingsObject(localSettings);
            }
        } else if(action.equals("edit")){
            // Edit an existing refinement.
            response.sendRedirect(request.getContextPath () +
                "/jobs/refinements/criteria.jsp?job=" + theJob.getUID() +
                "&currDomain=" + currDomain + "&reference=" +
                request.getParameter("refinement"));
            return;
        } else if(action.equals("delete")){
            // Delete refinement.
            String reference = request.getParameter("refinement");
            localSettings.removeRefinement(reference);
            settingsHandler.writeSettingsObject(localSettings);
        } else if(action.equals("goto")){
            // Goto another page of the job/profile settings
            if(global){
                response.sendRedirect(request.getParameter("where"));
            } else {
                response.sendRedirect(request.getParameter("where")+"&currDomain="+currDomain);
            }
            return;
        }
    }
    
    String title = "Refinements";
    int tab = theJob.isProfile()?2:1;
    int jobtab = 5;
%>
<%@include file="/include/head.jsp"%>
    <script type="text/javascript">
        function doSubmit(){
            document.frmRefinements.submit();
        }
        
        function doGoto(where){
            document.frmRefinements.action.value="goto";
            document.frmRefinements.where.value = where;
            doSubmit();
        }
        
        function doEdit(refinement){
            document.frmRefinements.action.value="edit";
            document.frmRefinements.refinement.value = refinement;
            doSubmit();
        }
        
        function doDelete(refinement){
            document.frmRefinements.action.value="delete";
            document.frmRefinements.refinement.value = refinement;
            doSubmit();
        }
        
        function newRefinement(){
            document.frmRefinements.action.value="new";
            doSubmit();
        }
    </script>   
    <% if(message != null && message.length() > 0){ %>
        <p>
            <span class="flashMessage"><b><%=message%></b></span>
    <% } %>
    <p>
        <% if (global) { %>
            <%@include file="/include/jobnav.jsp"%>
        <% } else { %>
            <b>Override for the <%=theJob.isProfile()?"profile":"job"%> <%=theJob.getJobName()%> on domain '<%=currDomain%>'</b>
            <%@include file="/include/jobpernav.jsp"%>
        <% } %>
    <p>
    <form name="frmRefinements" method="post" action="overview.jsp">
        <input type="hidden" name="action" value="done">
        <input type="hidden" name="where" value="">
        <input type="hidden" name="job" value="<%=theJob.getUID()%>">
        <input type="hidden" name="currDomain" value="<%=currDomain==null?"":currDomain%>">
        <input type="hidden" name="refinement" value="">
        <b>
            Known Refinements for '<%=global?"global settings":currDomain%>' of
            <%=theJob.isProfile()?"profile":"job"%>
            <%=theJob.getJobName()%>:
        </b>
        <table border="0" cellpadding="0" cellspacing="0" width="450">
            <%
                ListIterator refinements = localSettings.refinementsIterator();
                boolean alt = true;
                while(refinements.hasNext()){
                    Refinement ref = (Refinement)refinements.next();
            %>
                    <tr <%=alt?"bgcolor='#EEEEFF'":""%>>
                        <td width="350">
                            <b><%=ref.getReference()%></b>
                        </td>
                        <td nowrap>
                            &nbsp;<a href="javascript:doEdit('<%=ref.getReference()%>')" style="color: #003399;" class="underLineOnHover">Edit</a>
                        </td>
                        <td nowrap>
                            &nbsp;<a href="javascript:doDelete('<%=ref.getReference()%>')" style="color: #003399;" class="underLineOnHover">Remove</a>
                        </td>
                    </tr>
                    <tr <%=alt?"bgcolor='#EEEEFF'":""%>>
                        <td colspan="3">
                            <table border="0" cellpadding="0" cellspacing="0" width="100%">
                                <tr>
                                    <td>&nbsp;&nbsp;&nbsp;</td>
                                    <td width="100%">
                                        <i><%=ref.getDescription()%></i>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
            <%
                    alt = !alt;
                }
            %>
        </table>
        <p>
        <table border="0" cellpadding="0" cellspacing="0">
            <tr>
                <td colspan="2">
                    <b>Create new refinement</b>
                </td>
            </tr>
            <tr>
                <td>
                    Name:
                </td>
                <td>
                    <input name="newReference" style="width: 230px">
                </td>
            </tr>
            <tr>
                <td valign="top">
                    Description:&nbsp;
                </td>
                <td>
                    <textarea name="newDescription" style="width: 230px" rows="4"></textarea>
                </td>
            </tr>
            <tr>
                <td colspan="2" align="right">
                    <input type="button" value="Create" onClick="newRefinement()">
                </td>
        </table>
    </form>
<%@include file="/include/foot.jsp"%>
