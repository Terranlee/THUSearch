<%@ page import="org.archive.crawler.admin.CrawlJobErrorHandler" %>
<%@ page import="org.archive.crawler.admin.ui.CookieUtils" %>
<%@ page import="org.archive.crawler.settings.*" %>
<%@ page import="javax.management.MBeanInfo"%>
<%@ page import="javax.management.Attribute"%>
<%@ page import="javax.management.MBeanAttributeInfo"%>
<%@ page import="org.archive.util.TextUtils" %>
<%@ page import="java.util.regex.*"%>

<%!
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
%>


<script type="text/javascript">
    function doAddList(listName){
        newItem = document.getElementById(listName+".add");
        theList = document.getElementById(listName);
        
        if(newItem.value.length > 0){
            insertLocation = theList.length;
            theList.options[insertLocation] = new Option(newItem.value, newItem.value, false, false);
            newItem.value = "";
        }
        setEdited(listName);
    }
    
    function doDeleteList(listName){
        theList = document.getElementById(listName);
        theList.options[theList.selectedIndex] = null;
        setEdited(listName);
    }

    function doAddMap(mapName){
        document.frmConfig.action.value = "addMap";
        document.frmConfig.update.value = mapName;
        doSubmit();
    }
    
    function doDeleteMap(mapName, key){
        document.frmConfig.action.value = "deleteMap";
        document.frmConfig.update.value = mapName;
        document.frmConfig.item.value = key;
        doSubmit();
    }            
</script>

<%
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
%>
