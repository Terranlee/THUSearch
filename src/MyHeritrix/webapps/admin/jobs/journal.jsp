<%@include file="/include/handler.jsp"%>

<%@page import="org.archive.crawler.datamodel.CrawlOrder,org.archive.crawler.settings.SettingsHandler,org.archive.crawler.settings.XMLSettingsHandler,org.archive.crawler.admin.CrawlJob,org.archive.crawler.util.LogReader" %>

<%@ page import="java.io.File"%>
<%@ page import="java.io.OutputStreamWriter"%>
<%@ page import="java.io.FileOutputStream"%>
<%@ page import="java.util.Date"%>
<%@ page import="org.archive.crawler.admin.ui.CookieUtils"%>
<%@ page import="org.archive.util.ArchiveUtils"%>

<%
    String journal = null;
    
    String operator = CookieUtils.getCookieValue(request.getCookies(),
        "operator","");
    
    String journalFilename = "operator.journal";

    /* Location of journals (right alongside any logs */
    SettingsHandler settingsHandler = null;
    CrawlJob theJob = null;
    if(request.getParameter("job") != null && request.getParameter("job").length() > 0){
        //Get logs for specific job. This assumes that the logs/journal for each job are stored in a unique location.
        theJob = handler.getJob(request.getParameter("job"));
    }else{
        if(handler.getCurrentJob() != null){
            // If no specific job then assume current one
            theJob = handler.getCurrentJob();
        } else if(handler.getCompletedJobs().size() > 0){
            // If no current job, use the latest completed job.
            theJob = (CrawlJob)handler.getCompletedJobs().get(handler.getCompletedJobs().size()-1);
        }
    }
    
    if(theJob != null){
        settingsHandler = theJob.getSettingsHandler();
        String diskPath = (String)settingsHandler.getOrder().getAttribute(CrawlOrder.ATTR_DISK_PATH);
        diskPath = settingsHandler.getPathRelativeToWorkingDirectory(diskPath).getAbsolutePath()+"/";
        
        String action = request.getParameter("action");
        if(action != null){
            // Need to handle an action.
            if(action.equals("add")){
                operator = request.getParameter("operator");
                // Add new journal entry.
                int number = theJob.getNumberOfJournalEntries()+1;
                File file = new File(diskPath);
                file.mkdirs();
                OutputStreamWriter fw = 
                    new OutputStreamWriter(new FileOutputStream(diskPath + journalFilename,true),"UTF-8");
                fw.write("#" + number + "\n");
                fw.write("Operator: " + operator+"\n");
                SimpleDateFormat journalsdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
                journalsdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
                fw.write("Time: " + journalsdf.format(new Date()) + " GMT\n");
                fw.write("Entry: ");
                fw.write(request.getParameter("entry"));
                fw.write("\n"); // Make some room behind each entry.
                fw.flush();
                fw.close();
                theJob.setNumberOfJournalEntries(number);
                
                if(request.getParameter("remember") != null && request.getParameter("remember").equals("true")){
                    // Save operator name to cookie.
                    Cookie operatorCookie = new Cookie("operator", operator);
                    operatorCookie.setMaxAge(60*60*24*365);//One year
                    response.addCookie(operatorCookie);
                }
            }
        }

        journal = LogReader.get(diskPath + journalFilename);
        
        if(journal == null){
            journal = "No entries";
        }
    } else {
        journal = "No job found";
    }
    
    
    String title = "Job journal";
    int tab = 1;
%>

<%@include file="/include/head.jsp"%>
<% if(theJob != null){ %>
    <script type="text/javascript">
        function doAdd(){
            document.frmJournal.action.value = "add";
            document.frmJournal.submit();
        }
    </script>
    <form name="frmJournal" method="post" action="journal.jsp">
    <input type="hidden" name="action">
    <input type="hidden" name="job" value="<%=theJob.getUID()%>">
    <table>
        <tr>
            <td colspan="4">
                <b>New journal entry for job <%=theJob.getJobName()%></b>
            </td>
            <td width="100%">&nbsp;</td>
        <tr>
            <td>Operator:</td>
            <td width="10"><input name="operator" value="<%=operator%>"></td>
            <td width="10"><input name="remember" type="checkbox" value="true"></td>
            <td>Remember me</td>
        </tr>
        <tr>
            <td valign="top">Entry:</td>
            <td colspan="3"><textarea name="entry" cols="60" rows="5"></textarea></td>
        </tr>
        <tr>
            <td></td>
            <td colspan="3" align="right"><input type="Button" value="Submit entry" onClick="doAdd()"></td>
        </tr>
    </table>
    </form>
    <table>
        <tr>
            <td><b>Prior entries</b></td>
        </tr>
        <tr>
            <td colspan="3">
                <pre><%=journal%></pre>
            </td>
        </tr>
    </table>
<% } else { out.println(journal); } %>
<%@include file="/include/foot.jsp"%>
