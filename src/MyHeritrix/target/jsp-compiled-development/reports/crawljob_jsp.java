package org.apache.jsp;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import org.apache.jasper.runtime.*;
import org.archive.crawler.admin.CrawlJobHandler;
import org.archive.crawler.admin.CrawlJob;
import org.archive.crawler.Heritrix;
import org.archive.crawler.framework.CrawlController;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.util.ArchiveUtils;
import org.archive.crawler.admin.*;
import java.text.SimpleDateFormat;
import org.archive.crawler.Heritrix;
import org.archive.crawler.admin.CrawlJob;
import org.archive.util.ArchiveUtils;
import org.archive.util.TextUtils;
import org.archive.crawler.admin.StatisticsTracker;
import org.archive.crawler.Heritrix;
import java.util.Map;
import java.util.Iterator;

public class crawljob_jsp extends HttpJspBase {


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
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");

     /**
     * Page allows user to view the information on seeds in the
     * StatisticsTracker for a completed job.
     * Parameter: job - UID for the job.
     */

    // Pixel width of widest bar
    final int MAX_BAR_WIDTH = 450;

    // Indicates if we are viewing a completed job report or the
    // current job report
    boolean viewingCurrentJob = false;

    String job = request.getParameter("job");
    //CrawlJob cjob = (job != null)? handler.getJob(job): handler.getCurrentJob();
    CrawlJob cjob = null;
    if (job == null) {
    	// Get job that is currently running or paused
    	cjob = handler.getCurrentJob();
    	viewingCurrentJob = true;
    }
    else {
    	// Get job indicated in query string
    	cjob = handler.getJob(job);
    }     
    
    String title = "Crawl job report"; 
    int tab = 4;

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
      out.write("\n\n");
      out.write("<style>\n\ttr.headerrow {background-color: #ccccff }\n\ttr.totalrow {background-color: #ccccff }\n\t.percent {font-size: 70%}\n");
      out.write("</style>\n\n");

    // Do this uptop here. Needed before I go into the if/else that follows.
    StatisticsSummary summary = null;
    if (cjob != null) {
        summary = new StatisticsSummary(cjob);
    }

