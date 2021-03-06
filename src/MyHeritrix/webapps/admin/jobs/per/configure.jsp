<%
  /**
   * This pages allows the user to edit the configuration 
   * of an override. 
   * That is set any af the 'values', but does not allow
   * users to change which 'modules' are used.
   *
   * @author Kristinn Sigurdsson
   */
%>
<%@include file="/include/handler.jsp"%>
<%@include file="/include/jobconfigure.jsp"%>

<%@ page import="org.archive.crawler.datamodel.CrawlOrder" %>
<%@ page import="org.archive.crawler.admin.ui.JobConfigureUtils" %>

<%
    // Load display level
    String currDomain = request.getParameter("currDomain");
    CrawlerSettings orderfile = settingsHandler.getSettingsObject(currDomain);

    // Check for update.
    if(request.getParameter("update") != null && request.getParameter("update").equals("true")){
        // Update values with new ones in the request
        errorHandler.clearErrors();
        JobConfigureUtils.writeNewOrderFile(crawlOrder, orderfile, request,
            expert);
        settingsHandler.writeSettingsObject(orderfile);
    }
    
    // Check for actions
    String action = request.getParameter("action");
    if(action != null){
        if (action.equals("done")) {
            if(theJob.isRunning()){
                handler.kickUpdate();
            }
            response.sendRedirect(request.getContextPath() +
                "/jobs/per/overview.jsp?job=" + theJob.getUID() +
                "&currDomain=" + currDomain +
                "&message=Override changes saved");
            return;
        } else if (action.equals("goto")) {
            // Goto another page of the job/profile settings
            response.sendRedirect(request.getParameter("item")+"&currDomain="+currDomain);
            return;
        } else if (action.equals("addMap")) {
            // Adding to a simple map
            String mapName = request.getParameter("update");
            MapType map = (MapType)settingsHandler.getComplexTypeByAbsoluteName(orderfile,mapName);
            String key = request.getParameter(mapName+".key");
            String value = request.getParameter(mapName+".value");
            SimpleType t = new SimpleType(key,"",value);
            map.addElement(orderfile,t);
            response.sendRedirect("configure.jsp?job="+theJob.getUID()+"&currDomain="+currDomain);
            return;
        } else if (action.equals("deleteMap")) {
            // Removing from a simple map
            String mapName = request.getParameter("update");
            String key = request.getParameter("item");
            MapType map = (MapType)settingsHandler.getComplexTypeByAbsoluteName(orderfile,mapName);
            map.removeElement(orderfile,key);
            response.sendRedirect("configure.jsp?job="+theJob.getUID()+"&currDomain="+currDomain);
            return;
        } else if (action.equals("updateexpert")) {
            if(request.getParameter("expert") != null){
                if(request.getParameter("expert").equals("true")){
                    expert = true;
                } else {
                    expert = false;
                }
                // Save to cookie.
                Cookie operatorCookie = new Cookie("expert", Boolean.toString(expert));
                operatorCookie.setMaxAge(60*60*24*365);//One year
                response.addCookie(operatorCookie);
            }
        }
    }    

    // Get the HTML code to display the settigns.
    StringBuffer listsBuffer = new StringBuffer();
    String inputForm=printMBean(crawlOrder,orderfile,"",listsBuffer,expert,errorHandler);
    // The listsBuffer will have a trailing comma if not empty. Strip it off.
    String lists = listsBuffer.toString().substring(0,(listsBuffer.toString().length()>0?listsBuffer.toString().length()-1:0));

    // Settings for the page header
    String title = "Configure override";
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
            document.frmConfig.item.value=where;
            doSubmit();
        }
        
        function doPop(text){
            alert(text);
        }

        function setExpert(val){
            document.frmConfig.expert.value = val;
            document.frmConfig.action.value="updateexpert";
            doSubmit();
        }
        
        function setUpdate(){
            document.frmConfig.update.value = "true";
        }
        
        function setEdited(name){
            checkbox = document.getElementById(name+".override");
            checkbox.checked = true;
            setUpdate();
        }
    </script>

    <p>
        <b>Override for the <%=theJob.isProfile()?"profile":"job"%> <%=theJob.getJobName()%> on domain '<%=currDomain%>'</b>
        <%@include file="/include/jobpernav.jsp"%>
    <p>
    
    <form name="frmConfig" method="post" action="configure.jsp">
        <input type="hidden" name="update" value="true">        
        <input type="hidden" name="action" value="done">
        <input type="hidden" name="item" value="">
        <input type="hidden" name="expert" value="<%=expert%>">
        <input type="hidden" name="currDomain" value="<%=currDomain%>">
        <input type="hidden" name="job" value="<%=theJob.getUID()%>">
    
        <p>    
            <b>Instructions:</b> To override a setting, check the box in front of it and input new settings.<br>
            Unchecked settings are inherited settings from super domain. Changes to settings that<br>
            do not have a checked box will be discarded. Settings that can not be overridden will not<br>
            have a checkbox and will be displayed in a read only manner.
        <p>        
            <% if(expert){ %>
                <a href="javascript:setExpert('false')">Hide expert settings</a>
            <% } else { %>
                <a href="javascript:setExpert('true')">View expert settings</a>
            <% } %>
        <p>
        <table>
            <%=inputForm%>
        </table>
    </form>
    <p>
        <%@include file="/include/jobpernav.jsp"%>
        
<%@include file="/include/foot.jsp"%>
