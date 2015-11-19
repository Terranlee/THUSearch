<%@ include file="/include/handler.jsp" %>
<%@ page import="org.archive.crawler.admin.CrawlJob,java.util.List" %>
<%@ page import="org.archive.crawler.Heritrix" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Iterator" %>
<%
    String title = "Instances";
    int tab = 5;

    String error = null;
    String baseurl = request.getContextPath() + request.getServletPath();
    Map m = Heritrix.getInstances();
    String newName = request.getParameter("createName");
    String d = request.getParameter("delete");
    if (newName != null && newName.length() > 0) {
        // Create a new instance.
        new Heritrix(newName, true);
    } else if (d != null && d.length() > 0) {
        // Delete an instance.
        String name = request.getParameter("heritrixName");
        if (m.size() <= 1) {
            // Don't remove all Heritrix instances.  The UI goes loopy if
            // no Heritrix instance to go against.
            error = "ERROR: You cannot delete ALL instances of Heritrix. " +
                "There must be at least one instance remaining or the UI " +
                "gets confused (To be Fixed).";
        } else {
            Heritrix h = (Heritrix)m.get(name);
            if (h != null) {
                h.destroy();
                m = Heritrix.getInstances();
                // If the heritrix instance deleted was the one seleted, select
                // another to be hook the UI on.
                if (heritrix == h) {
                    if (m != null && m.size() > 0) {
                        // Just get first Heritrix found.
                        Object k = m.keySet().iterator().next();
                        heritrix = (Heritrix)m.get(k);
                        handler = heritrix.getJobHandler();
                        application.setAttribute("heritrix", heritrix);
                        application.setAttribute("handler", handler);
                    }
                }
            }
        }
    } else {
        String q = request.getQueryString();
        if (q != null && q.length() > 0) {
            // Then we've been passed a Heritrix key on the query line.
            // Select that instance.
            heritrix = (Heritrix)m.get(q);
            application.setAttribute("heritrix", heritrix);
            application.setAttribute("handler", heritrix.getJobHandler());
        }
    }
%>
<%@include file="/include/head.jsp"%>
<html>
    <head>
        <title>Local Heritrix Instances' List</title>
        <link rel="stylesheet" 
            href="<%=request.getContextPath()%>/css/heritrix.css">
    </head>
    <body>
        <h1>Local Heritrix Instances</h1>
        <% if (error != null && error.length() > 0) {%>
            <p class="flashMessage"><%=error%></p> 
        <%}%>
        <p>Use this page to instantiate new instances of Heritrix.
        </p>
        <p>Below is a listing of the Heritrix instances currently running
        locally.  To create a new instance, fill in the
        textbox below and hit <i>Create</i>.  To peruse your newly created
        instance, select the instance name in the below list.  This sets the UI
        running against the selected instance. To delete an instance,
        hit <i>Delete</i>.  This will destroy the instance cleanly terminating
        any running jobs.  Note, you cannot delete all Heritrix instances.
        The UI gets confused if doesn't have an instance to juggle.
        </p>
        <form action="<%=baseurl%>" method="POST">
        <table border="0" cellspacing="0" cellpadding="2" 
            description="List of all local Heritrix instances">
        <thead>
        <tr>
        <th>Instance Name</th>
        <th>Status</th>
        </tr>
        </thead>
        <tbody>
        <% 
            for (final Iterator i = m.keySet().iterator(); i.hasNext();) {
                String key = (String)i.next();
                String url = baseurl + "?" + response.encodeURL(key);
                Heritrix h = (Heritrix)m.get(key);
                boolean currentSelection = (heritrix == h);
                String state = h.getStatus();
        %>
            <tr>
            <td class="instance_name"><p><a href="<%=url%>">
                <%
                    if (currentSelection) {
                %>
                    <b>
                <%
                    }
                %>
                   <input type="hidden" name="heritrixName" value="<%=key%>" />
                   <%=key.replace(",", ", ")%>
                <%
                    if (currentSelection) {
                %>
                    </b>
                <%
                    }
                %>
                    </a></p></td>
            <td><p><small><%=state%></small></p></td>
            <td><p><input type="submit" name="delete" value="Delete" /></p>
            </td>
            </tr>
        <%
            }
        %>
        </tbody>
        </table>
        </form>
        <p>
        <%
        if (heritrix.getMBeanName() == null) {
        %>
            To create new instances, Heritrix needs to be able to register
            with a JMX Agent. JMX is not enabled/available.  See
            <a href="http://crawler.archive.org/articles/user_manual.html#mon_com">9.5 Remote Monitoring and Control</a> in the User Manual for more on
            JMX.
        <%
        } else {
        %>
        <form action="<%=baseurl%>" method="POST">
            Name of new Heritrix instance: <input type="text"
                name="createName" size="32" />
            <input type="submit" name="submit" value="Create"/>
        </form>
        <%
        }
        %>
        </p>
    </body>
</html>
