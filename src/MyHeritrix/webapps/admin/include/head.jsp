<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page errorPage="/error.jsp" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="org.archive.crawler.Heritrix" %>
<%@ page import="org.archive.crawler.admin.CrawlJob" %>
<%@ page import="org.archive.util.ArchiveUtils" %>
<%@ page import="org.archive.util.TextUtils" %>
<%@ page import="org.archive.crawler.admin.StatisticsTracker" %>
<%@ page import="org.archive.crawler.Heritrix" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Iterator" %>
<%
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
	
%>
<%@include file="stats.jsp"%>

<html>
    <head>
    	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <title>Heritrix: <%=title%></title>
        <link rel="stylesheet" 
            href="<%=request.getContextPath()%>/css/heritrix.css">
        <link rel="icon" href="<%=request.getContextPath()%>/images/<%=favicon%>" type="image/x-icon" />
        <link rel="shortcut icon" href="<%=request.getContextPath()%>/images/<%=favicon%>" type="image/x-icon" />
        <script src="/js/util.js">
        </script>
    </head>

    <body>
        <table border="0" cellspacing="0" cellpadding="0" width="100%">
            <tr>
                <td>
                    <table border="0" cellspacing="0" cellpadding="0" height="100%">
                        <tr>
                            <td height="60" width="155" valign="top" nowrap>
                                <table border="0" width="155" cellspacing="0" cellpadding="0" height="60">
                                    <tr>
                                        <td align="center" height="40" valign="bottom">
                                            <a border="0" 
                                            href="<%=request.getContextPath()%>/index.jsp"><img border="0" src="<%=request.getContextPath()%>/images/logo.gif" height="37" width="145"></a>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="subheading">
                                            <%=title%>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                            <td width="5" nowrap>
                                &nbsp;&nbsp;
                            </td>
                            <td width="460" align="left" nowrap>
                                <table border="0" cellspacing="0" cellpadding="0" height="60">
                                    <tr>
                                        <td colspan="2" nowrap>
                                            <%
                                                SimpleDateFormat sdf = new SimpleDateFormat("MMM. d, yyyy HH:mm:ss");
                                                sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
                                            %>
                                            <b>
                                                Status as of <a style="color: #000000" href="<%=request.getRequestURL()%>"><%=sdf.format(new java.util.Date())%> GMT</a>
                                            </b>
                                            &nbsp;&nbsp;
                                            <span style="text-align:right">
                                            <b>
                                                Alerts: 
                                            </b>
                                            <% if(heritrix.getAlertsCount() == 0) { %>
                                                <a style="color: #000000; text-decoration: none" href="<%=request.getContextPath()%>/console/alerts.jsp">no alerts</a>
                                            <% } else if(heritrix.getNewAlertsCount()>0){ %>
                                                <b><a href="<%=request.getContextPath()%>/console/alerts.jsp"><%=heritrix.getAlerts().size()%> (<%=heritrix.getNewAlertsCount()%> new)</a></b>
                                            <% } else { %>
                                                <a style="color: #000000" href="<%=request.getContextPath()%>/console/alerts.jsp"><%=heritrix.getAlertsCount()%> (<%=heritrix.getNewAlertsCount()%> new)</a>
                                            <% } %>
                                            </span>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td valign="top" nowrap>
										<%= handler.isRunning()
										    ? "<span class='status'>Crawling Jobs</span>"
										    : "<span class='status'>Holding Jobs</span>"
										%><i>&nbsp;</i>
										</td>
										<td valign="top" align="right" nowrap>
										<%
										if(handler.isRunning() || handler.isCrawling()) {
										    if(handler.getCurrentJob() != null)
										    {%>
										<span class='status'>
										<%= shortJobStatus %></span> job:
										<i><%= handler.getCurrentJob().getJobName() %></i>
										<%
										    } else {
										        out.println("No job ready <a href=\"");
										        out.println(request.getContextPath());
										        out.println("/jobs.jsp\" style='color: #000000'>(create new)</a>");
										     }
										 }
										%>
										</td>
                                    </tr>
                                    <tr>
                                        <td nowrap>
                                            <%=handler.getPendingJobs().size()%>
                                            jobs
                                            <a style="color: #000000" href="<%=request.getContextPath()%>/jobs.jsp#pending">pending</a>,
                                            <%=handler.getCompletedJobs().size()%>
                                            <a style="color: #000000" href="<%=request.getContextPath()%>/jobs.jsp#completed">completed</a>
                                            &nbsp;
                                        </td>
                                        <td nowrap align="right">
                                            <% if(handler.isCrawling()){ %>
                                                    <%=(stats != null)? stats.successfullyFetchedCount(): 0%> URIs in 
		                                            <%= ArchiveUtils.formatMillisecondsToConventional( 
		                                            		((stats != null) 
		                                            		  	? (stats.getCrawlerTotalElapsedTime())
		                                            		  	: 0),
		                                            		false
		                                            	)
		                                            %>
		                                            (<%=ArchiveUtils.doubleToString(((stats != null)? stats.currentProcessedDocsPerSec(): 0),2)%>/sec)
                                            <% } %>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </td>
                <td width="100%" nowrap>
                    &nbsp;
                </td>
            </tr>
            <tr>
                <td bgcolor="#0000FF" height="1" colspan="4">
                </td>
            </tr>
            <tr>
                <td colspan="4" height="20">
                    <table border="0" cellspacing="0" cellpadding="0" width="100%" height="20">
                        <tr>
                            <td class="tab_seperator">&nbsp;</td>
                            <td class="tab<%=tab==0?"_selected":""%>">
                                <a href="<%=request.getContextPath()%>/index.jsp" class="tab_text<%=tab==0?"_selected":""%>">Console</a>
                            </td>
                            <td class="tab_seperator">&nbsp;</td>
                            <td class="tab<%=tab==1?"_selected":""%>">
                                <a href="<%=request.getContextPath()%>/jobs.jsp" class="tab_text<%=tab==1?"_selected":""%>">Jobs</a>
                            </td>
                            <td class="tab_seperator">&nbsp;</td>
                            <td class="tab<%=tab==2?"_selected":""%>">
                                <a href="<%=request.getContextPath()%>/profiles.jsp" class="tab_text<%=tab==2?"_selected":""%>">Profiles</a>
                            </td>
                            <td class="tab_seperator">&nbsp;</td>
                            <td class="tab<%=tab==3?"_selected":""%>">
                                <a href="<%=request.getContextPath()%>/logs.jsp" class="tab_text<%=tab==3?"_selected":""%>">Logs</a>
                            </td>
                            <td class="tab_seperator">&nbsp;</td>
                            <td class="tab<%=tab==4?"_selected":""%>">
                                <a href="<%=request.getContextPath()%>/reports.jsp" class="tab_text<%=tab==4?"_selected":""%>">Reports</a>
                            </td>
                            <td class="tab_seperator">&nbsp;</td>
                            <td class="tab<%=tab==5?"_selected":""%>">
                                <a href="<%=request.getContextPath()%>/setup.jsp" class="tab_text<%=tab==5?"_selected":""%>">Setup</a>
                            </td>
                            <td class="tab_seperator">&nbsp;</td>
                            <td class="tab<%=tab==6?"_selected":""%>">
                                <a href="<%=request.getContextPath()%>/help.jsp" class="tab_text<%=tab==6?"_selected":""%>">Help</a>
                             </td>
                            <td width="100%">
                            </td>
                        </tr>
                    </table>
                </td>
            </tr>
            <tr>
                <td bgcolor="#0000FF" height="1" colspan="4"></td>
            </tr>
         </table>
                    <!-- MAIN BODY -->
