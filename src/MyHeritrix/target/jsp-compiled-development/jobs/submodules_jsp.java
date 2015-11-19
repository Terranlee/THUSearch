package org.apache.jsp;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import org.apache.jasper.runtime.*;
import org.archive.crawler.admin.CrawlJobHandler;
import org.archive.crawler.admin.CrawlJob;
import org.archive.crawler.Heritrix;
import org.archive.crawler.framework.CrawlController;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.io.IOException;
import org.archive.util.TextUtils;
import org.archive.crawler.settings.ModuleAttributeInfo;
import org.archive.crawler.settings.ModuleAttributeInfo;
import org.archive.crawler.settings.ModuleType;
import org.archive.crawler.settings.ComplexType;
import org.archive.crawler.settings.CrawlerSettings;
import org.archive.crawler.settings.MapType;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.framework.CrawlScope;
import org.archive.crawler.framework.Frontier;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import org.archive.crawler.admin.ui.JobConfigureUtils;
import org.archive.crawler.admin.CrawlJob;
import org.archive.crawler.url.canonicalize.BaseRule;
import java.text.SimpleDateFormat;
import org.archive.crawler.Heritrix;
import org.archive.crawler.admin.CrawlJob;
import org.archive.util.ArchiveUtils;
import org.archive.util.TextUtils;
import org.archive.crawler.admin.StatisticsTracker;
import org.archive.crawler.Heritrix;
import java.util.Map;
import java.util.Iterator;
import org.archive.crawler.admin.CrawlJob;

public class submodules_jsp extends HttpJspBase {


