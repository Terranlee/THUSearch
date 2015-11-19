<%@ page import="org.archive.crawler.util.LogReader" %><%
     // If there are empty lines before the start of the xml file Mozilla will
     // not display it properly!
    String inputFile = request.getParameter("file");
    if (inputFile == null) {
        out.println("No file");
        return;
    }
    response.setContentType("text/xml");
    out.println(LogReader.get(inputFile));
%>
