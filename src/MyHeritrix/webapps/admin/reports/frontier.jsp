<%@include file="/include/handler.jsp"%>
<%@ page import="java.io.BufferedOutputStream" %>
<%@ page import="java.io.FileOutputStream" %>

<%
    String title = "Frontier report";
    int tab = 4;
    String reportName = request.getParameter("name");
    String dumpFile = request.getParameter("dumpFile");
%>

<%@include file="/include/head.jsp"%>
        <pre>
<%
            if(handler.getCurrentJob() != null) {
                java.io.PrintWriter writer;
                if (dumpFile!=null) {
                    writer = new java.io.PrintWriter(new BufferedOutputStream(
                        new FileOutputStream(dumpFile)));
                    %> Report dumping to file '<%=dumpFile%>'... <%
                } else {
            	    writer = new java.io.PrintWriter(out);
            	}
            	handler.getCurrentJob().writeFrontierReport(reportName,writer);
            	writer.flush();
            	if (dumpFile!=null) {
            		writer.close();
            	    %> ...done. <%
            	}
            } else {
%> No current job <%
            }
%>      </pre>

<%@include file="/include/foot.jsp"%>
