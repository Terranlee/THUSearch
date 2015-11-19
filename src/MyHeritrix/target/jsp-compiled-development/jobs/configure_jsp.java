package org.apache.jsp;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import org.apache.jasper.runtime.*;
import org.archive.crawler.admin.CrawlJobHandler;
import org.archive.crawler.admin.CrawlJob;
import org.archive.crawler.Heritrix;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.admin.CrawlJobErrorHandler;
import org.archive.crawler.admin.ui.CookieUtils;
import org.archive.crawler.settings.*;
import javax.management.MBeanInfo;
import javax.management.Attribute;
import javax.management.MBeanAttributeInfo;
import org.archive.util.TextUtils;
import java.util.regex.*;
import org.archive.crawler.admin.CrawlJob;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.admin.ui.JobConfigureUtils;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import org.archive.crawler.Heritrix;
import org.archive.crawler.admin.CrawlJob;
import org.archive.util.ArchiveUtils;
import org.archive.util.TextUtils;
import org.archive.crawler.admin.StatisticsTracker;
import org.archive.crawler.Heritrix;
import java.util.Map;
import java.util.Iterator;

public class configure_jsp extends HttpJspBase {


    /**
     * This include page contains methods used by the job configuration pages,
     * global, override and refinements.
     * 
     * @author Kristinn Sigurdsson
     */
     
