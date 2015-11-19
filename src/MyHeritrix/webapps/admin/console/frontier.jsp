<%@include file="/include/handler.jsp"%>

<%@ page import="org.archive.crawler.framework.FrontierMarker"%>
<%@ page import="org.archive.crawler.framework.exceptions.InvalidFrontierMarkerException"%>

<%@ page import="java.util.ArrayList"%>

<%@ page import="org.apache.commons.lang.StringUtils"%>

<%
    /**
     * This page allows users to inspect URIs in the Frontier of a paused
     * crawl. It also allows them to delete those URIs based on regular
     * expressions, or add URIs from an external file. 
     */
    
    String title = "View/Edit Frontier";
    int tab = 0;
    
%>

<%@include file="/include/head.jsp"%>
<%
   if( handler.getCurrentJob() != null)  {
       if ( !handler.getCurrentJob().getStatus().equals(CrawlJob.STATUS_PAUSED) ) {
%>
    <b style="color:red">MODIFYING THE FRONTIER OF A RUNNING CRAWL IS HIGHLY LIKELY TO
    CORRUPT THE CRAWL!</b>
<hr>
<%      }
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
%>
    <script type="text/javascript">
        function doDisplayInitial(){
            document.frmFrontierList.action.value = "initial";
            document.frmFrontierList.method = "GET";
            document.frmFrontierList.submit();
        }
        
        function doDisplayNext(){
            document.frmFrontierList.action.value = "next";
            document.frmFrontierList.method = "GET";
            document.frmFrontierList.submit();
        }
        
        function doCount(){
            document.frmFrontierList.action.value = "count";
            document.frmFrontierList.method = "GET";
            document.frmFrontierList.submit();
        }
        
        function doDelete(){
            if(confirm("This action will delete ALL URIs in the Frontier that match the specified regular expression!\nAre you sure you wish to proceed?")){
                document.frmFrontierList.action.value = "delete";
                document.frmFrontierList.submit();
            }
        }
        function checkForEnter(e){
            if(e.keyCode == 13){ //13 ascii == enter key
                doDisplayInitial();
            }
        }
    </script>
    
    <b>Add URIs</b>
<%        
    if("add".equals(action)) {
        String resultMessage = handler.importUris(
            request.getParameter("file"),
            request.getParameter("style"),
            request.getParameter("forceRevisit"));
        out.println("<br><font color='red'>"+resultMessage+"</font><br>");
        // don't do anything else 
        action = null;
    }
%>
    <form name="frmFrontierAdd" method="POST" action="frontier.jsp">
    <input type="hidden" name="action" value="add">
    <table cellspacing="0" cellpadding="0" width="100%">
        <tr>
            <td nowrap valign="right">
                Import from file:
            </td>
            <td>
                <input name="file" size="33" value="">
            </td>
            <td nowrap>
               &nbsp;<input type="submit" value="Import URIs">
            </td>
            <td width="100%">
            </td>
        </tr>
        <tr>
            <td></td>
            <td colspan="2" nowrap>
                <input type="radio" name="style" checked value="perLine">one URI per line 
                <input type="radio" name="style" value="crawlLog">crawl.log style
                <input type="radio" name="style" value="recoveryJournal">recovery journal style (uncompressed)
            </td>
            <td width="100%">
            </td>
        </tr>
        <tr>
            <td></td>
            <td colspan="2">
                <input type="checkbox" name="forceRevisit" value="true" name="verbose">
                Force revisit
            </td>
            <td width="100%">
            </td>
        </tr>

    </table>
    </form>
    
    <hr>
    <b>View or Delete URIs</b>
    <form name="frmFrontierList" method="POST" action="frontier.jsp">
    <input type="hidden" name="action" value="">
    <table cellspacing="0" cellpadding="0" width="100%">
        <tr>
            <td nowrap>
                URI match regex:
            </td>
            <td colspan="3">
                <input name="match" size="33" value="<%=regexpr%>" onKeyPress="checkForEnter(event)">
            </td>
            <td nowrap>
                &nbsp;<a href="<%=request.getContextPath()%>/help/regexpr.jsp">?</a>&nbsp;&nbsp;
            </td>
            <td nowrap>
                <input type="button" value="Display URIs" onClick="doDisplayInitial()">&nbsp;&nbsp;&nbsp;
                <input type="button" value="Count URIs" onClick="doCount()">&nbsp;&nbsp;&nbsp;
                <input type="button" value="Delete URIs" onClick="doDelete()">
            </td>
            <td width="100%">
            </td>
        </tr>
        <tr>
            <td nowrap>
                queue match regex:
            </td>
            <td colspan="3">
                <input name="queueRegex" size="33" value="<%=queueRegex%>" onKeyPress="checkForEnter(event)">
            </td>
            <td nowrap>
                &nbsp;<a href="<%=request.getContextPath()%>/help/regexpr.jsp">?</a>&nbsp;&nbsp;
            </td>
            <td>
                (affects deletes only)
            </td>
            <td width="100%">
            </td>
        </tr>
        <tr>
            <td nowrap>
                Display matches:
            </td>
            <td colspan="4">
                <input name="numberOfMatches" size="6" value="<%=numberOfMatches%>" onKeyPress="checkForEnter(event)">
            </td>
        </tr>
        <tr>
            <td nowrap>
                Verbose description:
            </td>
            <td>
                <input type="checkbox" value="true" name="verbose" <%=verbose?"checked":""%>>
            </td>
            <td align="right">
                grep style URI regex:
            </td>
            <td align="right" width="20">
                <input type="checkbox" value="true" name="grep" <%=grep?"checked":""%>>
            </td>
            <td></td>
        </tr>
        <tr><td height="5"></td></tr>
        <tr bgColor="black">
            <td bgcolor="#000000" height="1" colspan="7">
            </td>
        </tr>
<%        
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
%>
                        <tr><td height="5"></td></tr>
                        <tr>
                            <td colspan="7">
                                <% if(to>0) 
                                	{ %> Displaying URIs <%=from%> - <%=to%> matching <% } 
                                   else 
                                    { %> No URIs found matching <% } %> expression '<code><%=regexpr%></code>'.  
                                 <% if(hasNext){ %> <a href="javascript:doDisplayNext()">Get next set of matches &gt;&gt;</a> <% } %>
                            </td>
                        </tr>
                        <tr><td height="5"></td></tr>
                        <tr bgColor="black">
                            <td bgcolor="#000000" height="1" colspan="7">
                            </td>
                        </tr>
                        <tr><td height="5"></td></tr>
                        <tr>
                            <td colspan="7"><pre><%=outputString.toString()%></pre></td>
                        </tr>
                        <tr><td height="5"></td></tr>
                        <tr bgColor="black">
                            <td bgcolor="#000000" height="1" colspan="7">
                            </td>
                        </tr>
                        <tr><td height="5"></td></tr>
                        <tr>
                            <td colspan="7">
                                <% if(to>0) { %> Displaying URIs <%=from%> - <%=to%> matching <% } else { %> No URIs found matching <% } %> expression '<code><%=regexpr%></code>'.  <% if(hasNext){ %> <a href="javascript:doDisplayNext()">Get next set of matches &gt;&gt;</a> <% } %>
                            </td>
                        </tr>
                        <tr><td height="5"></td></tr>
<%
                        out.println("</pre>");
                    }
                }
%>
        <tr bgColor="black">
            <td bgcolor="#000000" height="1" colspan="7">
            </td>
        </tr>
        <tr><td height="5"></td></tr>
    </table>
    </form>
<%
    } else { 
%>
        <b>No current job.</b>
<%  
    } 
%>

<%@include file="/include/foot.jsp"%>