    /**
     * Generates the HTML code to display and allow manipulation of all
     * MapTypes which include ModuleTypes with multiple options (except
     * Processors). 
     *
     * Will work it's way recursively down the crawlorder.
     *
     * @param mbean The ComplexType representing the crawl order or one
     * of it's subcomponents.
     * @param inMap
     * @param parent The absolute name of the ComplexType that contains the
     * current ComplexType (i.e. parent).
     * @return The variable part of the HTML code for selecting filters.
     * @throws Exception
     */
    public static String printAllMaps(ComplexType mbean,
            CrawlerSettings settings, boolean inMap,
            boolean edittable, String parent) throws Exception {
        if (mbean.isTransient()) {
            return "";
        }
        MBeanInfo info = mbean.getMBeanInfo(settings);
        MBeanAttributeInfo a[] = info.getAttributes();
        StringBuffer p = new StringBuffer();

        boolean subMap = false;
        boolean processorsMap = false;
        MapType thisMap = null;
        List availableOptions = Collections.EMPTY_LIST;
        if (mbean instanceof MapType) {
            thisMap = (MapType)mbean;
            if (thisMap.getContentType() != Processor.class) {
                // only if a maptype, with moduletype entries, with
                // multiple options, and not Processor, will this
                // get map treatment
                subMap = true;
            } else {
                processorsMap = true;
            }
            if (ModuleType.class.isAssignableFrom(thisMap.getContentType())) {
                availableOptions = getOptionsForType(thisMap.getContentType());
            }
            if (availableOptions.size() == 0) {
                subMap = false;
            }
        }

        String description = TextUtils.escapeForMarkupAttribute(mbean
                .getDescription());
        if (inMap) {
            p.append("<tr><td>" + mbean.getName() + "</td>");
            if(edittable) {
	            p.append("<td nowrap><a href=\"javascript:doMoveUp('"
	                    + mbean.getName() + "','" + parent
	                    + "')\">Up</a></td>");
	            p.append("<td nowrap><a href=\"javascript:doMoveDown('"
	                    + mbean.getName() + "','" + parent
	                    + "')\">Down</a></td> ");
	            p.append("<td><a href=\"javascript:doRemove('" + mbean.getName()
	                    + "','" + parent + "')\">Remove</a></td>");
	        } else {
	            p.append("<td colspan=\"3\">(inherited)</td>");
	        }
            p.append("<td title='" + description + "'>");
            p.append("<i><font size=\"-1\">" + mbean.getClass().getName() +
                    "</font></i>");
            p.append("&nbsp;<a href='javascript:alert(\"" + description
                    + "\")'>?</a>");
            p.append("</td></tr>\n");
        } else {
            p.append("<div class='SettingsBlock'>\n");
            p.append("<b title='" + description + "'>" + mbean.getName());
            p.append("</b>\n");
            Class type = mbean.getLegalValueType();
            if (CrawlScope.class.isAssignableFrom(type)
                    || Frontier.class.isAssignableFrom(type) || processorsMap) {
                p.append("<font size=\"-1\">" + description + "To change "
                        + mbean.getName() + ", go to the "
                        + "<i>Modules</i> tab.</font>");
            }
            p.append("<br/>\n");
        }

        if (subMap) {
            p.append("<table>\n");
        }

        for (int n = 0; n < a.length; n++) {
            if (a[n] == null) {
                p.append("  ERROR: null attribute");
            } else {
                Object currentAttribute = null;
                Object localAttribute = null;
                //The attributes of the current attribute.
                ModuleAttributeInfo att = (ModuleAttributeInfo)a[n];
                try {
                    currentAttribute = mbean.getAttribute(settings, att.getName());
                    localAttribute = mbean.getLocalAttribute(settings, att.getName());
                } catch (Exception e1) {
                    String error = e1.toString() + " " + e1.getMessage();
                    return error;
                }
                boolean locallyEdittable = (localAttribute != null);
                if (currentAttribute instanceof ComplexType) {
                    if (inMap) {
                        p.append("<tr><td colspan='5'>");
                    }
                    p.append(printAllMaps((ComplexType)currentAttribute,
                            settings, subMap, locallyEdittable,
                            mbean.getAbsoluteName()));
                    if (inMap) {
                        p.append("</td></tr>\n");
                    }
                }
            }
        }
        if (subMap) {
            p.append("</table>\n");
        }

        if (subMap) {
            // ordered list of options; append add controls
            if (availableOptions != null) {
                p.append("Name: <input size='8' name='"
                        + mbean.getAbsoluteName() + ".name' id='"
                        + mbean.getAbsoluteName() + ".name'>\n");
                p.append("Type: <select name='" + mbean.getAbsoluteName()
                        + ".class'>\n");
                for (int i = 0; i < availableOptions.size(); i++) {
                    p.append("<option value='" + availableOptions.get(i) + "'>"
                            + availableOptions.get(i) + "</option>\n");
                }
                p.append("</select>\n");
                p.append("<input type='button' value='Add'"
                        + " onClick=\"doAdd('" + mbean.getAbsoluteName()
                        + "')\">\n");
                p.append("<br/>");
            }
        }

        if (!inMap) {
            p.append("\n</div>\n");
        }
        return p.toString();
    }
    
    private static List getOptionsForType(Class type) {
        String typeName = type.getName();
        String simpleName = typeName.substring(typeName.lastIndexOf(".")+1);
        String optionsFilename = simpleName+".options";
        try {
            return CrawlJobHandler.loadOptions(optionsFilename);
        } catch (IOException e) {
            return new ArrayList();
        }
    }

    /**
     * Builds the HTML for selecting an implementation of a specific crawler module
     *
     * MOVED FROM webapps/admin/jobs/modules.jsp
     * @param module The MBeanAttributeInfo on the currently set module
     * @param availibleOptions A list of the availibe implementations (full class names as Strings)
     * @param name The name of the module
     *
     * @return the HTML for selecting an implementation of a specific crawler module
     */
    public static String buildModuleSetter(MBeanAttributeInfo module, Class allowableType, String name, String currentDescription){
        StringBuffer ret = new StringBuffer();

        List availableOptions = getOptionsForType(allowableType);

        ret.append("<table><tr><td>&nbsp;Current selection:</td><td>");
        ret.append(module.getType());
        ret.append("</td><td></td></tr>");
        ret.append("<tr><td></td><td width='100' colspan='2'><i>" + currentDescription + "</i></td>");

        if(availableOptions.size()>0){
            ret.append("<tr><td>&nbsp;Available alternatives:</td><td>");
            ret.append("<select name='cbo" + name + "'>");
            for(int i=0 ; i<availableOptions.size() ; i++){
                ret.append("<option value='"+availableOptions.get(i)+"'>");
                ret.append(availableOptions.get(i)+"</option>");
            }
            ret.append("</select>");
            ret.append("</td><td>");
            ret.append("<input type='button' value='Change' onClick='doSetModule(\"" + name + "\")'>");
            ret.append("</td></tr>");
        }
        ret.append("</table>");
        return ret.toString();
    }

