package org.apache.jsp;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import org.apache.jasper.runtime.*;
import org.archive.crawler.admin.CrawlJobHandler;
import org.archive.crawler.admin.CrawlJob;
import org.archive.crawler.Heritrix;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.FrontierMarker;
import org.archive.crawler.framework.exceptions.InvalidFrontierMarkerException;
import java.util.ArrayList;
import org.apache.commons.lang.StringUtils;
import java.text.SimpleDateFormat;
import org.archive.crawler.Heritrix;
import org.archive.crawler.admin.CrawlJob;
import org.archive.util.ArchiveUtils;
import org.archive.util.TextUtils;
import org.archive.crawler.admin.StatisticsTracker;
import org.archive.crawler.Heritrix;
import java.util.Map;
import java.util.Iterator;

public class frontier_jsp extends HttpJspBase {


  private static java.util.Vector _jspx_includes;

  static {
    _jspx_includes = new java.util.Vector(4);
    _jspx_includes.add("/include/handler.jsp");
    _jspx_includes.add("/include/head.jsp");
    _jspx_includes.add("/include/stats.jsp");
    _jspx_includes.add("/include/foot.jsp");
  }

  public java.util.List getIncludes() {
    return _jspx_includes;
  }

  public void _jspService(HttpServletRequest request, HttpServletResponse response)
        throws java.io.IOException, ServletException {

    JspFactory _jspxFactory = null;
    javax.servlet.jsp.PageContext pageContext = null;
    HttpSession session = null;
    ServletContext application = null;
    ServletConfig config = null;
    JspWriter out = null;
    Object page = this;
    JspWriter _jspx_out = null;


    try {
      _jspxFactory = JspFactory.getDefaultFactory();
      response.setContentType("text/html; charset=UTF-8");
      pageContext = _jspxFactory.getPageContext(this, request, response,
      			"/error.jsp", true, 8192, true);
      application = pageContext.getServletContext();
      config = pageContext.getServletConfig();
      session = pageContext.getSession();
      out = pageContext.getOut();
      _jspx_out = out;

      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");

    /**
     * This include page ensures that the handler exists and is ready to be
     * accessed.
     */
    CrawlJobHandler handler =
        (CrawlJobHandler)application.getAttribute("handler");
    Heritrix heritrix = (Heritrix)application.getAttribute("heritrix");
    
    // If handler is empty then this is the first time this bit of code is
    // being run since the server came online. In that case get or create the
    // handler.
    if (handler == null) {
        if(Heritrix.isSingleInstance()) {
            heritrix = Heritrix.getSingleInstance();
            handler = heritrix.getJobHandler();
            application.setAttribute("heritrix", heritrix);
            application.setAttribute("handler", handler);
        } else {
            // TODO:
            // If we get here, then there are multiple heritrix instances
            // and we have to put up a screen allowing the user choose between.
            // Otherwise, there is no Heritrix instance.  Thats a problem.
            throw new RuntimeException("No heritrix instance (or multiple " +
                    "to choose from and we haven't implemented this yet)");
        }
    }
    
    // ensure controller's settingsHandler is always thread-installed 
    // in web ui threads
    if(handler != null) {
        CrawlJob job = handler.getCurrentJob();
        if(job != null) {
            CrawlController controller = job.getController();
            if (controller != null) {
                controller.installThreadContextSettingsHandler();
            }
        }
    }

      out.write("\n");
      out.write("\n\n");
      out.write("\n");
      out.write("\n\n");
      out.write("\n\n");
      out.write("\n\n");

    /**
     * This page allows users to inspect URIs in the Frontier of a paused
     * crawl. It also allows them to delete those URIs based on regular
     * expressions, or add URIs from an external file. 
     */
    
    String title = "View/Edit Frontier";
    int tab = 0;
    

      out.write("\n\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");

    String currentHeritrixName = (heritrix == null)?
        "No current Heritrix instance":
        (heritrix.getMBeanName() == null)?
            heritrix.getInstances().keySet().iterator().next().toString():
            heritrix.getMBeanName().toString();

    /**
     * An include file that handles the "look" and navigation of a web page. 
     * Include at top (where you would normally begin the HTML code).
     * If used, the include "foot.jsp" should be included at the end of the HTML
     * code. It will close any table, body and html tags left open in this one.
     * Any custom HTML code is thus placed between the two.
     *
     * The following variables must exist prior to this file being included:
     *
     * String title - Title of the web page
     * int tab - Which to display as 'selected'.
     *           0 - Console
     *           1 - Jobs
     *           2 - Profiles
     *           3 - Logs
     *           4 - Reports
     *           5 - Settings
     *           6 - Help
     *
     * SimpleHandler handler - In general this is provided by the include
     *                         page 'handler.jsp' which should be included
     *                         prior to this one.
     *
     * @author Kristinn Sigurdsson
     */
    String shortJobStatus = null;
	if(handler.getCurrentJob() != null) {
		shortJobStatus = TextUtils.getFirstWord(handler.getCurrentJob().getStatus());
	}
	String favicon = System.getProperties().getProperty("heritrix.favicon","h.ico");
	

      out.write("\n");

    StatisticsTracker stats = null;
    if(handler.getCurrentJob() != null) {
        // Assume that StatisticsTracker is being used.
        stats = (StatisticsTracker)handler.getCurrentJob().
            getStatisticsTracking();
    }

      out.write("\n");
      out.write("\n\n");
      out.write("<html>\n    ");
      out.write("<head>\n    \t");
      out.write("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>\n        ");
      out.write("<title>Heritrix: ");
      out.print(title);
      out.write("</title>\n        ");
      out.write("<link rel=\"stylesheet\" \n            href=\"");
      out.print(request.getContextPath());
      out.write("/css/heritrix.css\">\n        ");
      out.write("<link rel=\"icon\" href=\"");
      out.print(request.getContextPath());
      out.write("/images/");
      out.print(favicon);
      out.write("\" type=\"image/x-icon\" />\n        ");
      out.write("<link rel=\"shortcut icon\" href=\"");
      out.print(request.getContextPath());
      out.write("/images/");
      out.print(favicon);
      out.write("\" type=\"image/x-icon\" />\n        ");
      out.write("<script src=\"/js/util.js\">\n        ");
      out.write("</script>\n    ");
      out.write("</head>\n\n    ");
      out.write("<body>\n        ");
      out.write("<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" width=\"100%\">\n            ");
      out.write("<tr>\n                ");
      out.write("<td>\n                    ");
      out.write("<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" height=\"100%\">\n                        ");
      out.write("<tr>\n                            ");
      out.write("<td height=\"60\" width=\"155\" valign=\"top\" nowrap>\n                                ");
      out.write("<table border=\"0\" width=\"155\" cellspacing=\"0\" cellpadding=\"0\" height=\"60\">\n                                    ");
      out.write("<tr>\n                                        ");
      out.write("<td align=\"center\" height=\"40\" valign=\"bottom\">\n                                            ");
      out.write("<a border=\"0\" \n                                            href=\"");
      out.print(request.getContextPath());
      out.write("/index.jsp\">");
      out.write("<img border=\"0\" src=\"");
      out.print(request.getContextPath());
      out.write("/images/logo.gif\" height=\"37\" width=\"145\">");
      out.write("</a>\n                                        ");
      out.write("</td>\n                                    ");
      out.write("</tr>\n                                    ");
      out.write("<tr>\n                                        ");
      out.write("<td class=\"subheading\">\n                                            ");
      out.print(title);
      out.write("\n                                        ");
      out.write("</td>\n                                    ");
      out.write("</tr>\n                                ");
      out.write("</table>\n                            ");
      out.write("</td>\n                            ");
      out.write("<td width=\"5\" nowrap>\n                                &nbsp;&nbsp;\n                            ");
      out.write("</td>\n                            ");
      out.write("<td width=\"460\" align=\"left\" nowrap>\n                                ");
      out.write("<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" height=\"60\">\n                                    ");
      out.write("<tr>\n                                        ");
      out.write("<td colspan=\"2\" nowrap>\n                                            ");

                                                SimpleDateFormat sdf = new SimpleDateFormat("MMM. d, yyyy HH:mm:ss");
                                                sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
                                            
      out.write("\n                                            ");
      out.write("<b>\n                                                Status as of ");
      out.write("<a style=\"color: #000000\" href=\"");
      out.print(request.getRequestURL());
      out.write("\">");
      out.print(sdf.format(new java.util.Date()));
      out.write(" GMT");
      out.write("</a>\n                                            ");
      out.write("</b>\n                                            &nbsp;&nbsp;\n                                            ");
      out.write("<span style=\"text-align:right\">\n                                            ");
      out.write("<b>\n                                                Alerts: \n                                            ");
      out.write("</b>\n                                            ");
 if(heritrix.getAlertsCount() == 0) { 
      out.write("\n                                                ");
      out.write("<a style=\"color: #000000; text-decoration: none\" href=\"");
      out.print(request.getContextPath());
      out.write("/console/alerts.jsp\">no alerts");
      out.write("</a>\n                                            ");
 } else if(heritrix.getNewAlertsCount()>0){ 
      out.write("\n                                                ");
      out.write("<b>");
      out.write("<a href=\"");
      out.print(request.getContextPath());
      out.write("/console/alerts.jsp\">");
      out.print(heritrix.getAlerts().size());
      out.write(" (");
      out.print(heritrix.getNewAlertsCount());
      out.write(" new)");
      out.write("</a>");
      out.write("</b>\n                                            ");
 } else { 
      out.write("\n                                                ");
      out.write("<a style=\"color: #000000\" href=\"");
      out.print(request.getContextPath());
      out.write("/console/alerts.jsp\">");
      out.print(heritrix.getAlertsCount());
      out.write(" (");
      out.print(heritrix.getNewAlertsCount());
      out.write(" new)");
      out.write("</a>\n                                            ");
 } 
      out.write("\n                                            ");
      out.write("</span>\n                                        ");
      out.write("</td>\n                                    ");
      out.write("</tr>\n                                    ");
      out.write("<tr>\n                                        ");
      out.write("<td valign=\"top\" nowrap>\n\t\t\t\t\t\t\t\t\t\t");
      out.print( handler.isRunning()
										    ? "<span class='status'>Crawling Jobs</span>"
										    : "<span class='status'>Holding Jobs</span>"
										);
      out.write("<i>&nbsp;");
      out.write("</i>\n\t\t\t\t\t\t\t\t\t\t");
      out.write("</td>\n\t\t\t\t\t\t\t\t\t\t");
      out.write("<td valign=\"top\" align=\"right\" nowrap>\n\t\t\t\t\t\t\t\t\t\t");

										if(handler.isRunning() || handler.isCrawling()) {
										    if(handler.getCurrentJob() != null)
										    {
      out.write("\n\t\t\t\t\t\t\t\t\t\t");
      out.write("<span class='status'>\n\t\t\t\t\t\t\t\t\t\t");
      out.print( shortJobStatus );
      out.write("</span> job:\n\t\t\t\t\t\t\t\t\t\t");
      out.write("<i>");
      out.print( handler.getCurrentJob().getJobName() );
      out.write("</i>\n\t\t\t\t\t\t\t\t\t\t");

										    } else {
										        out.println("No job ready <a href=\"");
										        out.println(request.getContextPath());
										        out.println("/jobs.jsp\" style='color: #000000'>(create new)</a>");
										     }
										 }
										
      out.write("\n\t\t\t\t\t\t\t\t\t\t");
      out.write("</td>\n                                    ");
      out.write("</tr>\n                                    ");
      out.write("<tr>\n                                        ");
      out.write("<td nowrap>\n                                            ");
      out.print(handler.getPendingJobs().size());
      out.write("\n                                            jobs\n                                            ");
      out.write("<a style=\"color: #000000\" href=\"");
      out.print(request.getContextPath());
      out.write("/jobs.jsp#pending\">pending");
      out.write("</a>,\n                                            ");
      out.print(handler.getCompletedJobs().size());
      out.write("\n                                            ");
      out.write("<a style=\"color: #000000\" href=\"");
      out.print(request.getContextPath());
      out.write("/jobs.jsp#completed\">completed");
      out.write("</a>\n                                            &nbsp;\n                                        ");
      out.write("</td>\n                                        ");
      out.write("<td nowrap align=\"right\">\n                                            ");
 if(handler.isCrawling()){ 
      out.write("\n                                                    ");
      out.print((stats != null)? stats.successfullyFetchedCount(): 0);
      out.write(" URIs in \n\t\t                                            ");
      out.print( ArchiveUtils.formatMillisecondsToConventional( 
		                                            		((stats != null) 
		                                            		  	? (stats.getCrawlerTotalElapsedTime())
		                                            		  	: 0),
		                                            		false
		                                            	)
		                                            );
      out.write("\n\t\t                                            (");
      out.print(ArchiveUtils.doubleToString(((stats != null)? stats.currentProcessedDocsPerSec(): 0),2));
      out.write("/sec)\n                                            ");
 } 
      out.write("\n                                        ");
      out.write("</td>\n                                    ");
      out.write("</tr>\n                                ");
      out.write("</table>\n                            ");
      out.write("</td>\n                        ");
      out.write("</tr>\n                    ");
      out.write("</table>\n                ");
      out.write("</td>\n                ");
      out.write("<td width=\"100%\" nowrap>\n                    &nbsp;\n                ");
      out.write("</td>\n            ");
      out.write("</tr>\n            ");
      out.write("<tr>\n                ");
      out.write("<td bgcolor=\"#0000FF\" height=\"1\" colspan=\"4\">\n                ");
      out.write("</td>\n            ");
      out.write("</tr>\n            ");
      out.write("<tr>\n                ");
      out.write("<td colspan=\"4\" height=\"20\">\n                    ");
      out.write("<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" width=\"100%\" height=\"20\">\n                        ");
      out.write("<tr>\n                            ");
      out.write("<td class=\"tab_seperator\">&nbsp;");
      out.write("</td>\n                            ");
      out.write("<td class=\"tab");
      out.print(tab==0?"_selected":"");
      out.write("\">\n                                ");
      out.write("<a href=\"");
      out.print(request.getContextPath());
      out.write("/index.jsp\" class=\"tab_text");
      out.print(tab==0?"_selected":"");
      out.write("\">Console");
      out.write("</a>\n                            ");
      out.write("</td>\n                            ");
      out.write("<td class=\"tab_seperator\">&nbsp;");
      out.write("</td>\n                            ");
      out.write("<td class=\"tab");
      out.print(tab==1?"_selected":"");
      out.write("\">\n                                ");
      out.write("<a href=\"");
      out.print(request.getContextPath());
      out.write("/jobs.jsp\" class=\"tab_text");
      out.print(tab==1?"_selected":"");
      out.write("\">Jobs");
      out.write("</a>\n                            ");
      out.write("</td>\n                            ");
      out.write("<td class=\"tab_seperator\">&nbsp;");
      out.write("</td>\n                            ");
      out.write("<td class=\"tab");
      out.print(tab==2?"_selected":"");
      out.write("\">\n                                ");
      out.write("<a href=\"");
      out.print(request.getContextPath());
      out.write("/profiles.jsp\" class=\"tab_text");
      out.print(tab==2?"_selected":"");
      out.write("\">Profiles");
      out.write("</a>\n                            ");
      out.write("</td>\n                            ");
      out.write("<td class=\"tab_seperator\">&nbsp;");
      out.write("</td>\n                            ");
      out.write("<td class=\"tab");
      out.print(tab==3?"_selected":"");
      out.write("\">\n                                ");
      out.write("<a href=\"");
      out.print(request.getContextPath());
      out.write("/logs.jsp\" class=\"tab_text");
      out.print(tab==3?"_selected":"");
      out.write("\">Logs");
      out.write("</a>\n                            ");
      out.write("</td>\n                            ");
      out.write("<td class=\"tab_seperator\">&nbsp;");
      out.write("</td>\n                            ");
      out.write("<td class=\"tab");
      out.print(tab==4?"_selected":"");
      out.write("\">\n                                ");
      out.write("<a href=\"");
      out.print(request.getContextPath());
      out.write("/reports.jsp\" class=\"tab_text");
      out.print(tab==4?"_selected":"");
      out.write("\">Reports");
      out.write("</a>\n                            ");
      out.write("</td>\n                            ");
      out.write("<td class=\"tab_seperator\">&nbsp;");
      out.write("</td>\n                            ");
      out.write("<td class=\"tab");
      out.print(tab==5?"_selected":"");
      out.write("\">\n                                ");
      out.write("<a href=\"");
      out.print(request.getContextPath());
      out.write("/setup.jsp\" class=\"tab_text");
      out.print(tab==5?"_selected":"");
      out.write("\">Setup");
      out.write("</a>\n                            ");
      out.write("</td>\n                            ");
      out.write("<td class=\"tab_seperator\">&nbsp;");
      out.write("</td>\n                            ");
      out.write("<td class=\"tab");
      out.print(tab==6?"_selected":"");
      out.write("\">\n                                ");
      out.write("<a href=\"");
      out.print(request.getContextPath());
      out.write("/help.jsp\" class=\"tab_text");
      out.print(tab==6?"_selected":"");
      out.write("\">Help");
      out.write("</a>\n                             ");
      out.write("</td>\n                            ");
      out.write("<td width=\"100%\">\n                            ");
      out.write("</td>\n                        ");
      out.write("</tr>\n                    ");
      out.write("</table>\n                ");
      out.write("</td>\n            ");
      out.write("</tr>\n            ");
      out.write("<tr>\n                ");
      out.write("<td bgcolor=\"#0000FF\" height=\"1\" colspan=\"4\">");
      out.write("</td>\n            ");
      out.write("</tr>\n         ");
      out.write("</table>\n                    ");
      out.write("<!-- MAIN BODY -->\n");
      out.write("\n");

   if( handler.getCurrentJob() != null)  {
       if ( !handler.getCurrentJob().getStatus().equals(CrawlJob.STATUS_PAUSED) ) {

      out.write("\n    ");
      out.write("<b style=\"color:red\">MODIFYING THE FRONTIER OF A RUNNING CRAWL IS HIGHLY LIKELY TO\n    CORRUPT THE CRAWL!");
      out.write("</b>\n");
      out.write("<hr>\n");
      }
        String regexpr = StringUtils.defaultString(request.getParameter("match"));

        String queueRegex = StringUtils.defaultString(request.getParameter("queueRegex"));
        
        int numberOfMatches = 1000;
        try {
            if(request.getParameter("numberOfMatches") != null ){
                numberOfMatches = Integer.parseInt(request.getParameter("numberOfMatches"));
            }
        } catch ( Exception e ){
            numberOfMatches = 1000;
        }
        
        boolean verbose = request.getParameter("verbose") != null && request.getParameter("verbose").equals("true");

        boolean grep = request.getParameter("grep") != null && request.getParameter("grep").equals("true");
        
        String action = request.getParameter("action");    

      out.write("\n    ");
      out.write("<script type=\"text/javascript\">\n        function doDisplayInitial(){\n            document.frmFrontierList.action.value = \"initial\";\n            document.frmFrontierList.method = \"GET\";\n            document.frmFrontierList.submit();\n        }\n        \n        function doDisplayNext(){\n            document.frmFrontierList.action.value = \"next\";\n            document.frmFrontierList.method = \"GET\";\n            document.frmFrontierList.submit();\n        }\n        \n        function doCount(){\n            document.frmFrontierList.action.value = \"count\";\n            document.frmFrontierList.method = \"GET\";\n            document.frmFrontierList.submit();\n        }\n        \n        function doDelete(){\n            if(confirm(\"This action will delete ALL URIs in the Frontier that match the specified regular expression!\\nAre you sure you wish to proceed?\")){\n                document.frmFrontierList.action.value = \"delete\";\n                document.frmFrontierList.submit();\n            }\n        }\n        function checkForEnter(e){\n");
      out.write("            if(e.keyCode == 13){ //13 ascii == enter key\n                doDisplayInitial();\n            }\n        }\n    ");
      out.write("</script>\n    \n    ");
      out.write("<b>Add URIs");
      out.write("</b>\n");
        
    if("add".equals(action)) {
        String resultMessage = handler.importUris(
            request.getParameter("file"),
            request.getParameter("style"),
            request.getParameter("forceRevisit"));
        out.println("<br><font color='red'>"+resultMessage+"</font><br>");
        // don't do anything else 
        action = null;
    }

      out.write("\n    ");
      out.write("<form name=\"frmFrontierAdd\" method=\"POST\" action=\"frontier.jsp\">\n    ");
      out.write("<input type=\"hidden\" name=\"action\" value=\"add\">\n    ");
      out.write("<table cellspacing=\"0\" cellpadding=\"0\" width=\"100%\">\n        ");
      out.write("<tr>\n            ");
      out.write("<td nowrap valign=\"right\">\n                Import from file:\n            ");
      out.write("</td>\n            ");
      out.write("<td>\n                ");
      out.write("<input name=\"file\" size=\"33\" value=\"\">\n            ");
      out.write("</td>\n            ");
      out.write("<td nowrap>\n               &nbsp;");
      out.write("<input type=\"submit\" value=\"Import URIs\">\n            ");
      out.write("</td>\n            ");
      out.write("<td width=\"100%\">\n            ");
      out.write("</td>\n        ");
      out.write("</tr>\n        ");
      out.write("<tr>\n            ");
      out.write("<td>");
      out.write("</td>\n            ");
      out.write("<td colspan=\"2\" nowrap>\n                ");
      out.write("<input type=\"radio\" name=\"style\" checked value=\"perLine\">one URI per line \n                ");
      out.write("<input type=\"radio\" name=\"style\" value=\"crawlLog\">crawl.log style\n                ");
      out.write("<input type=\"radio\" name=\"style\" value=\"recoveryJournal\">recovery journal style (uncompressed)\n            ");
      out.write("</td>\n            ");
      out.write("<td width=\"100%\">\n            ");
      out.write("</td>\n        ");
      out.write("</tr>\n        ");
      out.write("<tr>\n            ");
      out.write("<td>");
      out.write("</td>\n            ");
      out.write("<td colspan=\"2\">\n                ");
      out.write("<input type=\"checkbox\" name=\"forceRevisit\" value=\"true\" name=\"verbose\">\n                Force revisit\n            ");
      out.write("</td>\n            ");
      out.write("<td width=\"100%\">\n            ");
      out.write("</td>\n        ");
      out.write("</tr>\n\n    ");
      out.write("</table>\n    ");
      out.write("</form>\n    \n    ");
      out.write("<hr>\n    ");
      out.write("<b>View or Delete URIs");
      out.write("</b>\n    ");
      out.write("<form name=\"frmFrontierList\" method=\"POST\" action=\"frontier.jsp\">\n    ");
      out.write("<input type=\"hidden\" name=\"action\" value=\"\">\n    ");
      out.write("<table cellspacing=\"0\" cellpadding=\"0\" width=\"100%\">\n        ");
      out.write("<tr>\n            ");
      out.write("<td nowrap>\n                URI match regex:\n            ");
      out.write("</td>\n            ");
      out.write("<td colspan=\"3\">\n                ");
      out.write("<input name=\"match\" size=\"33\" value=\"");
      out.print(regexpr);
      out.write("\" onKeyPress=\"checkForEnter(event)\">\n            ");
      out.write("</td>\n            ");
      out.write("<td nowrap>\n                &nbsp;");
      out.write("<a href=\"");
      out.print(request.getContextPath());
      out.write("/help/regexpr.jsp\">?");
      out.write("</a>&nbsp;&nbsp;\n            ");
      out.write("</td>\n            ");
      out.write("<td nowrap>\n                ");
      out.write("<input type=\"button\" value=\"Display URIs\" onClick=\"doDisplayInitial()\">&nbsp;&nbsp;&nbsp;\n                ");
      out.write("<input type=\"button\" value=\"Count URIs\" onClick=\"doCount()\">&nbsp;&nbsp;&nbsp;\n                ");
      out.write("<input type=\"button\" value=\"Delete URIs\" onClick=\"doDelete()\">\n            ");
      out.write("</td>\n            ");
      out.write("<td width=\"100%\">\n            ");
      out.write("</td>\n        ");
      out.write("</tr>\n        ");
      out.write("<tr>\n            ");
      out.write("<td nowrap>\n                queue match regex:\n            ");
      out.write("</td>\n            ");
      out.write("<td colspan=\"3\">\n                ");
      out.write("<input name=\"queueRegex\" size=\"33\" value=\"");
      out.print(queueRegex);
      out.write("\" onKeyPress=\"checkForEnter(event)\">\n            ");
      out.write("</td>\n            ");
      out.write("<td nowrap>\n                &nbsp;");
      out.write("<a href=\"");
      out.print(request.getContextPath());
      out.write("/help/regexpr.jsp\">?");
      out.write("</a>&nbsp;&nbsp;\n            ");
      out.write("</td>\n            ");
      out.write("<td>\n                (affects deletes only)\n            ");
      out.write("</td>\n            ");
      out.write("<td width=\"100%\">\n            ");
      out.write("</td>\n        ");
      out.write("</tr>\n        ");
      out.write("<tr>\n            ");
      out.write("<td nowrap>\n                Display matches:\n            ");
      out.write("</td>\n            ");
      out.write("<td colspan=\"4\">\n                ");
      out.write("<input name=\"numberOfMatches\" size=\"6\" value=\"");
      out.print(numberOfMatches);
      out.write("\" onKeyPress=\"checkForEnter(event)\">\n            ");
      out.write("</td>\n        ");
      out.write("</tr>\n        ");
      out.write("<tr>\n            ");
      out.write("<td nowrap>\n                Verbose description:\n            ");
      out.write("</td>\n            ");
      out.write("<td>\n                ");
      out.write("<input type=\"checkbox\" value=\"true\" name=\"verbose\" ");
      out.print(verbose?"checked":"");
      out.write(">\n            ");
      out.write("</td>\n            ");
      out.write("<td align=\"right\">\n                grep style URI regex:\n            ");
      out.write("</td>\n            ");
      out.write("<td align=\"right\" width=\"20\">\n                ");
      out.write("<input type=\"checkbox\" value=\"true\" name=\"grep\" ");
      out.print(grep?"checked":"");
      out.write(">\n            ");
      out.write("</td>\n            ");
      out.write("<td>");
      out.write("</td>\n        ");
      out.write("</tr>\n        ");
      out.write("<tr>");
      out.write("<td height=\"5\">");
      out.write("</td>");
      out.write("</tr>\n        ");
      out.write("<tr bgColor=\"black\">\n            ");
      out.write("<td bgcolor=\"#000000\" height=\"1\" colspan=\"7\">\n            ");
      out.write("</td>\n        ");
      out.write("</tr>\n");
        
                StringBuffer outputString = new StringBuffer();
                if ( action != null ) {
                    
                    FrontierMarker marker = null;
                    if(grep){
                        if(regexpr.length() > 0){
                            regexpr = ".*" + regexpr + ".*";
                        } else {
                            regexpr = ".*";
                        }
                    }
                    
                    if(action.equals("initial")){
                       // Get initial marker.
                       marker = handler.getInitialMarker(regexpr,false);
                       session.setAttribute("marker",marker);
                    } else if(action.equals("next")) {
                       // Reuse old marker.
                       marker = (FrontierMarker)session.getAttribute("marker");
                       regexpr = marker.getMatchExpression();
                    } else if(action.equals("count")) {
					   //
                       marker = handler.getInitialMarker(regexpr,false);
					   int count = 0;
					   do {
					       ArrayList list = handler.getPendingURIsList(marker,100,false);
					       count += list.size();
					   } while (marker.hasNext());
					   marker = null;
                       out.println("<tr><td height='5'></td></tr>");
                       out.println("<tr><td colspan='7'><b>" + count + " URIs matching</b> <code>" + regexpr + "</code></b></td></tr>");
                       out.println("<tr><td height='5'></td></tr>");
                     } else if(action.equals("delete")){
                       // Delete based on regexpr.
                       long numberOfDeletes = handler.deleteURIsFromPending(regexpr,queueRegex);
                       out.println("<tr><td height='5'></td></tr>");
                       out.println("<tr><td colspan='7'><b>All " + numberOfDeletes + " URIs matching</b> <code>" + regexpr + "</code> <b> were deleted");
                       if(StringUtils.isNotBlank(queueRegex)) {
                          out.println(" from queues matching '"+queueRegex+"'");
                       }
                       out.println("</b></td></tr>");
                       out.println("<tr><td height='5'></td></tr>");
                    }
                    
                    if (marker != null) {             

                        int found = 0;
                        try{
                            ArrayList list = handler.getPendingURIsList(marker,numberOfMatches,verbose);
                            found = list.size();
                            for(int i=0 ; i < list.size() ; i++){
                                outputString.append((String)list.get(i)+"\n");
                            }
                        } catch ( InvalidFrontierMarkerException e ) {
                            session.removeAttribute("marker");
                            outputString.append("Invalid marker");
                        }

                        long from = 1;
                        long to = marker.getNextItemNumber()-1;
                        boolean hasNext = marker.hasNext();
                        
                        if(marker.getNextItemNumber() > numberOfMatches+1){
                            // Not starting from 1.
                            from = to-found+1;
                        }

      out.write("\n                        ");
      out.write("<tr>");
      out.write("<td height=\"5\">");
      out.write("</td>");
      out.write("</tr>\n                        ");
      out.write("<tr>\n                            ");
      out.write("<td colspan=\"7\">\n                                ");
 if(to>0) 
                                	{ 
      out.write(" Displaying URIs ");
      out.print(from);
      out.write(" - ");
      out.print(to);
      out.write(" matching ");
 } 
                                   else 
                                    { 
      out.write(" No URIs found matching ");
 } 
      out.write(" expression '");
      out.write("<code>");
      out.print(regexpr);
      out.write("</code>'.  \n                                 ");
 if(hasNext){ 
      out.write(" ");
      out.write("<a href=\"javascript:doDisplayNext()\">Get next set of matches &gt;&gt;");
      out.write("</a> ");
 } 
      out.write("\n                            ");
      out.write("</td>\n                        ");
      out.write("</tr>\n                        ");
      out.write("<tr>");
      out.write("<td height=\"5\">");
      out.write("</td>");
      out.write("</tr>\n                        ");
      out.write("<tr bgColor=\"black\">\n                            ");
      out.write("<td bgcolor=\"#000000\" height=\"1\" colspan=\"7\">\n                            ");
      out.write("</td>\n                        ");
      out.write("</tr>\n                        ");
      out.write("<tr>");
      out.write("<td height=\"5\">");
      out.write("</td>");
      out.write("</tr>\n                        ");
      out.write("<tr>\n                            ");
      out.write("<td colspan=\"7\">");
      out.write("<pre>");
      out.print(outputString.toString());
      out.write("</pre>");
      out.write("</td>\n                        ");
      out.write("</tr>\n                        ");
      out.write("<tr>");
      out.write("<td height=\"5\">");
      out.write("</td>");
      out.write("</tr>\n                        ");
      out.write("<tr bgColor=\"black\">\n                            ");
      out.write("<td bgcolor=\"#000000\" height=\"1\" colspan=\"7\">\n                            ");
      out.write("</td>\n                        ");
      out.write("</tr>\n                        ");
      out.write("<tr>");
      out.write("<td height=\"5\">");
      out.write("</td>");
      out.write("</tr>\n                        ");
      out.write("<tr>\n                            ");
      out.write("<td colspan=\"7\">\n                                ");
 if(to>0) { 
      out.write(" Displaying URIs ");
      out.print(from);
      out.write(" - ");
      out.print(to);
      out.write(" matching ");
 } else { 
      out.write(" No URIs found matching ");
 } 
      out.write(" expression '");
      out.write("<code>");
      out.print(regexpr);
      out.write("</code>'.  ");
 if(hasNext){ 
      out.write(" ");
      out.write("<a href=\"javascript:doDisplayNext()\">Get next set of matches &gt;&gt;");
      out.write("</a> ");
 } 
      out.write("\n                            ");
      out.write("</td>\n                        ");
      out.write("</tr>\n                        ");
      out.write("<tr>");
      out.write("<td height=\"5\">");
      out.write("</td>");
      out.write("</tr>\n");

                        out.println("</pre>");
                    }
                }

      out.write("\n        ");
      out.write("<tr bgColor=\"black\">\n            ");
      out.write("<td bgcolor=\"#000000\" height=\"1\" colspan=\"7\">\n            ");
      out.write("</td>\n        ");
      out.write("</tr>\n        ");
      out.write("<tr>");
      out.write("<td height=\"5\">");
      out.write("</td>");
      out.write("</tr>\n    ");
      out.write("</table>\n    ");
      out.write("</form>\n");

    } else { 

      out.write("\n        ");
      out.write("<b>No current job.");
      out.write("</b>\n");
  
    } 

      out.write("\n\n");

    /**
     * An include file that handles the "look" and navigation of a web page. 
     * Wrapps up things begun in the "head.jsp" include file.  See it for
     * more details.
     *
     * @author Kristinn Sigurdsson
     */

      out.write("\n");
      out.write("<br/>\n");
      out.write("<br/>\n        ");
      out.write("<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" width=\"100%\">\n            ");
      out.write("<tr>\n            ");
      out.write("<td bgcolor=\"#0000FF\" height=\"1\" colspan=\"4\">");
      out.write("</td>\n            ");
      out.write("</tr>\n            ");
      out.write("<tr>\n            ");
      out.write("<td class=\"instance_name\">Identifier: ");
      out.print(currentHeritrixName);
      out.write("</td>\n            ");
      out.write("</tr>\n        ");
      out.write("</table>\n                    ");
      out.write("<!-- END MAIN BODY -->\n    ");
      out.write("</body>\n");
      out.write("</html>");
      out.write("\n");
    } catch (Throwable t) {
      out = _jspx_out;
      if (out != null && out.getBufferSize() != 0)
        out.clearBuffer();
      if (pageContext != null) pageContext.handlePageException(t);
    } finally {
      if (_jspxFactory != null) _jspxFactory.releasePageContext(pageContext);
    }
  }
}
