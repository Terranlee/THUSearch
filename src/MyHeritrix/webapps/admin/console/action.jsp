<%@ page errorPage="/error.jsp" %>
<%@include file="/include/handler.jsp"%>
<%
    /**
     * This webpage performs actions that can be performed from the console.
     */
    String sAction = request.getParameter("action");
    if(sAction != null)
    {
        // Need to handle an action    
        if(sAction.equalsIgnoreCase("start"))
        {
            // Tell handler to start crawl job
            handler.startCrawler();
        } else if(sAction.equalsIgnoreCase("stop")) {
            // Tell handler to stop crawl job
            handler.stopCrawler();
        } else if(sAction.equalsIgnoreCase("terminate")) {
            // Delete current job
            if(handler.getCurrentJob()!=null){
                handler.deleteJob(handler.getCurrentJob().getUID());
            }
        } else if(sAction.equalsIgnoreCase("pause")) {
            // Tell handler to pause crawl job
            handler.pauseJob();
        } else if(sAction.equalsIgnoreCase("resume")) {
            // Tell handler to resume crawl job
            handler.resumeJob();
        } else if(sAction.equalsIgnoreCase("checkpoint")) {
            if(handler.getCurrentJob() != null) {
                handler.checkpointJob();
            }
        }
    }    
    response.sendRedirect(request.getContextPath() + "/index.jsp");
%>