    /**
     *
     * Builds the HTML to edit a map of modules
     *
     * MOVED FROM webapps/admin/jobs/modules.jsp
     * @param map The map to edit
     * @param allowableType
     * @param name A short name for the map (only alphanumeric chars.)
     *
     * @return the HTML to edit the specified modules map
     */
    public static String buildModuleMap(ComplexType map,
            Class allowableType, String name){
        StringBuffer ret = new StringBuffer();

        List availableOptions = getOptionsForType(allowableType);

        ret.append("<table cellspacing='0' cellpadding='2'>");

        ArrayList unusedOptions = new ArrayList();
        MBeanInfo mapInfo = map.getMBeanInfo();
        MBeanAttributeInfo m[] = mapInfo.getAttributes();

        // Printout modules in map.
        boolean alt = false;
        for(int n=0; n<m.length; n++) {
            ModuleAttributeInfo att = (ModuleAttributeInfo)m[n]; //The attributes of the current attribute

            ret.append("<tr");
            if(alt){
                ret.append(" bgcolor='#EEEEFF'");
            }
            ret.append("><td>&nbsp;"+att.getType()+"</td>");
            if(n!=0){
                ret.append("<td><a href=\"javascript:doMoveMapItemUp('" + name + "','"+att.getName()+"')\">Up</a></td>");
            } else {
                ret.append("<td></td>");
            }
            if(n!=m.length-1){
                ret.append("<td><a href=\"javascript:doMoveMapItemDown('" + name + "','"+att.getName()+"')\">Down</a></td>");
            } else {
                ret.append("<td></td>");
            }
            ret.append("<td><a href=\"javascript:doRemoveMapItem('" + name + "','"+att.getName()+"')\">Remove</a></td>");
            ret.append("<td><a href=\"javascript:alert('");
            ret.append(TextUtils.escapeForMarkupAttribute(att.getDescription()));
            ret.append("')\">Info</a></td>\n");
            ret.append("</tr>");
            alt = !alt;
        }

        // Find out which aren't being used.
        for(int i=0 ; i<availableOptions.size() ; i++){
            boolean isIncluded = false;

            for(int n=0; n<m.length; n++) {
                ModuleAttributeInfo att = (ModuleAttributeInfo)m[n]; //The attributes of the current attribute.

                try {
                    map.getAttribute(att.getName());
                } catch (Exception e1) {
                    ret.append(e1.toString() + " " + e1.getMessage());
                }
                String typeAndName = att.getType()+"|"+att.getName();
                if(typeAndName.equals(availableOptions.get(i))){
                    //Found it
                    isIncluded = true;
                    break;
                }
            }
            if(isIncluded == false){
                // Yep the current one is unused.
                unusedOptions.add(availableOptions.get(i));
            }
        }
        if(unusedOptions.size() > 0 ){
            ret.append("<tr><td>");
            ret.append("<select name='cboAdd" + name + "'>");
            for(int i=0 ; i<unusedOptions.size() ; i++){
                String curr = (String)unusedOptions.get(i);
                int index = curr.indexOf("|");
                if (index < 0) {
                    throw new RuntimeException("Failed to find '|' required" +
                        " divider in : " + curr + ". Repair modules file.");

                }
                ret.append("<option value='" + curr + "'>" +
                    curr.substring(0, index) + "</option>");
            }
            ret.append("</select>");
            ret.append("</td><td>");
            ret.append("<input type='button' value='Add' onClick=\"doAddMapItem('" + name + "')\">");
            ret.append("</td></tr>");
        }
        ret.append("</table>");
        return ret.toString();
    }


