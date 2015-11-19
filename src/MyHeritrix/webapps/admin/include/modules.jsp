<%@ page import="java.util.List"%>
<%@ page import="java.util.ArrayList"%>
<%@ page import="java.util.Collections"%>
<%@ page import="java.io.IOException"%>
<%@ page import="org.archive.util.TextUtils"%>
<%@ page import="org.archive.crawler.settings.ModuleAttributeInfo"%>
<%@ page import="org.archive.crawler.settings.ModuleAttributeInfo"%>
<%@ page import="org.archive.crawler.settings.ModuleType"%>
<%@ page import="org.archive.crawler.settings.ComplexType"%>
<%@ page import="org.archive.crawler.settings.CrawlerSettings"%>
<%@ page import="org.archive.crawler.settings.MapType"%>
<%@ page import="org.archive.crawler.framework.Processor"%>
<%@ page import="org.archive.crawler.framework.CrawlScope"%>
<%@ page import="org.archive.crawler.framework.Frontier"%>
<%@ page import="javax.management.MBeanAttributeInfo"%>
<%@ page import="javax.management.MBeanInfo"%>
<%!
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
%>
