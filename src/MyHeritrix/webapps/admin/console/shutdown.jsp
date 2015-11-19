<%@include file="/include/handler.jsp"%>
<%
    boolean shutdown = request.getParameter("shutdown")!=null
                       && request.getParameter("shutdown").equals("doit");
                       
    String title = "Shut down program";
    int tab = 0;
%>

<%@include file="/include/head.jsp"%>

    <% if(shutdown){ %>
        <p> 
            <b>Heritrix software has been shut down!</b>
        <p>
            This web access is no longer functioning. The software must 
            be relaunched via command line for it to be accessible again.
        <p>
            <i>Thank you for using Heritrix</i>
    <% } else { %>        
        <script type="text/javascript">
            function doShutDown(){
                if (confirm("Shut Heritrix software down?")){
                    document.frmShutDown.shutdown.value = "doit";
                    document.frmShutDown.submit();
                }
            }
        </script>
        
        <form name="frmShutDown" method="post" action="shutdown.jsp">
            <input type="hidden" name="shutdown">
        </form>
    
        <p>
            <b>Are you sure you wan't to shut Heritrix down?</b>
        <p>
            <span class="warning"><b>Warning:</b> Doing so will end any current job 
            and terminate this web access.<br> The program can only be restarted via
            command line launching</span>
        <p>
            <input type="button" value="I'm sure, shut it down" onClick="doShutDown()">
            <input type="button" value="Cancel" onClick="document.location='<%=request.getContextPath()%>/index.jsp'">

    <% } %>
<%@include file="/include/foot.jsp"%>

<% 
    if(shutdown){
        Thread temp = new Thread(){
            public void run(){
                try {
                    synchronized(this){
                        wait(200); // Wait a moment so we can finish displaying 'good bye' page
                    }
                } catch( InterruptedException e ) {
                    // We can ignore it.
                }
                Heritrix.prepareHeritrixShutDown();
                try {
                    synchronized(this){
                        wait(1000); // Wait for those threads that can terminate quickly.
                    }
                } catch( InterruptedException e ) {
                    // We can ignore it.
                }
                Heritrix.performHeritrixShutDown();
            }
        };
        temp.start();
    } 
%>
