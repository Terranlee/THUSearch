<%
  /**
   * This pages displays availible options for per host overrides of 
   * a given crawl job.
   *
   * @author Kristinn Sigurdsson
   */
%>
<%@include file="/include/handler.jsp"%>

<%@page import="java.util.Collection"%>
<%@page import="java.util.Iterator"%>

<%@page import="org.archive.crawler.admin.CrawlJob"%>
<%@page import="org.archive.crawler.settings.CrawlerSettings"%>
<%@page import="org.archive.crawler.settings.XMLSettingsHandler"%>

<%
    String message = request.getParameter("message");
    // Load the job.
    CrawlJob theJob = handler.getJob(request.getParameter("job"));
    
    if(theJob == null) {
        // Didn't find any job with the given UID or no UID given.
        response.sendRedirect(request.getContextPath() +
            "/jobs.jsp?message=No job selected " +
            request.getParameter("job"));
        return;
    } else if(theJob.isReadOnly()){
        // Can't edit this job.
        response.sendRedirect(request.getContextPath() +
            "/jobs.jsp?message=Can't configure a running job");
        return;
    }

    // Load display level
    String currDomain = request.getParameter("currDomain");
    if( currDomain != null ) {
        currDomain = currDomain.trim();
    }

    // Get the settings objects.
    XMLSettingsHandler settingsHandler = theJob.getSettingsHandler();
    CrawlerSettings localSettings = settingsHandler.getSettingsObject(currDomain);
    
    // Check for actions
    String action = request.getParameter("action");
    if(action != null){
        // Need to do something!
        if(action.equals("done")){
            // Ok, done editing.
            if(theJob.isNew()){            
                handler.addJob(theJob);
                response.sendRedirect(request.getContextPath() +
                    "/jobs.jsp?message=Job created");
            }else{
                if(theJob.isRunning()){
                    handler.kickUpdate();
                }
                if(theJob.isProfile()){
                    response.sendRedirect(request.getContextPath() +
                        "/profiles.jsp?message=Profile modified");
                }else{
                    response.sendRedirect(request.getContextPath() +
                        "/jobs.jsp?message=Job modified");
                }
            }
            return;
        } else if(action.equals("edit")){
            // Edit settings.
            // First make sure the file exists (create it if it doesn't)
            if(currDomain != null && currDomain.length()>0){
                settingsHandler.writeSettingsObject(settingsHandler.getOrCreateSettingsObject(currDomain));
                // Then redirect to configure override.
                response.sendRedirect(request.getContextPath() +
                    "/jobs/per/configure.jsp?job=" + theJob.getUID() +
                    "&currDomain="+currDomain);
                return;
            }
        } else if(action.equals("delete")){
            // Delete settings.
            if(currDomain != null && currDomain.length()>0){
                settingsHandler.deleteSettingsObject(settingsHandler.getOrCreateSettingsObject(currDomain));
                message = "Override for domain '"+currDomain+"' deleted.";
                currDomain = "";
            }
        } else if(action.equals("goto")){
            // Goto another page of the job/profile settings
            response.sendRedirect(request.getParameter("where"));
            return;
        }
    }
    
    // Adjust display data.
    String parentDomain = null;
    if(currDomain==null){
        currDomain = "";
    } else {
        if(currDomain.indexOf('.')==-1){
            parentDomain = "";
        } else {
            parentDomain = currDomain.substring(currDomain.indexOf('.')+1);
        }
    }    
    String title = "Per domain settings";
    int tab = theJob.isProfile()?2:1;
    int jobtab = 3;
%>
<%@include file="/include/head.jsp"%>
    <script type="text/javascript">
        function doSubmit(){
            document.frmPer.submit();
        }
        
        function doGoto(where){
            document.frmPer.action.value="goto";
            document.frmPer.where.value = where;
            doSubmit();
        }
        
        function doCreateEdit(){
            document.frmPer.currDomain.value = document.frmPer.newDomain.value;
            doEdit();
        }
        
        function doDeleteThis(){
            if(!confirm("Are you sure you want to delete override for '"+
                document.frmPer.currDomain.value +"'?")) {
                return;
            }
            document.frmPer.action.value="delete";
            doSubmit();
        }
        
        function doDelete(){
            if(!confirm("Are you sure you want to delete override for '"+
                document.frmPer.newDomain.value +"'?")) {
                return;
            }
            document.frmPer.currDomain.value = document.frmPer.newDomain.value;
            document.frmPer.action.value="delete";
            doSubmit();
        }
        
        function doCreate(){
            doEdit();
        }
        
        function doEdit(){
            document.frmPer.action.value="edit";
            doSubmit();
        }
        
        function doGotoDomain(domain){
            document.frmPer.currDomain.value = domain;
            document.frmPer.action.value="noop";
            doSubmit();
        }
    </script>    
    <% if(message != null && message.length() > 0){ %>
        <p>
            <span class="flashMessage"><b><%=message%></b></span>
    <% } %>
    <p>
        <%@include file="/include/jobnav.jsp"%>
    <p>
    <form name="frmPer" method="post" action="overview.jsp">
        <input type="hidden" name="action" value="done">
        <input type="hidden" name="where" value="">
        <input type="hidden" name="job" value="<%=theJob.getUID()%>">
        <input type="hidden" name="currDomain" value="<%=currDomain%>">
        <b>
            Known overrides for 
            <%=theJob.isProfile()?"profile":"job"%>
            <%=theJob.getJobName()%>:
        </b>
        <p>
        <b><%=currDomain%></b>
        <ul>
        <%     
            if(currDomain.length()>0) { 
        %>
                <li> <a href="javascript:doGotoDomain('<%=parentDomain%>')">- Up -</a>
        <%
            }
            Collection subs = settingsHandler.getDomainOverrides(currDomain);
            for (Iterator i = subs.iterator(); i.hasNext();) {
                String printDomain = (String)i.next();
                if(currDomain.length()>0){
                    printDomain += "."+currDomain;
                }
        %>
                <li> <a href="javascript:doGotoDomain('<%=printDomain%>')"><%=printDomain%></a>
        <%
            }
        %>
        </ul>
        <% 
            if(currDomain.length()>0){ 
                if(localSettings != null){
        %>
                    <p><a href="javascript:doEdit()">Edit override for '<%=currDomain%>'</a><br>
                        <p><a href="javascript:doDeleteThis()">Delete override for '<%=currDomain%>'</a><br>
        <%
                } else {
        %>
                    <p><a href="javascript:doCreate()">Create override for '<%=currDomain%>'</a>
        <% 
                }
            } 
        %>
        <p>
            <b>Quick override:</b><br>
            Domain: <input name="newDomain" value="<%=currDomain%>">
            <input type="submit" value="Create/Edit" onClick="doCreateEdit()">
                <input type="button" value="Delete" onClick="doDelete()">
    </form>
<%@include file="/include/foot.jsp"%>