    if(cjob == null)
    {
    	viewingCurrentJob = false;
    	
        // NO JOB SELECTED - ERROR

      out.write("\n        ");
      out.write("<p>&nbsp;");
      out.write("<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
      out.write("<b>Invalid job selected.");
      out.write("</b>\n");

		} else if(!viewingCurrentJob && summary != null && summary.isStats()) {
		// If the job selected is not the current job, then show stats
        // for completed job
		java.text.DecimalFormat percentFormat =
            new java.text.DecimalFormat("#####.#");

      out.write("\n        ");
      out.write("<table border=\"0\">\n            ");
      out.write("<tr>\n                ");
      out.write("<td valign=\"top\">\n                    ");
      out.write("<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" >\n                        ");
      out.write("<tr>\n                            ");
      out.write("<td>\n                                ");
      out.write("<b>Job name:");
      out.write("</b>&nbsp;\n                            ");
      out.write("</td>\n                            ");
      out.write("<td>\n                                ");
      out.print(cjob.getJobName());
      out.write("\n                            ");
      out.write("</td>\n                        ");
      out.write("</tr>\n                        ");
      out.write("<tr>\n                            ");
      out.write("<td>\n                                ");
      out.write("<b>Status:");
      out.write("</b>&nbsp;\n                            ");
      out.write("</td>\n                            ");
      out.write("<td>\n                                ");
      out.print(cjob.getStatus());
      out.write("\n                            ");
      out.write("</td>\n                        ");
      out.write("</tr>\n                        ");
      out.write("<tr>\n                            ");
      out.write("<td>\n                                ");
      out.write("<b>Time:");
      out.write("</b>&nbsp;\n                            ");
      out.write("</td>\n                            ");
      out.write("<td>\n                               ");
      out.print(summary.getDurationTime());
      out.write("\n                            ");
      out.write("</td>\n                        ");
      out.write("</tr>\n                    ");
      out.write("</table>\n                ");
      out.write("</td>\n                ");
      out.write("<td>\n                    &nbsp;&nbsp;&nbsp;\n                ");
      out.write("</td>\n                ");
      out.write("<td valign=\"top\">\n                    ");
      out.write("<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" >\n                        ");
      out.write("<tr>\n                            ");
      out.write("<td>\n                                ");
      out.write("<b>Processed docs/sec:");
      out.write("</b>&nbsp;\n                            ");
      out.write("</td>\n                            ");
      out.write("<td>                           \n                               ");
      out.print(summary.getProcessedDocsPerSec());
      out.write("                              \n                            ");
      out.write("</td>\n                            ");
      out.write("</tr>\n                        ");
      out.write("<tr>\n                        ");
      out.write("<td>\n                                ");
      out.write("<b>Processed KB/sec:");
      out.write("</b>&nbsp;\n                            ");
      out.write("</td>\n                            ");
      out.write("<td>\n                               ");
      out.print(summary.getBandwidthKbytesPerSec());
      out.write("\n                            ");
      out.write("</td>\n                        ");
      out.write("</tr>\n                        ");
      out.write("<tr>\n                            ");
      out.write("<td>\n                                ");
      out.write("<b>Total data written:");
      out.write("</b>&nbsp;\n                            ");
      out.write("</td>\n                            ");
      out.write("<td>\n                                ");
      out.print(summary.getTotalDataWritten());
      out.write("\n                            ");
      out.write("</td>\n                        ");
      out.write("</tr>\n                    ");
      out.write("</table>\n                ");
      out.write("</td>\n            ");
      out.write("</tr>\n        ");
      out.write("</table>                       \n    \n    ");
      out.write("<p>\n    ");
      out.write("</p>\n          \n          \n    ");
      out.write("<table width=750>\n\t    ");
      out.write("<tr>\n\t        ");
      out.write("<td valign=\"center\" >");
      out.write("<img \n\t        src=\"");
      out.print(request.getContextPath());
      out.write("/images/blue.jpg\" height=\"1\" width=\"40\">");
      out.write("</td>\n\t        ");
      out.write("<td align=\"center\">");
      out.write("<i>HTTP");
      out.write("</i>");
      out.write("</td>\n\t        ");
      out.write("<td valign=\"center\" >");
      out.write("<img \n\t        src=\"");
      out.print(request.getContextPath());
      out.write("/images/blue.jpg\" height=\"1\" width=\"660\">");
      out.write("</td>\n\t    ");
      out.write("</tr>\n\t");
      out.write("</table>\n\t\n\t");
      out.write("<table cellspacing=\"0\" width=750>\n\t        \n        \t");
      out.write("<tr class=\"headerrow\">\n                ");
      out.write("<th>\n                    Status code\n                ");
      out.write("</th>\n                ");
      out.write("<th width=\"200\" colspan=\"2\">\n                    Documents\n                ");
      out.write("</th>\n            ");
      out.write("</tr>\n            ");

                            boolean alt = true;
                            TreeMap scd = summary.getReverseSortedCopy(summary.getStatusCodeDistribution());
                            for (Iterator i = scd.keySet().iterator(); i.hasNext();) {
                                Object key = i.next();
                                long count = ((AtomicLong)scd.get(key)).get();
                                long displaybarwidth = 0;
                                long barwidthadjust = 5;
                                double per = ((double)count) / summary.getTotalStatusCodeDocuments();
                                if(summary.getTotalStatusCodeDocuments() > 0){
                                   displaybarwidth = (long)(per * MAX_BAR_WIDTH);
                                } 
                                if(displaybarwidth==0){
                                   displaybarwidth=1;
                                }
                                
                                String percent = percentFormat.format(100 * per);
            
      out.write("\n                    ");
      out.write("<tr ");
      out.print(alt?"bgcolor=#EEEEFF":"");
      out.write(">\n                        ");
      out.write("<td nowrap>\n                            ");
      out.write("<a style=\"text-decoration: none;\" href=\"");
      out.print(request.getContextPath());
      out.write("/logs.jsp?job=");
      out.print(cjob.getUID());
      out.write("&log=crawl.log&mode=regexpr&regexpr=^.{24}\\s*");
      out.print((String)key);
      out.write("&grep=true\">\n                                ");
      out.print(CrawlURI.fetchStatusCodesToString(Integer.parseInt((String)key)));
      out.write("\n                            ");
      out.write("</a>&nbsp;\n                        ");
      out.write("</td>\n                        ");
      out.write("<td colspan=\"2\" nowrap>\n                            ");
      out.write("<img src=\"");
      out.print(request.getContextPath());
      out.write("/images/blue.jpg\" height=\"10\" width=\"");
      out.print(displaybarwidth);
      out.write("\"> ");
      out.print(count);
      out.write(" &nbsp;\n                            \t");
      out.write("<span class=percent>(");
      out.print(percent);
      out.write("%)");
      out.write("</span>\n                        ");
      out.write("</td>\n                    ");
      out.write("</tr>\n            ");

                            alt = !alt;
                            }
            
      out.write("                \n            ");
      out.write("<tr class=\"totalrow\">\n            \t");
      out.write("<td>");
      out.write("<b>Total:");
      out.write("</b>");
      out.write("</td>\n            \t");
      out.write("<td>");
      out.print(summary.getTotalStatusCodeDocuments());
      out.write(" &nbsp; ");
      out.write("</td>\n            \t");
      out.write("<td>&nbsp;");
      out.write("</td>\n            ");
      out.write("</tr>\n            ");
      out.write("<tr>\n                ");
      out.write("<td>&nbsp;");
      out.write("</td>\n            ");
      out.write("</tr>\n            ");
      out.write("<tr class=\"headerrow\">\n                ");
      out.write("<th width=\"100\">\n                    MIME type\n                ");
      out.write("</th>\n                ");
      out.write("<th width=\"200\">\n                    Documents\n                ");
      out.write("</th>\n                ");
      out.write("<th>\n                    Data\n                ");
      out.write("</th>\n            ");
      out.write("</tr>\n            ");

                            alt = true;
                            TreeMap fd = summary.getReverseSortedCopy(summary.getMimeDistribution());
                            for (Iterator i = fd.keySet().iterator(); i.hasNext();) {
                                Object key = i.next();
                                long count = ((AtomicLong)fd.get(key)).get();
                                long displaybarwidth = 0;
                                double per = ((double)count) / summary.getTotalMimeTypeDocuments();
                                if(summary.getTotalMimeTypeDocuments()>1){
                                   displaybarwidth = (long)(per * MAX_BAR_WIDTH);
                                } 
                                if(displaybarwidth==0){
                                   displaybarwidth=1;
                                }
                                
                                String percent = percentFormat.format(100 * per);
            
      out.write("\n                    ");
      out.write("<tr ");
      out.print(alt?"bgcolor=#EEEEFF":"");
      out.write(">\n                        ");
      out.write("<td nowrap>\n                            ");
      out.write("<a style=\"text-decoration: none;\" href=\"");
      out.print(request.getContextPath());
      out.write("/logs.jsp?job=");
      out.print(cjob.getUID());
      out.write("&log=crawl.log&mode=regexpr&regexpr=^[^ ].*");
      out.print((String)key);
      out.write("&grep=true\">");
      out.print(key);
      out.write("</a>&nbsp;&nbsp;\n                        ");
      out.write("</td>\n                        ");
      out.write("<td nowrap>\n                            ");
      out.write("<img src=\"");
      out.print(request.getContextPath());
      out.write("/images/blue.jpg\" height=\"10\" width=\"");
      out.print(displaybarwidth);
      out.write("\"> ");
      out.print(count);
      out.write(" &nbsp;\n                            \t");
      out.write("<span class=percent>(");
      out.print(percent);
      out.write("%)");
      out.write("</span>\n                        ");
      out.write("</td>\n                        ");
      out.write("<td align=\"right\" nowrap>\n                            ");
      out.print(ArchiveUtils.formatBytesForDisplay(summary.getBytesPerMimeType((String)key)));
      out.write("&nbsp;\n                        ");
      out.write("</td>\n                    ");
      out.write("</tr>\n            ");

                            alt = !alt;
                            }
            
      out.write("   \n           \n            ");
      out.write("<tr class=\"totalrow\">\n  \t        \t");
      out.write("<td>");
      out.write("<b>Total");
      out.write("</b>");
      out.write("</td>\n  \t        \t");
      out.write("<td>");
      out.print(summary.getTotalMimeTypeDocuments());
      out.write(" &nbsp; ");
      out.write("</td> \n  \t        \t");
      out.write("<td align=\"right\" nowrap>\n        \t      \t\t");
      out.print(ArchiveUtils.formatBytesForDisplay(summary.getTotalMimeSize()));
      out.write("&nbsp;\n        \t    ");
      out.write("</td>\n            ");
      out.write("</tr>\n            \t    \n        \t");
      out.write("<tr>\n        \t\t");
      out.write("<td>&nbsp;");
      out.write("</td>\n        \t");
      out.write("</tr>\n            ");
      out.write("<tr class=\"headerrow\">\n                ");
      out.write("<th>\n                    Hosts\n                ");
      out.write("</th>\n                ");
      out.write("<th>\n                    Documents\n                ");
      out.write("</th>\n                ");
      out.write("<th>\n                    Data\n                ");
      out.write("</th>\n            ");
      out.write("</tr>\n            ");

                            alt = true;
                            SortedMap hd = summary.getReverseSortedHostsDistribution();
                            for (Iterator i = hd.keySet().iterator(); i.hasNext();) {
                                Object key = i.next();
                                AtomicLong lw = (AtomicLong)hd.get(key);
                                long count = lw == null ? 0 : lw.get();
                                long displaybarwidth = 0;
                                double per = ((double)count) / summary.getTotalHostDocuments();
                                if(summary.getTotalHostDocuments() > 1) {
                                   displaybarwidth = (long)(per * MAX_BAR_WIDTH);
                                } 
                                if(displaybarwidth==0){
                                   displaybarwidth=1;
                                }
                                
                                String percent = percentFormat.format(100 * per);
            
      out.write("\n                    ");
      out.write("<tr ");
      out.print(alt?"bgcolor=#EEEEFF":"");
      out.write(">\n                        ");
      out.write("<td nowrap>\n                            ");
      out.write("<a style=\"text-decoration: none;\" href=\"");
      out.print(request.getContextPath());
      out.write("/logs.jsp?job=");
      out.print(cjob.getUID());
      out.write("&log=crawl.log&mode=regexpr&regexpr=^[^ ].*");
      out.print((String)key);
      out.write("&grep=true\">");
      out.print((String)key);
      out.write("</a>&nbsp;\n                        ");
      out.write("</td>\n                        ");
      out.write("<td nowrap>\n                            ");
      out.write("<img src=\"");
      out.print(request.getContextPath());
      out.write("/images/blue.jpg\" height=\"10\" width=\"");
      out.print(displaybarwidth);
      out.write("\"> ");
      out.print(count);
      out.write(" &nbsp;\n                            \t");
      out.write("<span class=percent>(");
      out.print(percent);
      out.write("%)");
      out.write("</span>\n                        ");
      out.write("</td>\n                        ");
      out.write("<td align=\"right\" nowrap>\n                            ");
      out.print(ArchiveUtils.formatBytesForDisplay(summary.getBytesPerHost((String)key)));
      out.write("&nbsp;\n                        ");
      out.write("</td>                      \n                    ");
      out.write("</tr>\n            ");

                            alt = !alt;
                            }
            
      out.write("                \n            ");
      out.write("<tr class=\"totalrow\">\n  \t        \t");
      out.write("<td>");
      out.write("<b>Total");
      out.write("</b>");
      out.write("</td>\n  \t        \t");
      out.write("<td>");
      out.print(summary.getTotalHostDocuments());
      out.write(" &nbsp; ");
      out.write("</td> \n  \t        \t");
      out.write("<td align=\"right\" nowrap>\n        \t      \t\t");
      out.print(ArchiveUtils.formatBytesForDisplay(summary.getTotalHostSize()));
      out.write("&nbsp;\n        \t    ");
      out.write("</td>\n            ");
      out.write("</tr>\n            \n            ");
      out.write("<tr>\n            \t");
      out.write("<td>&nbsp;");
      out.write("</td>\n            ");
      out.write("</tr>\n            \n            ");
      out.write("</table>\n            \n            \n            \n            ");
      out.write("<table cellspacing=\"0\" width=750>\t   \n                \n            ");
      out.write("<tr class=\"headerrow\">\n                ");
      out.write("<th width=70>\n                    TLD\n                ");
      out.write("</th>\n                ");
      out.write("<th width=100>\n                \tHosts\n                ");
      out.write("</th>\n                ");
      out.write("<th>\n                    Documents\n                ");
      out.write("</th>\n                ");
      out.write("<th>\n                    Data\n                ");
      out.write("</th>\n            ");
      out.write("</tr>\n            ");

                        	alt = true;
                        	scd = summary.getReverseSortedCopy(summary.getTldDistribution());
                            for (Iterator i = scd.keySet().iterator(); i.hasNext();) {
                                Object key = i.next();
                                AtomicLong lw = (AtomicLong)scd.get(key);
                                long count = lw == null ? 0 : lw.get();
                                long displaybarwidth = 0;
                                double per = ((double)count) / summary.getTotalTldDocuments();
                                
                                long hostsPerTld = summary.getHostsPerTld((String)key);
                                double perHost = ((double)hostsPerTld) / summary.getTotalHosts();
                                
                                if (summary.getTotalTldDocuments() > 1) {
                                   displaybarwidth = (long)(per * MAX_BAR_WIDTH);
                                } 
                                if (displaybarwidth == 0){
                                   displaybarwidth = 1;
                                }
                                
                                String percent = percentFormat.format(100 * per);
                                String percentHost = percentFormat.format(100 * perHost);
            
      out.write("\n                    ");
      out.write("<tr ");
      out.print(alt?"bgcolor=#EEEEFF":"");
      out.write(">\n                        ");
      out.write("<td nowrap>\n                            ");
      out.write("<a style=\"text-decoration: none;\" href=\"");
      out.print(request.getContextPath());
      out.write("/logs.jsp?job=");
      out.print(cjob.getUID());
      out.write("&log=crawl.log&mode=regexpr&regexpr=^[^ ].*");
      out.print((String)key);
      out.write("&grep=true\">");
      out.print((String)key);
      out.write("</a>&nbsp;\n                        ");
      out.write("</td>\n                        ");
      out.write("<td>\n                        \t");
      out.print(hostsPerTld);
      out.write(" &nbsp; ");
      out.write("<span class=percent>(");
      out.print(percentHost);
      out.write("%)");
      out.write("</span>\n                        ");
      out.write("</td>\n                        ");
      out.write("<td nowrap>\n                            ");
      out.write("<img src=\"");
      out.print(request.getContextPath());
      out.write("/images/blue.jpg\" height=\"10\" width=\"");
      out.print(displaybarwidth);
      out.write("\"> ");
      out.print(count);
      out.write(" &nbsp;\n                            \t");
      out.write("<span class=percent>(");
      out.print(percent);
      out.write("%)");
      out.write("</span>\n                        ");
      out.write("</td>\n                        ");
      out.write("<td align=\"right\" nowrap>\n                            ");
      out.print(ArchiveUtils.formatBytesForDisplay(summary.getBytesPerTld((String)key)));
      out.write("&nbsp;\n                        ");
      out.write("</td>                      \n                    ");
      out.write("</tr>\n            ");

                            alt = !alt;
                            }
            
      out.write("                \n            ");
      out.write("<tr class=\"totalrow\">\n  \t        \t");
      out.write("<td>");
      out.write("<b>Total");
      out.write("</b>");
      out.write("</td>\n  \t        \t");
      out.write("<td>");
      out.print(summary.getTotalHosts());
      out.write("</td>\n  \t        \t");
      out.write("<td>");
      out.print(summary.getTotalTldDocuments());
      out.write(" &nbsp; ");
      out.write("</td> \n  \t        \t");
      out.write("<td align=\"right\" nowrap>\n        \t      \t\t");
      out.print(ArchiveUtils.formatBytesForDisplay(summary.getTotalTldSize()));
      out.write("&nbsp;\n        \t    ");
      out.write("</td>\n            ");
      out.write("</tr>                   \n            \n        ");
      out.write("</table>\n        \n         ");
      out.write("<p>\n\t\t");
      out.write("<br>\n\t\t");
      out.write("</p>\n\t\t\n\t");
      out.write("<table width=750>\n\t    ");
      out.write("<tr>\n\t        ");
      out.write("<td valign=\"center\" >");
      out.write("<img \n\t        src=\"");
      out.print(request.getContextPath());
      out.write("/images/blue.jpg\" height=\"1\" width=\"40\">");
      out.write("</td>\n\t        ");
      out.write("<td align=\"center\">");
      out.write("<i>DNS");
      out.write("</i>");
      out.write("</td>\n\t        ");
      out.write("<td valign=\"center\" >");
      out.write("<img \n\t        src=\"");
      out.print(request.getContextPath());
      out.write("/images/blue.jpg\" height=\"1\" width=\"660\">");
      out.write("</td>\n\t    ");
      out.write("</tr>\n\t");
      out.write("</table>\n\t\n         ");
      out.write("<table cellspacing=\"0\" width=750>\n\t        \n        \t");
      out.write("<tr class=\"headerrow\">\n                ");
      out.write("<th>\n                    Status code\n                ");
      out.write("</th>\n                ");
      out.write("<th width=\"200\" colspan=\"2\">\n                    Documents\n                ");
      out.write("</th>\n            ");
      out.write("</tr>\n            ");

                            alt = true;
                            scd = summary.getReverseSortedCopy(summary.getDnsStatusCodeDistribution());
                            for (Iterator i = scd.keySet().iterator(); i.hasNext();) {
                                Object key = i.next();
                                long count = ((AtomicLong)scd.get(key)).get();
                                long displaybarwidth = 0;
                                long barwidthadjust = 3;
                                double per = ((double)count) / summary.getTotalDnsStatusCodeDocuments(); 
                                if(summary.getTotalDnsStatusCodeDocuments()/barwidthadjust>0){
                                   displaybarwidth = (long)(per * MAX_BAR_WIDTH);
                                } 
                                if(displaybarwidth==0){
                                   displaybarwidth=1;
                                }
                                             
                                String percent = percentFormat.format(100 * per);
            
      out.write("\n                    ");
      out.write("<tr ");
      out.print(alt?"bgcolor=#EEEEFF":"");
      out.write(">\n                        ");
      out.write("<td nowrap>\n                            ");
      out.write("<a style=\"text-decoration: none;\" href=\"");
      out.print(request.getContextPath());
      out.write("/logs.jsp?job=");
      out.print(cjob.getUID());
      out.write("&log=crawl.log&mode=regexpr&regexpr=^.{24}\\s*");
      out.print((String)key);
      out.write("&grep=true\">\n                                ");
      out.print(CrawlURI.fetchStatusCodesToString(Integer.parseInt((String)key)));
      out.write("\n                            ");
      out.write("</a>&nbsp;\n                        ");
      out.write("</td>\n                        ");
      out.write("<td colspan=\"2\" nowrap>\n                            ");
      out.write("<img src=\"");
      out.print(request.getContextPath());
      out.write("/images/blue.jpg\" height=\"10\" width=\"");
      out.print(displaybarwidth);
      out.write("\"> ");
      out.print(count);
      out.write(" &nbsp;\n                            \t");
      out.write("<span class=percent>(");
      out.print(percent);
      out.write("%)");
      out.write("</span>\n                        ");
      out.write("</td>\n                    ");
      out.write("</tr>\n            ");

                            alt = !alt;
                            }
            
      out.write("                \n            ");
      out.write("<tr class=\"totalrow\">\n            \t");
      out.write("<td>");
      out.write("<b>Total:");
      out.write("</b>");
      out.write("</td>\n            \t");
      out.write("<td>");
      out.print(summary.getTotalDnsStatusCodeDocuments());
      out.write(" &nbsp; ");
      out.write("</td>\n            \t");
      out.write("<td>&nbsp;");
      out.write("</td>\n            ");
      out.write("</tr>\n            ");
      out.write("<tr>\n                ");
      out.write("<td>&nbsp;");
      out.write("</td>\n            ");
      out.write("</tr>\n            ");
      out.write("<tr class=\"headerrow\">\n                ");
      out.write("<th width=\"100\">\n                    MIME type\n                ");
      out.write("</th>\n                ");
      out.write("<th width=\"200\">\n                    Documents\n                ");
      out.write("</th>\n                ");
      out.write("<th>\n                    Data\n                ");
      out.write("</th>\n            ");
      out.write("</tr>\n            ");

                            alt = true;
                            fd = summary.getReverseSortedCopy(summary.getDnsMimeDistribution());
                            for (Iterator i = fd.keySet().iterator(); i.hasNext();) {
                                Object key = i.next();
                                long count = ((AtomicLong)fd.get(key)).get();
                                long displaybarwidth = 0;
                                double per = ((double)count)/summary.getTotalDnsMimeTypeDocuments();
                                if(summary.getTotalMimeTypeDocuments()/6>0){
                                   displaybarwidth = (long)(per * MAX_BAR_WIDTH);
                                } 
                                if(displaybarwidth==0){
                                   displaybarwidth=1;
                                }
                                
                                String percent = percentFormat.format(100 * per);
            
      out.write("\n                    ");
      out.write("<tr ");
      out.print(alt?"bgcolor=#EEEEFF":"");
      out.write(">\n                        ");
      out.write("<td nowrap>\n                            ");
      out.write("<a style=\"text-decoration: none;\" href=\"");
      out.print(request.getContextPath());
      out.write("/logs.jsp?job=");
      out.print(cjob.getUID());
      out.write("&log=crawl.log&mode=regexpr&regexpr=^[^ ].*");
      out.print((String)key);
      out.write("&grep=true\">");
      out.print(key);
      out.write("</a>&nbsp;&nbsp;\n                        ");
      out.write("</td>\n                        ");
      out.write("<td nowrap>\n                            ");
      out.write("<img src=\"");
      out.print(request.getContextPath());
      out.write("/images/blue.jpg\" height=\"10\" width=\"");
      out.print(displaybarwidth);
      out.write("\"> ");
      out.print(count);
      out.write(" &nbsp;\n                             ");
      out.write("<span class=percent>(");
      out.print(percent);
      out.write("%)");
      out.write("</span>\n                        ");
      out.write("</td>\n                        ");
      out.write("<td align=\"right\" nowrap>\n                            ");
      out.print(ArchiveUtils.formatBytesForDisplay(summary.getBytesPerMimeType((String)key)));
      out.write("&nbsp;                           \n                        ");
      out.write("</td>\n                    ");
      out.write("</tr>\n            ");

                            alt = !alt;
                            }
            
      out.write("   \n\t\t            ");
      out.write("<tr class=\"totalrow\">\n        \t        \t");
      out.write("<td>");
      out.write("<b>Total");
      out.write("</b>");
      out.write("</td>\n        \t        \t");
      out.write("<td>\n        \t        \t\t");
      out.print(summary.getTotalDnsMimeTypeDocuments());
      out.write(" &nbsp; \n        \t        \t");
      out.write("</td> \n        \t        \t");
      out.write("<td align=\"right\" nowrap>\n        \t        \t\t");
      out.print(ArchiveUtils.formatBytesForDisplay(summary.getTotalDnsMimeSize()));
      out.write("&nbsp;\n        \t        \t");
      out.write("</td>\n            \t    ");
      out.write("</tr>\n            \t    \n            \t    ");
      out.write("<tr>\n            \t");
      out.write("<td>&nbsp;");
      out.write("</td>\n            ");
      out.write("</tr>\n            \n            \t    ");
      out.write("<tr class=\"headerrow\">\n                ");
      out.write("<th>\n                    Hosts\n                ");
      out.write("</th>\n                ");
      out.write("<th>\n                    Documents\n                ");
      out.write("</th>\n                ");
      out.write("<th>\n                    Data\n                ");
      out.write("</th>\n            ");
      out.write("</tr>\n            ");

                            alt = true;
                            hd = summary.getReverseSortedCopy(summary.getHostsDnsDistribution());
                            for (Iterator i = hd.keySet().iterator(); i.hasNext();) {
                                Object key = i.next();
                                AtomicLong lw = (AtomicLong)hd.get(key);
                                long count = lw == null ? 0 : lw.get();
                                long displaybarwidth = 0;
                                double per = ((double)count) / summary.getTotalHostDnsDocuments();
                                if(summary.getTotalHostDnsDocuments() > 1) {
                                   displaybarwidth = (long)(per * MAX_BAR_WIDTH);
                                } 
                                if(displaybarwidth==0){
                                   displaybarwidth=1;
                                }
                                
                                String percent = percentFormat.format(100 * per);
            
      out.write("\n                    ");
      out.write("<tr ");
      out.print(alt?"bgcolor=#EEEEFF":"");
      out.write(">\n                        ");
      out.write("<td nowrap>\n                            ");
      out.write("<a style=\"text-decoration: none;\" href=\"");
      out.print(request.getContextPath());
      out.write("/logs.jsp?job=");
      out.print(cjob.getUID());
      out.write("&log=crawl.log&mode=regexpr&regexpr=^[^ ].*");
      out.print((String)key);
      out.write("&grep=true\">");
      out.print((String)key);
      out.write("</a>\n                        ");
      out.write("</td>\n                        ");
      out.write("<td nowrap>\n                            ");
      out.write("<img src=\"");
      out.print(request.getContextPath());
      out.write("/images/blue.jpg\" height=\"10\" width=\"");
      out.print(displaybarwidth);
      out.write("\"> ");
      out.print(count);
      out.write(" &nbsp;\n                            \t");
      out.write("<span class=percent>(");
      out.print(percent);
      out.write("%)");
      out.write("</span>\n                        ");
      out.write("</td>\n                        ");
      out.write("<td align=\"right\" nowrap>\n                            ");
      out.print(ArchiveUtils.formatBytesForDisplay(summary.getBytesPerHost((String)key)));
      out.write("&nbsp;\n                        ");
      out.write("</td>                      \n                    ");
      out.write("</tr>\n            ");

                            alt = !alt;
                            }
            
      out.write("                \n            ");
      out.write("<tr class=\"totalrow\">\n  \t        \t");
      out.write("<td>");
      out.write("<b>Total");
      out.write("</b>");
      out.write("</td>\n  \t        \t");
      out.write("<td>");
      out.print(summary.getTotalDnsHostDocuments());
      out.write(" &nbsp; ");
      out.write("</td> \n  \t        \t");
      out.write("<td align=\"right\" nowrap>\n        \t      \t\t");
      out.print(ArchiveUtils.formatBytesForDisplay(summary.getTotalDnsHostSize()));
      out.write("&nbsp;\n        \t    ");
      out.write("</td>\n            ");
      out.write("</tr>\n            \n            ");
      out.write("<tr>\n            \t");
      out.write("<td>&nbsp;");
      out.write("</td>\n            ");
      out.write("</tr>\n\t");
      out.write("</table>\n ");

         } else if(stats == null) {
         out.println("<b>No statistics associated with job.</b><p><b>Job status:</b> " + cjob.getStatus());            
         if(cjob.getErrorMessage()!=null){
             out.println("<p><pre><font color='red'>"+cjob.getErrorMessage()+"</font></pre>");
         }
     } else {
 
      out.write("\n        ");
      out.write("<table border=\"0\">\n            ");
      out.write("<tr>\n                ");
      out.write("<td valign=\"top\">\n                    ");
      out.write("<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" >\n                        ");
      out.write("<tr>\n                            ");
      out.write("<td>\n                                ");
      out.write("<b>Job name:");
      out.write("</b>&nbsp;\n                            ");
      out.write("</td>\n                            ");
      out.write("<td>\n                                ");
      out.print(cjob.getJobName());
      out.write("\n                            ");
      out.write("</td>\n                        ");
      out.write("</tr>\n                        ");
      out.write("<tr>\n                            ");
      out.write("<td>\n                                ");
      out.write("<b>Status:");
      out.write("</b>&nbsp;\n                            ");
      out.write("</td>\n                            ");
      out.write("<td>\n                                ");
      out.print(cjob.getStatus());
      out.write("\n                            ");
      out.write("</td>\n                        ");
      out.write("</tr>\n                        ");
      out.write("<tr>\n                            ");
      out.write("<td>\n                                ");
      out.write("<b>Time:");
      out.write("</b>&nbsp;\n                            ");
      out.write("</td>\n                            ");
      out.write("<td>\n                                ");

                                                                    long time = (stats.getCrawlerTotalElapsedTime())/1000;
                                                                    if(time>3600)
                                                                    {
                                                                        //got hours.
                                                                        out.println(time/3600 + " h., ");
                                                                        time = time % 3600;
                                                                    }
                                                                    
                                                                    if(time > 60)
                                                                    {
                                                                        out.println(time/60 + " min. and ");
                                                                        time = time % 60;
                                                                    }

                                                                    out.println(time + " sec.");
                                
      out.write("\n                            ");
      out.write("</td>\n                        ");
      out.write("</tr>\n                    ");
      out.write("</table>\n                ");
      out.write("</td>\n                ");
      out.write("<td>\n                    &nbsp;&nbsp;&nbsp;\n                ");
      out.write("</td>\n                ");
      out.write("<td valign=\"top\">\n                    ");
      out.write("<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" >\n                        ");
      out.write("<tr>\n                            ");
      out.write("<td>\n                                ");
      out.write("<b>Processed docs/sec:");
      out.write("</b>&nbsp;\n                            ");
      out.write("</td>\n                            ");
      out.write("<td>\n                                ");

                                                                    if(cjob.getStatus().equalsIgnoreCase(CrawlJob.STATUS_RUNNING))
                                                                    {
                                                                        // Show current and overall stats.
                                
      out.write("\n                                        ");
      out.print(ArchiveUtils.doubleToString(stats.currentProcessedDocsPerSec(),2));
      out.write(" (");
      out.print(ArchiveUtils.doubleToString(stats.processedDocsPerSec(),2));
      out.write(")\n                                ");

                                                                            }
                                                                            else
                                                                            {
                                                                                // Only show overall stats.
                                        
      out.write("\n                                        ");
      out.print(ArchiveUtils.doubleToString(stats.processedDocsPerSec(),2));
      out.write("\n                                ");

                                }
                                
      out.write("\n                            ");
      out.write("</td>\n                        ");
      out.write("</tr>\n                        ");
      out.write("<tr>\n                            ");
      out.write("<td>\n                                ");
      out.write("<b>Processed KB/sec:");
      out.write("</b>&nbsp;\n                            ");
      out.write("</td>\n                            ");
      out.write("<td>\n                                ");

                                                                    if(cjob.getStatus().equalsIgnoreCase(CrawlJob.STATUS_RUNNING))
                                                                    {
                                                                        // Show current and overall stats.
                                
      out.write("\n                                        ");
      out.print(ArchiveUtils.doubleToString(stats.currentProcessedKBPerSec(),2));
      out.write(" (");
      out.print(ArchiveUtils.doubleToString(stats.processedKBPerSec(),2));
      out.write(")\n                                ");

                                                                            }
                                                                            else
                                                                            {
                                                                                // Only show overall stats.
                                        
      out.write("\n                                        ");
      out.print(ArchiveUtils.doubleToString(stats.processedKBPerSec(),2));
      out.write("\n                                ");

                                }
                                
      out.write("\n                            ");
      out.write("</td>\n                        ");
      out.write("</tr>\n                        ");
      out.write("<tr>\n                            ");
      out.write("<td>\n                                ");
      out.write("<b>Total data written:");
      out.write("</b>&nbsp;\n                            ");
      out.write("</td>\n                            ");
      out.write("<td>\n                                ");
      out.print(ArchiveUtils.formatBytesForDisplay(stats.totalBytesWritten()));
      out.write("\n                            ");
      out.write("</td>\n                        ");
      out.write("</tr>\n                    ");
      out.write("</table>\n                ");
      out.write("</td>\n            ");
      out.write("</tr>\n        ");
      out.write("</table>\n        \n        ");
      out.write("<p>\n        \n        ");
      out.write("<table width=\"400\">\n            ");
      out.write("<tr>\n                ");
      out.write("<td colspan=\"6\">\n                    ");
      out.write("<table>\n                        ");
      out.write("<tr>\n                            ");
      out.write("<td valign=\"center\" >");
      out.write("<img \n                            src=\"");
      out.print(request.getContextPath());
      out.write("/images/blue.jpg\" height=\"1\" width=\"40\">");
      out.write("</td>\n                            ");
      out.write("<td align=\"center\">");
      out.write("<i>URIs");
      out.write("</i>");
      out.write("</td>\n                            ");
      out.write("<td valign=\"center\" >");
      out.write("<img \n                            src=\"");
      out.print(request.getContextPath());
      out.write("/images/blue.jpg\" height=\"1\" width=\"300\">");
      out.write("</td>\n                        ");
      out.write("</tr>\n                    ");
      out.write("</table>\n                ");
      out.write("</td>\n            ");
      out.write("</tr>\n            ");
      out.write("<tr>\n                ");
      out.write("<td nowrap>\n                    ");
      out.write("<b>Discovered:");
      out.write("</b>\n                ");
      out.write("</td>\n                ");
      out.write("<td align=\"right\">\n                    ");
      out.print(stats.discoveredUriCount());
      out.write("\n                ");
      out.write("</td>\n                ");
      out.write("<td colspan=\"3\">\n                    &nbsp;");
      out.write("<a class='help' href=\"javascript:alert('URIs that the crawler has discovered and confirmed to be within scope. \\nNOTE: Because the same URI can be fetched multiple times this number may be lower then the number of queued, in process and finished URIs.')\">?");
      out.write("</a>\n                ");
      out.write("</td>\n            ");
      out.write("</tr>\n            ");
      out.write("<tr>\n                ");
      out.write("<td nowrap>\n                    &nbsp;&nbsp;");
      out.write("<b>Queued:");
      out.write("</b>\n                ");
      out.write("</td>\n                ");
      out.write("<td align=\"right\">\n                    ");
      out.print(stats.queuedUriCount());
      out.write("\n                ");
      out.write("</td>\n                ");
      out.write("<td colspan=\"3\">\n                    &nbsp;");
      out.write("<a class='help' href=\"javascript:alert('URIs that are waiting to be processed. \\nThat is all URI that have been discovered (or should be revisited) that are waiting for processing.')\">?");
      out.write("</a>\n                ");
      out.write("</td>\n            ");
      out.write("</tr>\n            ");
      out.write("<tr>\n                ");
      out.write("<td nowrap>\n                    &nbsp;&nbsp;");
      out.write("<b>In progress:");
      out.write("</b>\n                ");
      out.write("</td>\n                ");
      out.write("<td align=\"right\">\n                    ");
      out.print(stats.activeThreadCount());
      out.write("\n                ");
      out.write("</td>\n                ");
      out.write("<td colspan=\"3\">\n                    &nbsp;");
      out.write("<a class='help' href=\"javascript:alert('Number of URIs being processed at the moment. \\nThis is based on the number of active threads.')\">?");
      out.write("</a>\n                ");
      out.write("</td>\n            ");
      out.write("</tr>\n            ");
      out.write("<tr>\n                ");
      out.write("<td>\n                ");
      out.write("</td>\n                ");
      out.write("<td align=\"right\" width=\"1\" nowrap>\n                    &nbsp;&nbsp;");
      out.write("<i>Total");
      out.write("</i>\n                ");
      out.write("</td>\n                ");
      out.write("<td align=\"right\" width=\"1\" nowrap>\n                    &nbsp;&nbsp;");
      out.write("<i>Successfully");
      out.write("</i>\n                ");
      out.write("</td>\n                ");
      out.write("<td align=\"right\" width=\"1\" nowrap>\n                    &nbsp;&nbsp;");
      out.write("<i>Failed");
      out.write("</i>\n                ");
      out.write("</td>\n                ");
      out.write("<td align=\"right\" width=\"1\" nowrap>\n                    &nbsp;&nbsp;");
      out.write("<i>Disregarded");
      out.write("</i>\n                ");
      out.write("</td>\n            ");
      out.write("</tr>\n            ");
      out.write("<tr>\n                ");
      out.write("<td nowrap>\n                    &nbsp;&nbsp;");
      out.write("<b>Finished:");
      out.write("</b>\n                ");
      out.write("</td>\n                ");
      out.write("<td align=\"right\">\n                    ");
      out.print(stats.finishedUriCount());
      out.write("\n                ");
      out.write("</td>\n                ");
      out.write("<td align=\"right\">\n                    ");
      out.print(stats.successfullyFetchedCount());
      out.write("\n                ");
      out.write("</td>\n                ");
      out.write("<td align=\"right\">\n                    ");
      out.print(stats.failedFetchAttempts());
      out.write("\n                ");
      out.write("</td>\n                ");
      out.write("<td align=\"right\">\n                    ");
      out.print(stats.disregardedFetchAttempts());
      out.write("\n                ");
      out.write("</td>\n            ");
      out.write("</tr>\n        ");
      out.write("</table>\n        \n        ");
      out.write("<p>\n\n        ");
      out.write("<table cellspacing=\"0\">\n            ");
      out.write("<tr>\n                ");
      out.write("<th>\n                    Status code\n                ");
      out.write("</th>\n                ");
      out.write("<th width=\"200\" colspan=\"2\">\n                    Documents\n                ");
      out.write("</th>\n            ");
      out.write("</tr>\n            ");

                            boolean alt = true;
                            TreeMap scd = stats.getReverseSortedCopy(stats.
                                getStatusCodeDistribution());
                            for (Iterator i = scd.keySet().iterator();
                                    i.hasNext();) {
                                Object key = i.next();
                                long count = ((AtomicLong)scd.get(key)).get();
                                long displaybarwidth = 0;
                                if(stats.successfullyFetchedCount()/6>0){
                                   displaybarwidth = count*100/(stats.successfullyFetchedCount()/6);
                                } 
                                if(displaybarwidth==0){
                                   displaybarwidth=1;
                                }
            
      out.write("\n                    ");
      out.write("<tr ");
      out.print(alt?"bgcolor=#EEEEFF":"");
      out.write(">\n                        ");
      out.write("<td nowrap>\n                            ");
      out.write("<a style=\"text-decoration: none;\" href=\"");
      out.print(request.getContextPath());
      out.write("/logs.jsp?job=");
      out.print(cjob.getUID());
      out.write("&log=crawl.log&mode=regexpr&regexpr=^.{24}\\s*");
      out.print((String)key);
      out.write("&grep=true\">\n                                ");
      out.print(CrawlURI.fetchStatusCodesToString(Integer.parseInt((String)key)));
      out.write("\n                            ");
      out.write("</a>&nbsp;\n                        ");
      out.write("</td>\n                        ");
      out.write("<td colspan=\"2\" nowrap>\n                            ");
      out.write("<img src=\"");
      out.print(request.getContextPath());
      out.write("/images/blue.jpg\" height=\"10\" width=\"");
      out.print(displaybarwidth);
      out.write("\"> ");
      out.print(count);
      out.write(" &nbsp;\n                        ");
      out.write("</td>\n                    ");
      out.write("</tr>\n            ");

                    alt = !alt;
                }
            
      out.write("                \n            ");
      out.write("<tr>\n                ");
      out.write("<td>&nbsp;");
      out.write("</td>\n            ");
      out.write("</tr>\n            ");
      out.write("<tr>\n                ");
      out.write("<th width=\"100\">\n                    File type\n                ");
      out.write("</th>\n                ");
      out.write("<th width=\"200\">\n                    Documents\n                ");
      out.write("</th>\n                ");
      out.write("<th>\n                    Data\n                ");
      out.write("</th>\n            ");
      out.write("</tr>\n            ");

                alt=true;
                TreeMap fd = stats.getReverseSortedCopy(stats.
                        getFileDistribution());
                for (Iterator i = fd.keySet().iterator(); i.hasNext();) {
                    Object key = i.next();
                    long count = ((AtomicLong)fd.get(key)).get();
                    long displaybarwidth = 0;
                    if(stats.successfullyFetchedCount()/6>0){
                       displaybarwidth = count*100/(stats.successfullyFetchedCount()/6);
                    } 
                    if(displaybarwidth==0){
                       displaybarwidth=1;
                    }
            
      out.write("\n                    ");
      out.write("<tr ");
      out.print(alt?"bgcolor=#EEEEFF":"");
      out.write(">\n                        ");
      out.write("<td nowrap>\n                            ");
      out.write("<a style=\"text-decoration: none;\" href=\"");
      out.print(request.getContextPath());
      out.write("/logs.jsp?job=");
      out.print(cjob.getUID());
      out.write("&log=crawl.log&mode=regexpr&regexpr=^[^ ].*");
      out.print((String)key);
      out.write("&grep=true\">");
      out.print(key);
      out.write("</a>&nbsp;&nbsp;\n                        ");
      out.write("</td>\n                        ");
      out.write("<td nowrap>\n                            ");
      out.write("<img src=\"");
      out.print(request.getContextPath());
      out.write("/images/blue.jpg\" height=\"10\" width=\"");
      out.print(displaybarwidth);
      out.write("\"> ");
      out.print(count);
      out.write(" &nbsp;&nbsp;\n                        ");
      out.write("</td>\n                        ");
      out.write("<td align=\"right\" nowrap>\n                            ");
      out.print(ArchiveUtils.formatBytesForDisplay(stats.getBytesPerFileType((String)key)));
      out.write("\n                        ");
      out.write("</td>\n                    ");
      out.write("</tr>\n            ");

                    alt = !alt;
                }
            
      out.write("                \n        ");
      out.write("</table>\n        \n        ");
      out.write("<p>\n        \n        ");
      out.write("<table cellspacing=\"0\">\n            ");
      out.write("<tr>\n                ");
      out.write("<th>\n                    Hosts&nbsp;\n                ");
      out.write("</th>\n                ");
      out.write("<th>\n                    Documents&nbsp;\n                ");
      out.write("</th>\n                ");
      out.write("<th>\n                    Data&nbsp;\n                ");
      out.write("</th>\n                ");
 if (cjob.getStatus().equals(CrawlJob.STATUS_RUNNING) ||
                         cjob.getStatus().equals(CrawlJob.STATUS_PAUSED) ||
                         cjob.getStatus().equals(CrawlJob.STATUS_WAITING_FOR_PAUSE)){ 
      out.write("\n                    ");
      out.write("<th>\n                        Time since last URI finished\n                    ");
      out.write("</th>\n                ");
 } 
      out.write("\n            ");
      out.write("</tr>\n            ");

                            alt = true;
                            SortedMap hd = stats.getReverseSortedHostsDistribution();
                            for (Iterator i = hd.keySet().iterator(); i.hasNext();) {
                                Object key = i.next();
            
      out.write("\n                    ");
      out.write("<tr ");
      out.print(alt?"bgcolor=#EEEEFF":"");
      out.write(">\n                        ");
      out.write("<td nowrap>\n                            ");
      out.write("<a style=\"text-decoration: none;\" href=\"");
      out.print(request.getContextPath());
      out.write("/logs.jsp?job=");
      out.print(cjob.getUID());
      out.write("&log=crawl.log&mode=regexpr&regexpr=^[^ ].*");
      out.print((String)key);
      out.write("&grep=true\">");
      out.print((String)key);
      out.write("</a>&nbsp;\n                        ");
      out.write("</td>\n                        ");
      out.write("<td nowrap>\n                            ");
 AtomicLong lw = ((AtomicLong)hd.get(key)); 
      out.write("\n                            ");
      out.print((lw == null) ?
                                "null": Long.toString(lw.get()));
      out.write("&nbsp;\n                        ");
      out.write("</td>\n                        ");
      out.write("<td align=\"right\" nowrap>\n                            ");
      out.print(ArchiveUtils.formatBytesForDisplay(stats.getBytesPerHost((String)key)));
      out.write("&nbsp;\n                        ");
      out.write("</td>\n                        ");
 if (cjob.getStatus().equalsIgnoreCase(CrawlJob.STATUS_RUNNING) ||
                                 cjob.getStatus().equals(CrawlJob.STATUS_PAUSED) ||
                                 cjob.getStatus().equals(CrawlJob.STATUS_WAITING_FOR_PAUSE)){ 
      out.write("\n                            ");
      out.write("<td align=\"right\">\n                                ");
      out.print(ArchiveUtils.formatMillisecondsToConventional(System.currentTimeMillis()-stats.getHostLastFinished((String)key).longValue()));
      out.write("\n                            ");
      out.write("</td>\n                        ");
 } 
      out.write("\n                    ");
      out.write("</tr>\n            ");

                    alt = !alt;
                }
            
      out.write("                \n        ");
      out.write("</table>\n");

    } // End if(cjob==null)else clause

      out.write("\n");

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
