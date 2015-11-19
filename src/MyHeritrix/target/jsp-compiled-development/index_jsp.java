package org.apache.jsp;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import org.apache.jasper.runtime.*;
import org.archive.crawler.admin.CrawlJobHandler;
import org.archive.crawler.admin.CrawlJob;
import org.archive.crawler.Heritrix;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.admin.CrawlJob;
import org.archive.crawler.Heritrix;
import org.archive.crawler.admin.StatisticsTracker;
import org.archive.util.ArchiveUtils;
import org.archive.util.TextUtils;
import javax.servlet.jsp.JspWriter;
import java.text.SimpleDateFormat;
import org.archive.crawler.Heritrix;
import org.archive.crawler.admin.CrawlJob;
import org.archive.util.ArchiveUtils;
import org.archive.util.TextUtils;
import org.archive.crawler.admin.StatisticsTracker;
import org.archive.crawler.Heritrix;
import java.util.Map;
import java.util.Iterator;

public class index_jsp extends HttpJspBase {


	private void printTime(final JspWriter out,long time)
    throws java.io.IOException {
	    out.println(ArchiveUtils.formatMillisecondsToConventional(time,false));
	}


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
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");

    String sAction = request.getParameter("action");
    if(sAction != null) {
        if(sAction.equalsIgnoreCase("logout")) {
            // Logging out.
            session = request.getSession();
            if (session != null) {
                session.invalidate();
                // Redirect back to here and we'll get thrown to the login
                // page.
                response.sendRedirect(request.getContextPath() + "/index.jsp"); 
            }
        }
    }

