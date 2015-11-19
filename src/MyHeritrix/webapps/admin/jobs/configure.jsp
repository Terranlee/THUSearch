<%
  /**
   * This pages allows the user to edit the configuration 
   * of a crawl order. 
   * That is set any af the 'values', but does not allow
   * users to change which 'modules' are used.
   *
   * @author Kristinn Sigurdsson
   */
%>
<%@include file="/include/handler.jsp"%>
<%@include file="/include/jobconfigure.jsp"%>

<%@ page import="org.archive.crawler.admin.CrawlJob" %>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder" %>
<%@ page import="org.archive.crawler.admin.ui.JobConfigureUtils" %>
<%@ page import="java.io.FileReader" %>
<%@ page import="java.io.BufferedReader" %>
<%@ page import="java.io.BufferedWriter" %>
<%@ page import="java.io.IOException" %>

<% 
    CrawlerSettings orderfile = settingsHandler.getSettingsObject(null);

    if(request.getCharacterEncoding() == null) {
    	request.setCharacterEncoding("UTF-8");
    }

    // Should we update with changes.
    if(request.getParameter("update") != null &&
            request.getParameter("update").equals("true")) {
        // Update values with new ones in the request
        errorHandler.clearErrors();
        JobConfigureUtils.writeNewOrderFile(crawlOrder, null, request, expert);
        orderfile.setDescription(request.getParameter("meta/description"));
        orderfile.setOperator(request.getParameter("meta/operator"));
        orderfile.setOrganization(request.getParameter("meta/organization"));
        orderfile.setAudience(request.getParameter("meta/audience"));
        settingsHandler.writeSettingsObject(orderfile);
        BufferedWriter writer;
        try {
        	if(request.getParameter("seeds") != null) {
	            JobConfigureUtils.printOutSeeds(settingsHandler,
	                    request.getParameter("seeds"));
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    // Check for actions.
    String action = request.getParameter("action");
    if(action != null) {
        if(action.equals("done")) {
            if(theJob.isNew()){            
                handler.addJob(theJob);
                response.sendRedirect(request.getContextPath() +
                    "/jobs.jsp?message=Job created");
            }else{
                if(theJob.isRunning()) {
                    handler.kickUpdate();
                }
                if(theJob.isProfile()) {
                    response.sendRedirect(request.getContextPath() +
                        "/profiles.jsp?message=Profile modified");
                }else {
                    response.sendRedirect(request.getContextPath() +
                        "/jobs.jsp?message=Job modified");
                }
            }
            return;
        } else if(action.equals("goto")) {
            // Goto another page of the job/profile settings
            response.sendRedirect(request.getParameter("item"));
            return;
        } else if (action.equals("addMap")) {
            // Adding to a simple map
            String mapName = request.getParameter("update");
            MapType map = (MapType)settingsHandler.
                getComplexTypeByAbsoluteName(orderfile, mapName);
            String key = request.getParameter(mapName + ".key");
            String value = request.getParameter(mapName + ".value");
            SimpleType t = new SimpleType(key, "", value);
            map.addElement(orderfile, t);
            response.sendRedirect("configure.jsp?job="+theJob.getUID());
            return;
        } else if (action.equals("deleteMap")) {
            // Removing from a simple map
            String mapName = request.getParameter("update");
            String key = request.getParameter("item");
            MapType map = (MapType)settingsHandler.
                getComplexTypeByAbsoluteName(orderfile, mapName);
            map.removeElement(orderfile,key);
            response.sendRedirect("configure.jsp?job=" + theJob.getUID());
            return;
        }
    }    

    // Get the HTML code to display the settigns.
    StringBuffer listsBuffer = new StringBuffer();
    String inputForm = printMBean(crawlOrder, null, "", listsBuffer, expert,
        errorHandler);
    // The listsBuffer will have a trailing comma if not empty. Strip it off.
    String lists = listsBuffer.toString().substring(0, 
        (listsBuffer.toString().length() > 0? 
            listsBuffer.toString().length() - 1: 0));

    // Settings for the page header
    String title = "Configure settings";
    int tab = theJob.isProfile()?2:1;
    int jobtab = 2;
%>

<%@include file="/include/head.jsp"%>

    <script type="text/javascript">
        function doSubmit(){
            // Before the form can be submitted we must
            // ensure that ALL elements in ALL lists
            // are selected. Otherwise they will be lost.
            lists = new Array(<%=lists%>);
            for(i=0 ; i<lists.length ; i++){
                theList = document.getElementById(lists[i]);
                for(j=0 ; j < theList.length ; j++){
                    theList.options[j].selected = true;
                }
            }
            document.frmConfig.submit();
        }
        
        function doGoto(where){
            document.frmConfig.action.value="goto";
            document.frmConfig.item.value = where;
            doSubmit();
        }
        
        function doPop(text){
            alert(text);
        }
        
        function setUpdate(){
            document.frmConfig.update.value = "true";
        }

        function setEdited(name){
            setUpdate();
        }
        
        expert = <%=expert%>;
        function setExpert(exp) {
            var initVal = exp ? "expertHide" : "expertShow";
            var newVal = exp ? "expertShow" : "expertHide";
            var trElements = document.getElementsByTagName("tr");
            for(i = 0; i < trElements.length; i++) {
                if(trElements[i].className == initVal) {
                    trElements[i].className = newVal;
                }   
            }
            eraseCookie('expert','/jobs/'); // erase legacy cookie if any
            createCookie('expert',exp,365);
            document.getElementById('hideExpertLink').className=exp?'show':'hide';
            document.getElementById('showExpertLink').className=exp?'hide':'show';
        }
    </script>

    <p>
        <%@include file="/include/jobnav.jsp"%>
    <p>

            <a id='hideExpertLink' 
                class='<%=expert?"show":"hide"%>' 
                href="javascript:setExpert(false)">Hide expert settings</a>

            <a id='showExpertLink' 
               class='<%=expert?"hide":"show"%>'  
               href="javascript:setExpert(true)">View expert settings</a>

    <p>
    
    <form name="frmConfig" method="post" action="configure.jsp">
        <input type="hidden" name="update" value="false">        
        <input type="hidden" name="action" value="done">
        <input type="hidden" name="item" value="">
        <input type="hidden" name="expert" value="<%=expert%>">
        <input type="hidden" name="job" value="<%=theJob.getUID()%>">
    
        <p>            
        <table>
            <tr>
                <td colspan="3">
                    <b>Meta data</b>
                </td>
            </tr>
            <tr>
                <td>
                    Description:
                </td>
                <td></td>
                <td>
                    <input name="meta/description" 
                        value="<%=orderfile.getDescription()%>"
                        style="width: 440px">
                </td>
            </tr>
            <tr>
                <td>
                    Crawl Operator:
                </td>
                <td></td>
                <td>
                    <input name="meta/operator"
                        value="<%=orderfile.getOperator()%>" 
                        style="width: 440px">
                </td>
            </tr>
            <tr>
                <td>
                    Crawl Organization:
                </td>
                <td></td>
                <td>
                    <input name="meta/organization" 
                        value="<%=orderfile.getOrganization()%>" 
                        style="width: 440px">
                </td>
            </tr>
            <tr>
                <td>
                    Crawl Job Recipient:
                </td>
                <td></td>
                <td>
                    <input name="meta/audience" 
                        value="<%=orderfile.getAudience()%>" 
                        style="width: 440px">
                </td>
            </tr>
            <%=inputForm%>
            <tr>
                <td colspan="3">
                    <b>Seeds</b>
                </td>
            </tr>
            <tr>
                <td valign="top">
                    Seeds:
                </td>
                <td></td>
                <td>
                    <%
                        if(JobConfigureUtils.seedsEdittableSize(settingsHandler)) {
                     %>
                    <textarea name="seeds" style="width: 440px" 
                        rows="8" onChange="setUpdate()"><%
                        JobConfigureUtils.printOutSeeds(settingsHandler, out);
                    %></textarea>
                    <%
                        } else {
                    %>
                    <a href="viewseeds.jsp?job=<%=theJob.getUID()%>">Seed list</a>
                    too large to edit.
                    <%
                        }
                    %>
                </td>
            </tr>
        </table>
    </form>
    <p>
        <%@include file="/include/jobnav.jsp"%>
        
<%@include file="/include/foot.jsp"%>