    /**
     * Builds up the the HTML code to display any ComplexType attribute
     * of the settings in an editable form. Uses recursion.
     *
     * Javascript methods presumed to exist:
     *   setUpdated() - Noting that something has been changed.
     *   setEdited(name) - Noting that the 'name' attribute has been edited
     *      @param name absolute name of the attribute
     *   doPop(text) - Displays text in a pop-up dialog of some sort.
     *      @param text the text that will be displayed.
     *   doDeleteList(name) - Delete selected from specified list. INCLUDED
     *      @param name the absolute name of the list attribute.
     *   doAddList(name) - Add an entry to a list INCLUDED
     *      @param name the absolute name of the list attribute to add to
     *                  name + ".add" will provide the element name of that
     *                  contains the new entry
     *   doAddMap(name) - Add to a simple typed map. INCLUDED
     *      @param name the absolute name of the map attribute to add to.
     *   doDeleteMap(name, key) - Delete  entry from a simple typed map INCLUDED
     *      @param name the absolute name of the map attribute to remove from
     *      @param key the key of the item in the map that is to be removed.
     *
     * Override checkboxes are named with their respective attributes 
     * absolute name + ".override". 
     *
     * @param mbean The ComplexType to build a display
     * @param settings CrawlerSettings for the domain to override setting
     *                 for. For global domain always use null (or else
     *                 the override checkboxes will be displayed.
     * @param indent A string that will be added in front to indent the
     *               current type.
     * @param lists All 'lists' encountered will have their name added   
     *              to this StringBuffer followed by a comma.
     * @param expert if true then expert settings will be included, else
     *               they will be hidden.
     * @param errorHandler the error handler for the current job
     * @returns The HTML code described above.
     */
    public String printMBean(ComplexType mbean, 
                             CrawlerSettings settings, 
                             String indent, 
                             StringBuffer lists, 
                             boolean expert,
                             CrawlJobErrorHandler errorHandler) 
                         throws Exception {
        if(mbean.isTransient()){
            return "";
        }
        String expertClass = expert ? "expertShow" : "expertHide";
        
        StringBuffer p = new StringBuffer();
        MBeanInfo info = mbean.getMBeanInfo(settings);
        MBeanAttributeInfo[] a = info.getAttributes();
        
        if( mbean instanceof MapType && a.length ==0 ){
            // Empty map, ignore it.
            return "";
        }
        
        String descriptionForAttribute = 
        	TextUtils.escapeForMarkupAttribute(mbean.getDescription());
        String descriptionForJs = 
        	TextUtils.escapeForHTMLJavascript(mbean.getDescription());
        p.append(mbean.isExpertSetting()?"<tr class='"+expertClass+"'>":"<tr>");
        p.append("<td title=\"" + descriptionForAttribute +"\">");
        p.append("<b>" + indent + mbean.getName() + "</b></td>\n");
        p.append("<td><a class='help' href=\"javascript:doPop('");
        p.append(descriptionForJs);
        p.append("')\">?</a>");
        p.append(checkError(mbean.getAbsoluteName(),errorHandler,settings));
        p.append("</td>");

        String shortDescription = mbean.getDescription();
        // Need to cut off everything after the first sentance.
        Pattern firstSentance = Pattern.compile("^[^\\.)]*\\.\\s");
        Matcher m = firstSentance.matcher(mbean.getDescription());
        if(m.find()){
            shortDescription = m.group(0);
        }

        
        p.append("<td title=\'"+ descriptionForAttribute + "\' colspan='" 
             + (settings==null?"2":"3") + "'><font size=\"-1\">" 
        	 + shortDescription + "</font></td></tr>\n");

        for(int n=0; n<a.length; n++) {
            if(a[n] == null) {
                p.append("  ERROR: null attribute");
            } else {
                Object currentAttribute = null;
                Object localAttribute = null;
                ModuleAttributeInfo att = (ModuleAttributeInfo)a[n]; //The attributes of the current attribute.

                if(att.isTransient()==false){
                    try {
                        currentAttribute = mbean.getAttribute(settings, att.getName());
                        localAttribute = mbean.getLocalAttribute(settings, att.getName());
                    } catch (Exception e1) {
                        String error = e1.toString() + " " + e1.getMessage();
                        return error;
                    }
    
                    // MapTypes that contain Strings, int or other Java primatives are 'simple maps' and while
                    // technically complex types we will treat them like simple types.
                    boolean simpleMap = currentAttribute instanceof MapType; 
                    if(simpleMap){
                        Class contentType = ((MapType)currentAttribute).getContentType();
                        simpleMap = contentType == String.class
                                    || contentType == Integer.class
                                    || contentType == Double.class
                                    || contentType == Float.class
                                    || contentType == Boolean.class;
                    }
                    if(currentAttribute instanceof ComplexType && simpleMap == false) {
                        // Recursive call for complex types (contain other nodes and leaves)
                        p.append(printMBean((ComplexType)currentAttribute,settings,indent+"&nbsp;&nbsp;",lists,expert,errorHandler));
                    } else {
                        String attAbsoluteName = mbean.getAbsoluteName() + "/" + att.getName();
                        Object[] legalValues = att.getLegalValues();
                        
                        descriptionForAttribute = 
        					TextUtils.escapeForMarkupAttribute(att.getDescription());
        				descriptionForJs = 
        					TextUtils.escapeForHTMLJavascript(att.getDescription());
        				p.append((att.isExpertSetting()||mbean.isExpertSetting())
        				           ?"<tr class='"+expertClass+"'>":"<tr>");
                        p.append("<td title=\"" + descriptionForAttribute +"\" valign='top'>");
                        p.append(indent + "&nbsp;&nbsp;" + att.getName() + ":&nbsp;</td>");
                        p.append("<td valign='top'><a class='help' href=\"javascript:doPop('");
                        p.append(descriptionForJs);
                        p.append("')\">?</a>&nbsp;");
                        p.append(checkError(attAbsoluteName,errorHandler,settings));
                        p.append("</td>");
                        
                        // Create override (if needed)
                        boolean allowEdit = true;
                        if ((att.isOverrideable() || localAttribute!=null) && settings != null) {
                            p.append("<td valign='top' width='1'><input name='" + attAbsoluteName + ".override' id='" + attAbsoluteName + ".override' value='true' onChange='setUpdate()'");
                            if(localAttribute != null){
                                p.append(" checked");
                            }
                            if(att.isOverrideable() == false && localAttribute != null){
                                p.append(" type='hidden'>");
                            } else {
                                p.append(" type='checkbox'>");
                            }
                            p.append("</td>\n");
                        } else if (settings != null){
                            allowEdit = false;
                        }

                        p.append("<td valign='top'>\n");
                        if (allowEdit) {
                            // Print out interface for simple types (leaves)
                            if(currentAttribute instanceof ListType){
                                // Some type of list.
                                ListType list = (ListType)currentAttribute;
                                p.append("<table border='0' cellspacing='0' cellpadding='0'>\n");
                                p.append("<tr><td><select multiple name='" + attAbsoluteName + "' id='" + attAbsoluteName + "' size='4' style='width: 440px'>\n");
                                for(int i=0 ; i<list.size() ; i++){
                                    p.append("<option value='" + list.get(i) +"'>"+list.get(i)+"</option>\n");
                                }
                                p.append("</select>");
                                p.append("</td>\n");
                                p.append("<td valign='top'><input type='button' value='Delete' onClick=\"doDeleteList('" + attAbsoluteName + "')\"></td></tr>\n");
                                p.append("<tr><td><input name='" + attAbsoluteName + ".add' id='" + attAbsoluteName + ".add' style='width: 440px'></td>\n");
                                p.append("<td><input type='button' value='Add' onClick=\"doAddList('" + attAbsoluteName + "')\"></td></tr>\n");
                                p.append("</table>\n");
            
                                lists.append("'"+attAbsoluteName+"',");
                            } else if(simpleMap) {
                                // Simple map
                                MapType map = (MapType)currentAttribute;
                                p.append("<table border='0' cellspacing='0' cellpadding='0'>\n");
                                
                                MBeanInfo mapInfo = map.getMBeanInfo(settings);
                                MBeanAttributeInfo mp[] = mapInfo.getAttributes();

                                // Printout modules in map.
                                boolean alt = true;
                                for(int n2=0; n2<mp.length; n2++) {
                                    ModuleAttributeInfo mapAtt = (ModuleAttributeInfo)mp[n2]; //The attributes of the current attribute.

                                    Object currentMapAttribute = null;
                                    Object localMapAttribute = null;
                        
                                    try {
                                        currentMapAttribute = map.getAttribute(settings,mapAtt.getName());
                                        localMapAttribute = map.getLocalAttribute(settings,mapAtt.getName());
                                    } catch (Exception e1) {
                                        p.append(e1.toString() + " " + e1.getMessage());
                                    }
                                    p.append("<tr " + (alt?"bgcolor='#EEEEFF'":"") + "><td>" + mapAtt.getName() + "</td><td>&nbsp;-&gt;&nbsp</td><td>" + currentMapAttribute + "</td><td>&nbsp;<a href=\"javascript:doDeleteMap('"+attAbsoluteName+"','"+mapAtt.getName()+"')\">Remove</a></td></tr>\n");
                                    alt = !alt;
                                }
                                p.append("<tr><td><input name='"+attAbsoluteName+".key' name='"+attAbsoluteName+".key'></td><td>&nbsp;->&nbsp;</td><td><input name='"+attAbsoluteName+".value' name='"+attAbsoluteName+".value'></td><td>&nbsp;<input type='button' value='Add' onClick=\"doAddMap('"+attAbsoluteName+"')\"></td></tr>");
                                p.append("</table>\n");
                            } else if(legalValues != null && legalValues.length > 0) {
                                //Have legal values. Build combobox.
                                p.append("<select name='" + attAbsoluteName + "' style='width: 440px' onChange=\"setEdited('" + attAbsoluteName + "')\">\n");
                                for(int i=0 ; i < legalValues.length ; i++){
                                    p.append("<option value='"+legalValues[i]+"'");
                                    if(currentAttribute.equals(legalValues[i])){
                                        p.append(" selected");
                                    }
                                    p.append(">"+legalValues[i]+"</option>\n");
                                }
                                p.append("</select>\n");
                            } else if (currentAttribute instanceof Boolean){
                                // Boolean value
                                p.append("<select name='" + attAbsoluteName + "' style='width: 440px' onChange=\"setEdited('" + attAbsoluteName + "')\">\n");
                                p.append("<option value='False'"+ (currentAttribute.equals(new Boolean(false))?" selected":"") +">False</option>\n");
                                p.append("<option value='True'"+ (currentAttribute.equals(new Boolean(true))?" selected":"") +">True</option>\n");
                                p.append("</select>\n");
                            } else if (currentAttribute instanceof TextField){
                                // Text area
                                p.append("<textarea name='" + attAbsoluteName + "' style='width: 440px' rows='4' onChange=\"setEdited('" + attAbsoluteName + "')\">");
                                p.append(currentAttribute + "\n");
                                p.append("</textarea>\n");
                            } else {
                                //Input box
                                String patchedAttribute = currentAttribute.toString().replaceAll("&","&amp;");
                                p.append("<input name='" + attAbsoluteName + "' value='" + patchedAttribute + "' style='width: 440px' onChange=\"setEdited('" + attAbsoluteName + "')\">\n");
                            }
                        } else {
                            // Display non editable
                            if(currentAttribute instanceof ListType){
                                // Print list
                                ListType list = (ListType)currentAttribute;
                                p.append("</td><td colspan='" + (settings==null?"1":"2") + "'>");
                                for(int i=0 ; i<list.size() ; i++){
                                    p.append(list.get(i)+"<br>\n");
                                }
                            } else if(simpleMap) {
                                // Simple map
                                MapType map = (MapType)currentAttribute;
                                p.append("</td><td '" + (settings==null?"1":"2") + "'><table border='0' cellspacing='0' cellpadding='0'>\n");
                                
                                MBeanInfo mapInfo = map.getMBeanInfo(settings);
                                MBeanAttributeInfo mp[] = mapInfo.getAttributes();

                                // Printout modules in map.
                                for(int n2=0; n2<mp.length; n2++) {
                                    ModuleAttributeInfo mapAtt = (ModuleAttributeInfo)mp[n2]; //The attributes of the current attribute.

                                    Object currentMapAttribute = null;
                                    Object localMapAttribute = null;
                        
                                    try {
                                        currentMapAttribute = map.getAttribute(settings,mapAtt.getName());
                                        localMapAttribute = map.getLocalAttribute(settings,mapAtt.getName());
                                    } catch (Exception e1) {
                                        p.append(e1.toString() + " " + e1.getMessage());
                                    }
                                    p.append("<tr><td>" + mapAtt.getName() + "</td><td>&nbsp;-&gt;&nbsp</td><td>" + currentMapAttribute + "</td></tr>\n");
                                }
                                p.append("</table>\n");
                            } else {
                                p.append("</td><td colspan='" + (settings==null?"1":"2") + "'>"+currentAttribute);                        
                            }
                        }
                        p.append("</td></tr>\n");
                    }
                }
            }
        }
        return p.toString();
    }
    