    String title = "Admin Console";
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
      out.write("\n    \n    ");
      out.write("<script type=\"text/javascript\">\n        function doTerminateCurrentJob(){\n            if(confirm(\"Are you sure you wish to terminate the job currently being crawled?\")){\n                document.location = '");
out.print(request.getContextPath());
      out.write("/console/action.jsp?action=terminate';\n            }\n        }    \n    ");
      out.write("</script>\n    \n    ");
      out.write("<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\">");
      out.write("<tr>");
      out.write("<td>\n    ");
      out.write("<fieldset style=\"width: 750px\">\n        ");
      out.write("<legend> \n        ");
      out.write("<b>");
      out.write("<span class=\"legendTitle\">Crawler Status:");
      out.write("</span> \n        ");
      out.print( handler.isRunning() 
            ? "<span class='status crawling'>CRAWLING JOBS</span></b> | "
              +"<a href='"+request.getContextPath()+"/console/action.jsp?action=stop'>Hold</a>"
            : "<span class='status holding'>HOLDING JOBS</span></b> | "
              +"<a href='"+request.getContextPath()+"/console/action.jsp?action=start'>Start</a>"
        );
      out.write(" ");
      out.write("</b>\n        ");
      out.write("</legend>\n        ");
      out.write("<div style=\"float:right;padding-right:50px;\">\n\t        ");
      out.write("<b>Memory");
      out.write("</b>");
      out.write("<br>\n\t        ");
      out.write("<div style=\"padding-left:20px\">\n\t\t        ");
      out.print((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024);
      out.write(" KB \n\t\t        used");
      out.write("<br>\n\t\t        ");
      out.print((Runtime.getRuntime().totalMemory())/1024);
      out.write(" KB\n\t\t        current heap");
      out.write("<br>\n\t\t        ");
      out.print((Runtime.getRuntime().maxMemory())/1024);
      out.write(" KB\n\t\t        max heap\n\t        ");
      out.write("</div>\n\t    ");
      out.write("</div>\n        ");
      out.write("<b>Jobs");
      out.write("</b>\n        ");
      out.write("<div style=\"padding-left:20px\">\n\t\t\t");
      out.print( handler.getCurrentJob()!=null
			    ? shortJobStatus+": <i>"
			      +handler.getCurrentJob().getJobName()+"</i>"
			    : ((handler.isRunning()) ? "None available" : "None running")
			 );
      out.write("<br>\n\t        ");
      out.print( handler.getPendingJobs().size() );
      out.write(" pending,\n\t        ");
      out.print( handler.getCompletedJobs().size() );
      out.write(" completed\n        ");
      out.write("</div>\n\n        ");
      out.write("<b>Alerts:");
      out.write("</b>\n\t        ");
      out.write("<a style=\"color: #000000\" \n\t            href=\"");
      out.print(request.getContextPath());
      out.write("/console/alerts.jsp\">\n\t            ");
      out.print(heritrix.getAlertsCount());
      out.write(" (");
      out.print(heritrix.getNewAlertsCount());
      out.write(" new)\n\t        ");
      out.write("</a>\n\t        \n         ");
      out.write("</fieldset>\n            ");

            	long begin, end;
	            if(stats != null) {
	                begin = stats.successfullyFetchedCount();
	                end = stats.totalCount();
	                if(end < 1) {
	                    end = 1;
	                }
	            } else {
                    begin = 0;
                    end = 1;
	            }
                
                if(handler.getCurrentJob() != null)
                {
                    final long timeElapsed, timeRemain;
                    if(stats == null) {
                        timeElapsed= 0;
                        timeRemain = -1;
                    } else {
	                    timeElapsed = (stats.getCrawlerTotalElapsedTime());
	                    if(begin == 0) {
	                        timeRemain = -1;
	                    } else {
	                        timeRemain = ((long)(timeElapsed*end/(double)begin))-timeElapsed;
	                    }
                    }
            
      out.write("\n            ");
      out.write("<fieldset style=\"width: 750px\">\n               ");
      out.write("<legend>\n               ");
      out.write("<b>");
      out.write("<span class=\"legendTitle\">Job Status:");
      out.write("</span>\n               ");
      out.print( 
               "<span class='status "
               +shortJobStatus+"'>"
               +shortJobStatus+"</span>"
               );
      out.write("\n               ");
      out.write("</b> \n");
      
    if(handler.isCrawling()) {
	    if ((handler.getCurrentJob().getStatus().
                equals(CrawlJob.STATUS_PAUSED) ||
            handler.getCurrentJob().getStatus().
			    equals(CrawlJob.STATUS_WAITING_FOR_PAUSE))) {
            out.println("| <a href='/console/action.jsp?action=resume'>" +
                "Resume</a>");
            out.println(" | ");
            out.println("<a href=\"");
            out.println(request.getContextPath());
            out.println("/console/action.jsp?action=checkpoint\">" +
                "Checkpoint</a>");
        } else if (!handler.getCurrentJob().isCheckpointing()) {
            out.println("| <a href=\"");
            out.println(request.getContextPath());
            out.println("/console/action.jsp?action=pause\">Pause</a> ");
            if (!handler.getCurrentJob().getStatus().
                   equals(CrawlJob.STATUS_PENDING)) {
                out.println(" | ");
                out.println("<a href=\"");
                out.println(request.getContextPath());
                out.println("/console/action.jsp?action=checkpoint\">" +
                    "Checkpoint</a>");
            }
        }
        out.println(" | <a href='javascript:doTerminateCurrentJob()'>" +
            "Terminate</a>");
    }

      out.write("\n               ");
      out.write("</legend>\n\n                ");

                  if(handler.isCrawling() && stats != null)
                  {
                
      out.write("\n                \t");
      out.write("<div style=\"float:right; padding-right:50px;\">\n                \t    ");
      out.write("<b>Load");
      out.write("</b>\n            \t\t\t");
      out.write("<div style=\"padding-left:20px\">\n\t\t\t            \t");
      out.print(stats.activeThreadCount());
      out.write(" active of ");
      out.print(stats.threadCount());
      out.write(" threads\n\t\t\t            \t");
      out.write("<br>\n\t\t\t            \t");
      out.print(ArchiveUtils.doubleToString((double)stats.congestionRatio(),2));
      out.write("\n\t\t\t            \tcongestion ratio\n\t\t\t            \t");
      out.write("<br>\n\t\t\t            \t");
      out.print(stats.deepestUri());
      out.write(" deepest queue\n\t\t\t            \t");
      out.write("<br>\n\t\t\t            \t");
      out.print(stats.averageDepth());
      out.write(" average depth\n\t\t\t\t\t\t");
      out.write("</div>\n\t\t\t\t\t");
      out.write("</div>\n\t                ");
      out.write("<b>Rates");
      out.write("</b>\n\t                ");
      out.write("<div style=\"padding-left:20px\">\n\t\t                ");
      out.print(ArchiveUtils.doubleToString(stats.currentProcessedDocsPerSec(),2));
      out.write(" \t\t                \n\t\t                URIs/sec\n\t\t                (");
      out.print(ArchiveUtils.doubleToString(stats.processedDocsPerSec(),2));
      out.write(" avg)\n\t\t                ");
      out.write("<br>\n\t\t                ");
      out.print(stats.currentProcessedKBPerSec());
      out.write("\n\t\t\t\t\t\tKB/sec\n\t\t\t\t\t\t(");
      out.print(stats.processedKBPerSec());
      out.write(" avg)\n\t\t\t\t\t");
      out.write("</div>\n\n                    ");
      out.write("<b>Time");
      out.write("</b>\n                    ");
      out.write("<div class='indent'>\n\t                    ");
      out.print( ArchiveUtils.formatMillisecondsToConventional(timeElapsed,false) );
      out.write("\n\t\t\t\t\t\telapsed\n\t\t\t\t\t\t");
      out.write("<br>\n\t                    ");

	                       if(timeRemain != -1) {
	                    
      out.write("\n\t\t                    ");
      out.print( ArchiveUtils.formatMillisecondsToConventional(timeRemain,false) );
      out.write("\n\t\t                    remaining (estimated)\n\t\t               \t");

	                       }
                   		
      out.write("\n\t\t\t\t\t");
      out.write("</div>\n                    ");
      out.write("<b>Totals");
      out.write("</b>\n                \t");

                          }
                }
                if(stats != null)
                {
	                int ratio = (int) (100 * begin / end);
            
      out.write("\n                            ");
      out.write("<center>\n                            ");
      out.write("<table border=\"0\" cellpadding=\"0\" cellspacing= \"0\" width=\"600\"> \n                                ");
      out.write("<tr>\n                                    ");
      out.write("<td align='right' width=\"25%\">downloaded ");
      out.print( begin );
      out.write("&nbsp;");
      out.write("</td>\n                                    ");
      out.write("<td class='completedBar' width=\"");
      out.print( (int)ratio/2 );
      out.write("%\" align=\"right\">\n                                    ");
      out.print( ratio > 50 ? "<b>"+ratio+"</b>%&nbsp;" : "" );
      out.write("\n                                    ");
      out.write("</td>\n                                    ");
      out.write("<td class='queuedBar' align=\"left\" width=\"");
      out.print( (int) ((100-ratio)/2) );
      out.write("%\">\n                                    ");
      out.print( ratio <= 50 ? "&nbsp;<b>"+ratio+"</b>%" : "" );
      out.write("\n                                    ");
      out.write("</td>\n                                    ");
      out.write("<td width=\"25%\" nowrap>&nbsp;");
      out.print( stats.queuedUriCount() );
      out.write(" queued");
      out.write("</td>\n                                ");
      out.write("</tr>\n                            ");
      out.write("</table>\n                            ");
      out.print( end );
      out.write(" total downloaded and queued");
      out.write("<br>      \n                    \t\t");
      out.print(stats.crawledBytesSummary());
      out.write("\n                            ");
      out.write("</center>\n            ");

                }
                if (handler.getCurrentJob() != null &&
                	handler.getCurrentJob().getStatus().equals(CrawlJob.STATUS_PAUSED)) {
            
      out.write("\n            \t\t");
      out.write("<b>Paused Operations");
      out.write("</b>\n            \t\t");
      out.write("<div class='indent'>\n\t                \t");
      out.write("<a href='");
      out.print( request.getContextPath() );
      out.write("/console/frontier.jsp'>View or Edit Frontier URIs");
      out.write("</a>\n\t                ");
      out.write("</div>\n\t        ");

            	}
            
      out.write("\n    ");
      out.write("</fieldset>\n    ");
      out.write("</td>");
      out.write("</tr>\n    ");
      out.write("<tr>");
      out.write("<td>\n    \n\t");
      out.write("<a href=\"");
      out.print(request.getContextPath());
      out.write("/\">Refresh");
      out.write("</a>\n    ");
      out.write("</td>");
      out.write("</tr>\n    ");
      out.write("<tr>");
      out.write("<td>\n        ");
      out.write("<p>\n            &nbsp;\n        ");
      out.write("<p>\n            &nbsp;\n    ");
      out.write("</td>");
      out.write("</tr>\n    ");
      out.write("<tr>");
      out.write("<td>\n        ");
 if (heritrix.isCommandLine()) {  
            // Print the shutdown only if we were started from command line.
            // It makes no sense when in webcontainer mode.
         
      out.write("\n        ");
      out.write("<a href=\"");
      out.print(request.getContextPath());
      out.write("/console/shutdown.jsp\">Shut down Heritrix software");
      out.write("</a> |\n        ");
 } 
      out.write("\n        ");
      out.write("<a href=\"");
      out.print(request.getContextPath());
      out.write("/index.jsp?action=logout\">Logout");
      out.write("</a>\n    ");
      out.write("</td>");
      out.write("</tr>");
      out.write("</table>\n");

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