  private static java.util.Vector _jspx_includes;

  static {
    _jspx_includes = new java.util.Vector(8);
    _jspx_includes.add("/include/handler.jsp");
    _jspx_includes.add("/include/modules.jsp");
    _jspx_includes.add("/include/head.jsp");
    _jspx_includes.add("/include/stats.jsp");
    _jspx_includes.add("/include/filters_js.jsp");
    _jspx_includes.add("/include/jobnav.jsp");
    _jspx_includes.add("/include/jobnav.jsp");
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


  /**
   * This pages allows the user to select all submodules which appear
   * in collections inside other modules 
   *
   * @author Kristinn Sigurdsson
   */

      out.write("\n");
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
      out.write("\n\n");
      out.write("\n");
      out.write("\n");
      out.write("\n\n");

    CrawlJob theJob = JobConfigureUtils.handleJobAction(handler, request,
            response, request.getContextPath() + "/jobs.jsp", null, null);
    int tab = theJob.isProfile()?2:1;

      out.write("\n\n");

    // Set page header.
    String title = "Submodules";
    int jobtab = 7;

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
      out.write("\n");
      out.write("\n");
      out.write("<script type=\"text/javascript\">\n    function doSubmit(){\n        document.frmFilters.submit();\n    }\n    \n    function doGoto(where){\n        document.frmFilters.action.value=\"goto\";\n        document.frmFilters.subaction.value = where;\n        doSubmit();\n    }\n    \n    function doMoveUp(filter,map){\n        document.frmFilters.action.value = \"filters\";\n        document.frmFilters.subaction.value = \"moveup\";\n        document.frmFilters.map.value = map;\n        document.frmFilters.filter.value = filter;\n        doSubmit();\n    }\n\n    function doMoveDown(filter,map){\n        document.frmFilters.action.value = \"filters\";\n        document.frmFilters.subaction.value = \"movedown\";\n        document.frmFilters.map.value = map;\n        document.frmFilters.filter.value = filter;\n        doSubmit();\n    }\n\n    function doRemove(filter,map){\n        document.frmFilters.action.value = \"filters\";\n        document.frmFilters.subaction.value = \"remove\";\n        document.frmFilters.map.value = map;\n        document.frmFilters.filter.value = filter;\n");
      out.write("        doSubmit();\n    }\n\n    function doAdd(map){\n        if(document.getElementById(map+\".name\").value == \"\"){\n            alert(\"Must enter a unique name for the subcomponent\");\n        } else {\n            document.frmFilters.action.value = \"filters\";\n            document.frmFilters.subaction.value = \"add\";\n            document.frmFilters.map.value = map;\n            doSubmit();\n        }\n    }\n");
      out.write("</script>\n");
      out.write("\n    ");
      out.write("<p>\n        ");

    /**
     * An include file that handles the sub navigation of a 'job' page. 
     * Include where the sub navigation should be displayed.
     *
     * The following variables must exist prior to this file being included:
     *
     * String theJob - The CrawlJob being manipulated.
     * int jobtab - Which to display as 'selected'.
     *          0 - Modules
     *          SUPERCEDED BY SUBMODULES 1 - Filters
     *          2 - Settings
     *          3 - Overrides
     *          SUPERCEDED BY SUBMODULES 4 - Credentials
     *          5 - Refinements
     *          SUPERCEDED BY SUBMODULES 6 - URL (Canonicalization)
     *          7 - Submodules 
     *
     * @author Kristinn Sigurdsson
     */

      out.write("\n    ");
      out.write("<table cellspacing=\"0\" cellpadding=\"0\">\n        ");
      out.write("<tr>\n            ");
      out.write("<td bgcolor=\"#0000FF\" height=\"1\">\n            ");
      out.write("</td>\n        ");
      out.write("</tr>\n        ");
      out.write("<tr>\n            ");
      out.write("<td>\n                ");
      out.write("<table cellspacing=\"0\" cellpadding=\"0\">\n                    ");
      out.write("<tr>\n                        ");
      out.write("<td nowrap>\n                            ");
      out.write("<b>");
      out.print(theJob.isProfile()?"Profile":"Job");
      out.write(" ");
      out.print(theJob.getJobName());
      out.write(":");
      out.write("</b>\n                        ");
      out.write("</td>\n                        ");
      out.write("<td class=\"tab_seperator\">\n                        ");
      out.write("</td>\n                        ");
 if(theJob.isRunning()){ 
      out.write("\n                            ");
      out.write("<td class=\"tab_inactive\" nowrap>\n                                ");
      out.write("<a href=\"javascript:alert('Can not edit modules on running jobs!')\" class=\"tab_text_inactive\">Modules");
      out.write("</a>\n                            ");
      out.write("</td>\n                        ");
 } else { 
      out.write("\n                            ");
      out.write("<td class=\"tab");
      out.print(jobtab==0?"_selected":"");
      out.write("\" nowrap>\n                                ");
      out.write("<a href=\"javascript:doGoto('");
      out.print(request.getContextPath());
      out.write("/jobs/modules.jsp?job=");
      out.print(theJob.getUID());
      out.write("')\" class=\"tab_text");
      out.print(jobtab==0?"_selected":"");
      out.write("\">Modules");
      out.write("</a>\n                            ");
      out.write("</td>\n                        ");
 } 
      out.write("\n                        ");
      out.write("<td class=\"tab_seperator\">\n                        ");
      out.write("</td>\n                        ");
      out.write("<td class=\"tab");
      out.print(jobtab==7?"_selected":"");
      out.write("\" nowrap>\n                            ");
      out.write("<a href=\"javascript:doGoto('");
      out.print(request.getContextPath());
      out.write("/jobs/submodules.jsp?job=");
      out.print(theJob.getUID());
      out.write("')\" class=\"tab_text");
      out.print(jobtab==7?"_selected":"");
      out.write("\">Submodules");
      out.write("</a>\n                        ");
      out.write("<td class=\"tab_seperator\">\n                        ");
      out.write("</td>\n                        ");
      out.write("<td class=\"tab");
      out.print(jobtab==2?"_selected":"");
      out.write("\" nowrap>\n                            ");
      out.write("<a href=\"javascript:doGoto('");
      out.print(request.getContextPath());
      out.write("/jobs/configure.jsp?job=");
      out.print(theJob.getUID());
      out.write("')\" class=\"tab_text");
      out.print(jobtab==2?"_selected":"");
      out.write("\">Settings");
      out.write("</a>\n                        ");
      out.write("</td>\n                        ");
      out.write("<td class=\"tab_seperator\">\n                        ");
      out.write("</td>\n                        ");
      out.write("<td class=\"tab");
      out.print(jobtab==3?"_selected":"");
      out.write("\" nowrap>\n                            ");
      out.write("<a href=\"javascript:doGoto('");
      out.print(request.getContextPath());
      out.write("/jobs/per/overview.jsp?job=");
      out.print(theJob.getUID());
      out.write("')\" class=\"tab_text");
      out.print(jobtab==3?"_selected":"");
      out.write("\">Overrides");
      out.write("</a>\n                        ");
      out.write("</td>\n                        ");
      out.write("<td class=\"tab_seperator\">\n                        ");
      out.write("</td>\n                        ");
      out.write("<td class=\"tab");
      out.print(jobtab==5?"_selected":"");
      out.write("\" nowrap>\n                            ");
      out.write("<a href=\"javascript:doGoto('");
      out.print(request.getContextPath());
      out.write("/jobs/refinements/overview.jsp?job=");
      out.print(theJob.getUID());
      out.write("')\" class=\"tab_text");
      out.print(jobtab==5?"_selected":"");
      out.write("\">Refinements");
      out.write("</a>\n                        ");
      out.write("</td>\n                        ");
      out.write("<td class=\"tab_seperator\">\n                        ");
      out.write("</td>\n                        ");
      out.write("<td class=\"tab\">\n                            ");
      out.write("<a href=\"javascript:doSubmit()\" class=\"tab_text\">");
      out.print(theJob.isNew()?"Submit job":"Finished");
      out.write("</a>\n                        ");
      out.write("</td>\n                        ");
      out.write("<td class=\"tab_seperator\">\n                        ");
      out.write("</td>\n                    ");
      out.write("</tr>\n                ");
      out.write("</table>\n            ");
      out.write("</td>\n        ");
      out.write("</tr>\n        ");
      out.write("<tr>\n            ");
      out.write("<td bgcolor=\"#0000FF\" height=\"1\">\n            ");
      out.write("</td>\n        ");
      out.write("</tr>\n    ");
      out.write("</table>\n");
      out.write("\n    ");
      out.write("<p>\n        ");
      out.write("<p>\n            ");
      out.write("<b>Add/Remove/Order Submodules");
      out.write("</b>\n        ");
      out.write("<p>\n        ");
      out.write("<p>Use this page to add/remove/order submodules. Go to the\n        ");
      out.write("<a href=\"javascript:doGoto('");
      out.print(request.getContextPath());
      out.write("/jobs/configure.jsp?job=");
      out.print(theJob.getUID());
      out.write("')\">Settings");
      out.write("</a>\n        page to complete configuration of added submodules (e.g. To\n        add the particular regex to an added canonicalization RegexRule\n        or to fill in the authentication information into an added\n        RFC2617 credential).");
      out.write("</p>\n\n    ");
      out.write("<form name=\"frmFilters\" method=\"post\" \n            action=\"submodules.jsp\">\n        ");
      out.write("<input type=\"hidden\" name=\"job\" value=\"");
      out.print(theJob.getUID());
      out.write("\">\n        ");
      out.write("<input type=\"hidden\" name=\"action\" value=\"done\">\n        ");
      out.write("<input type=\"hidden\" name=\"subaction\" value=\"\">\n        ");
      out.write("<input type=\"hidden\" name=\"map\" value=\"\">\n        ");
      out.write("<input type=\"hidden\" name=\"filter\" value=\"\">\n        ");
      out.print(printAllMaps(theJob.getSettingsHandler().getOrder(), null, false, true, null));
      out.write("\n    ");
      out.write("</form>\n    ");
      out.write("<p>\n");

    /**
     * An include file that handles the sub navigation of a 'job' page. 
     * Include where the sub navigation should be displayed.
     *
     * The following variables must exist prior to this file being included:
     *
     * String theJob - The CrawlJob being manipulated.
     * int jobtab - Which to display as 'selected'.
     *          0 - Modules
     *          SUPERCEDED BY SUBMODULES 1 - Filters
     *          2 - Settings
     *          3 - Overrides
     *          SUPERCEDED BY SUBMODULES 4 - Credentials
     *          5 - Refinements
     *          SUPERCEDED BY SUBMODULES 6 - URL (Canonicalization)
     *          7 - Submodules 
     *
     * @author Kristinn Sigurdsson
     */

      out.write("\n    ");
      out.write("<table cellspacing=\"0\" cellpadding=\"0\">\n        ");
      out.write("<tr>\n            ");
      out.write("<td bgcolor=\"#0000FF\" height=\"1\">\n            ");
      out.write("</td>\n        ");
      out.write("</tr>\n        ");
      out.write("<tr>\n            ");
      out.write("<td>\n                ");
      out.write("<table cellspacing=\"0\" cellpadding=\"0\">\n                    ");
      out.write("<tr>\n                        ");
      out.write("<td nowrap>\n                            ");
      out.write("<b>");
      out.print(theJob.isProfile()?"Profile":"Job");
      out.write(" ");
      out.print(theJob.getJobName());
      out.write(":");
      out.write("</b>\n                        ");
      out.write("</td>\n                        ");
      out.write("<td class=\"tab_seperator\">\n                        ");
      out.write("</td>\n                        ");
 if(theJob.isRunning()){ 
      out.write("\n                            ");
      out.write("<td class=\"tab_inactive\" nowrap>\n                                ");
      out.write("<a href=\"javascript:alert('Can not edit modules on running jobs!')\" class=\"tab_text_inactive\">Modules");
      out.write("</a>\n                            ");
      out.write("</td>\n                        ");
 } else { 
      out.write("\n                            ");
      out.write("<td class=\"tab");
      out.print(jobtab==0?"_selected":"");
      out.write("\" nowrap>\n                                ");
      out.write("<a href=\"javascript:doGoto('");
      out.print(request.getContextPath());
      out.write("/jobs/modules.jsp?job=");
      out.print(theJob.getUID());
      out.write("')\" class=\"tab_text");
      out.print(jobtab==0?"_selected":"");
      out.write("\">Modules");
      out.write("</a>\n                            ");
      out.write("</td>\n                        ");
 } 
      out.write("\n                        ");
      out.write("<td class=\"tab_seperator\">\n                        ");
      out.write("</td>\n                        ");
      out.write("<td class=\"tab");
      out.print(jobtab==7?"_selected":"");
      out.write("\" nowrap>\n                            ");
      out.write("<a href=\"javascript:doGoto('");
      out.print(request.getContextPath());
      out.write("/jobs/submodules.jsp?job=");
      out.print(theJob.getUID());
      out.write("')\" class=\"tab_text");
      out.print(jobtab==7?"_selected":"");
      out.write("\">Submodules");
      out.write("</a>\n                        ");
      out.write("<td class=\"tab_seperator\">\n                        ");
      out.write("</td>\n                        ");
      out.write("<td class=\"tab");
      out.print(jobtab==2?"_selected":"");
      out.write("\" nowrap>\n                            ");
      out.write("<a href=\"javascript:doGoto('");
      out.print(request.getContextPath());
      out.write("/jobs/configure.jsp?job=");
      out.print(theJob.getUID());
      out.write("')\" class=\"tab_text");
      out.print(jobtab==2?"_selected":"");
      out.write("\">Settings");
      out.write("</a>\n                        ");
      out.write("</td>\n                        ");
      out.write("<td class=\"tab_seperator\">\n                        ");
      out.write("</td>\n                        ");
      out.write("<td class=\"tab");
      out.print(jobtab==3?"_selected":"");
      out.write("\" nowrap>\n                            ");
      out.write("<a href=\"javascript:doGoto('");
      out.print(request.getContextPath());
      out.write("/jobs/per/overview.jsp?job=");
      out.print(theJob.getUID());
      out.write("')\" class=\"tab_text");
      out.print(jobtab==3?"_selected":"");
      out.write("\">Overrides");
      out.write("</a>\n                        ");
      out.write("</td>\n                        ");
      out.write("<td class=\"tab_seperator\">\n                        ");
      out.write("</td>\n                        ");
      out.write("<td class=\"tab");
      out.print(jobtab==5?"_selected":"");
      out.write("\" nowrap>\n                            ");
      out.write("<a href=\"javascript:doGoto('");
      out.print(request.getContextPath());
      out.write("/jobs/refinements/overview.jsp?job=");
      out.print(theJob.getUID());
      out.write("')\" class=\"tab_text");
      out.print(jobtab==5?"_selected":"");
      out.write("\">Refinements");
      out.write("</a>\n                        ");
      out.write("</td>\n                        ");
      out.write("<td class=\"tab_seperator\">\n                        ");
      out.write("</td>\n                        ");
      out.write("<td class=\"tab\">\n                            ");
      out.write("<a href=\"javascript:doSubmit()\" class=\"tab_text\">");
      out.print(theJob.isNew()?"Submit job":"Finished");
      out.write("</a>\n                        ");
      out.write("</td>\n                        ");
      out.write("<td class=\"tab_seperator\">\n                        ");
      out.write("</td>\n                    ");
      out.write("</tr>\n                ");
      out.write("</table>\n            ");
      out.write("</td>\n        ");
      out.write("</tr>\n        ");
      out.write("<tr>\n            ");
      out.write("<td bgcolor=\"#0000FF\" height=\"1\">\n            ");
      out.write("</td>\n        ");
      out.write("</tr>\n    ");
      out.write("</table>\n");
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
      out.write("\n\n\n");
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