    /**
     * Checks if there is an error for a specific attribute for a given
     * CrawlerSettings
     *
     * @param key The absolutename of the attribute to check for.
     * @param errorHandler The errorHandler containing the errors
     * @param settings the CrawlerSettings that is the 'current' context
     *
     */
    public String checkError(String key, CrawlJobErrorHandler errorHandler,
            CrawlerSettings settings){
        Constraint.FailedCheck failedCheck = null;
        if (errorHandler != null) {
            failedCheck = (Constraint.FailedCheck)errorHandler.getError(key);
        }
        if (failedCheck != null) {
            boolean sameSetting = false;
            if(settings != null && failedCheck.getSettings() == settings) {
                sameSetting = true;
            } else if(settings == null) {
                // If failedCheck.getSettings is the global setting then true.
                if(failedCheck.getSettings().getScope() == null ||
                        failedCheck.getSettings().getScope().length() == 0) {
                    sameSetting = true;
                }
            }
            
            if(sameSetting){
                return "<a class='help' style='color: red' href=\"javascript:doPop('" + 
                    TextUtils.escapeForHTMLJavascript(failedCheck.getMessage()) + "')\">*</a>";
            }
        }
        return "";
    }


  private static java.util.Vector _jspx_includes;

  static {
    _jspx_includes = new java.util.Vector(7);
    _jspx_includes.add("/include/handler.jsp");
    _jspx_includes.add("/include/jobconfigure.jsp");
    _jspx_includes.add("/include/head.jsp");
    _jspx_includes.add("/include/stats.jsp");
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
   * This pages allows the user to edit the configuration 
   * of a crawl order. 
   * That is set any af the 'values', but does not allow
   * users to change which 'modules' are used.
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
      out.write("\n\n");
      out.write("\n\n\n");
      out.write("<script type=\"text/javascript\">\n    function doAddList(listName){\n        newItem = document.getElementById(listName+\".add\");\n        theList = document.getElementById(listName);\n        \n        if(newItem.value.length > 0){\n            insertLocation = theList.length;\n            theList.options[insertLocation] = new Option(newItem.value, newItem.value, false, false);\n            newItem.value = \"\";\n        }\n        setEdited(listName);\n    }\n    \n    function doDeleteList(listName){\n        theList = document.getElementById(listName);\n        theList.options[theList.selectedIndex] = null;\n        setEdited(listName);\n    }\n\n    function doAddMap(mapName){\n        document.frmConfig.action.value = \"addMap\";\n        document.frmConfig.update.value = mapName;\n        doSubmit();\n    }\n    \n    function doDeleteMap(mapName, key){\n        document.frmConfig.action.value = \"deleteMap\";\n        document.frmConfig.update.value = mapName;\n        document.frmConfig.item.value = key;\n        doSubmit();\n    }            \n");
            out.write("</script>\n\n");

    // This code is shared by each of the configure.jsp pages. 
    // Sets up the CrawlJob, CrawlOrder, settingsHandler, 
    // the CrawlJobErrorHandler and sets the expert boolean.
    CrawlJob theJob = handler.getJob(request.getParameter("job"));
    if (theJob == null) {
        // Didn't find any job with the given UID or no UID given.
        response.sendRedirect(request.getContextPath() + "/jobs.jsp?message=" +
            "No job selected " + request.getParameter("job"));
        return;
    } else if(theJob.isReadOnly()) {
        // Can't edit this job.
        response.sendRedirect(request.getContextPath() + "/jobs.jsp?message=" +
            "Can't configure a read only job");
        return;
    }
    CrawlJobErrorHandler errorHandler = theJob.getErrorHandler();
    boolean expert = false;
    if(CookieUtils.getCookieValue(request.getCookies(), "expert", 
            "false").equals("true")) {
        expert = true;
    }
    // Get the settings objects.
    XMLSettingsHandler settingsHandler = theJob.getSettingsHandler();
    CrawlOrder crawlOrder = settingsHandler.getOrder();

      out.write("\n");
      out.write("\n\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n\n");
 
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
      out.write("\n\n    ");
      out.write("<script type=\"text/javascript\">\n        function doSubmit(){\n            // Before the form can be submitted we must\n            // ensure that ALL elements in ALL lists\n            // are selected. Otherwise they will be lost.\n            lists = new Array(");
      out.print(lists);
      out.write(");\n            for(i=0 ; i");
      out.write("<lists.length ; i++){\n                theList = document.getElementById(lists[i]);\n                for(j=0 ; j ");
      out.write("< theList.length ; j++){\n                    theList.options[j].selected = true;\n                }\n            }\n            document.frmConfig.submit();\n        }\n        \n        function doGoto(where){\n            document.frmConfig.action.value=\"goto\";\n            document.frmConfig.item.value = where;\n            doSubmit();\n        }\n        \n        function doPop(text){\n            alert(text);\n        }\n        \n        function setUpdate(){\n            document.frmConfig.update.value = \"true\";\n        }\n\n        function setEdited(name){\n            setUpdate();\n        }\n        \n        expert = ");
      out.print(expert);
      out.write(";\n        function setExpert(exp) {\n            var initVal = exp ? \"expertHide\" : \"expertShow\";\n            var newVal = exp ? \"expertShow\" : \"expertHide\";\n            var trElements = document.getElementsByTagName(\"tr\");\n            for(i = 0; i ");
      out.write("< trElements.length; i++) {\n                if(trElements[i].className == initVal) {\n                    trElements[i].className = newVal;\n                }   \n            }\n            eraseCookie('expert','/jobs/'); // erase legacy cookie if any\n            createCookie('expert',exp,365);\n            document.getElementById('hideExpertLink').className=exp?'show':'hide';\n            document.getElementById('showExpertLink').className=exp?'hide':'show';\n        }\n    ");
      out.write("</script>\n\n    ");
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
      out.write("<p>\n\n            ");
      out.write("<a id='hideExpertLink' \n                class='");
      out.print(expert?"show":"hide");
      out.write("' \n                href=\"javascript:setExpert(false)\">Hide expert settings");
      out.write("</a>\n\n            ");
      out.write("<a id='showExpertLink' \n               class='");
      out.print(expert?"hide":"show");
      out.write("'  \n               href=\"javascript:setExpert(true)\">View expert settings");
      out.write("</a>\n\n    ");
      out.write("<p>\n    \n    ");
      out.write("<form name=\"frmConfig\" method=\"post\" action=\"configure.jsp\">\n        ");
      out.write("<input type=\"hidden\" name=\"update\" value=\"false\">        \n        ");
      out.write("<input type=\"hidden\" name=\"action\" value=\"done\">\n        ");
      out.write("<input type=\"hidden\" name=\"item\" value=\"\">\n        ");
      out.write("<input type=\"hidden\" name=\"expert\" value=\"");
      out.print(expert);
      out.write("\">\n        ");
      out.write("<input type=\"hidden\" name=\"job\" value=\"");
      out.print(theJob.getUID());
      out.write("\">\n    \n        ");
      out.write("<p>            \n        ");
      out.write("<table>\n            ");
      out.write("<tr>\n                ");
      out.write("<td colspan=\"3\">\n                    ");
      out.write("<b>Meta data");
      out.write("</b>\n                ");
      out.write("</td>\n            ");
      out.write("</tr>\n            ");
      out.write("<tr>\n                ");
      out.write("<td>\n                    Description:\n                ");
      out.write("</td>\n                ");
      out.write("<td>");
      out.write("</td>\n                ");
      out.write("<td>\n                    ");
      out.write("<input name=\"meta/description\" \n                        value=\"");
      out.print(orderfile.getDescription());
      out.write("\"\n                        style=\"width: 440px\">\n                ");
      out.write("</td>\n            ");
      out.write("</tr>\n            ");
      out.write("<tr>\n                ");
      out.write("<td>\n                    Crawl Operator:\n                ");
      out.write("</td>\n                ");
      out.write("<td>");
      out.write("</td>\n                ");
      out.write("<td>\n                    ");
      out.write("<input name=\"meta/operator\"\n                        value=\"");
      out.print(orderfile.getOperator());
      out.write("\" \n                        style=\"width: 440px\">\n                ");
      out.write("</td>\n            ");
      out.write("</tr>\n            ");
      out.write("<tr>\n                ");
      out.write("<td>\n                    Crawl Organization:\n                ");
      out.write("</td>\n                ");
      out.write("<td>");
      out.write("</td>\n                ");
      out.write("<td>\n                    ");
      out.write("<input name=\"meta/organization\" \n                        value=\"");
      out.print(orderfile.getOrganization());
      out.write("\" \n                        style=\"width: 440px\">\n                ");
      out.write("</td>\n            ");
      out.write("</tr>\n            ");
      out.write("<tr>\n                ");
      out.write("<td>\n                    Crawl Job Recipient:\n                ");
      out.write("</td>\n                ");
      out.write("<td>");
      out.write("</td>\n                ");
      out.write("<td>\n                    ");
      out.write("<input name=\"meta/audience\" \n                        value=\"");
      out.print(orderfile.getAudience());
      out.write("\" \n                        style=\"width: 440px\">\n                ");
      out.write("</td>\n            ");
      out.write("</tr>\n            ");
      out.print(inputForm);
      out.write("\n            ");
      out.write("<tr>\n                ");
      out.write("<td colspan=\"3\">\n                    ");
      out.write("<b>Seeds");
      out.write("</b>\n                ");
      out.write("</td>\n            ");
      out.write("</tr>\n            ");
      out.write("<tr>\n                ");
      out.write("<td valign=\"top\">\n                    Seeds:\n                ");
      out.write("</td>\n                ");
      out.write("<td>");
      out.write("</td>\n                ");
      out.write("<td>\n                    ");

                        if(JobConfigureUtils.seedsEdittableSize(settingsHandler)) {
                     
      out.write("\n                    ");
      out.write("<textarea name=\"seeds\" style=\"width: 440px\" \n                        rows=\"8\" onChange=\"setUpdate()\">");

                        JobConfigureUtils.printOutSeeds(settingsHandler, out);
                    
      out.write("</textarea>\n                    ");

                        } else {
                    
      out.write("\n                    ");
      out.write("<a href=\"viewseeds.jsp?job=");
      out.print(theJob.getUID());
      out.write("\">Seed list");
      out.write("</a>\n                    too large to edit.\n                    ");

                        }
                    
      out.write("\n                ");
      out.write("</td>\n            ");
      out.write("</tr>\n        ");
      out.write("</table>\n    ");
      out.write("</form>\n    ");
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
      out.write("\n        \n");

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
