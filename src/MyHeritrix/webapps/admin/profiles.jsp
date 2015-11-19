<%@include file="/include/handler.jsp"%>

<%@page import="java.util.List"%>
<%@page import="org.archive.crawler.admin.CrawlJob"%>
<%@page import="org.archive.crawler.settings.XMLSettingsHandler"%>

<%
    if(request.getParameter("default") != null) {
        CrawlJob defaultJob = handler.getJob(request.getParameter("default"));
        if(defaultJob != null && defaultJob.isProfile()){
            handler.setDefaultProfile(defaultJob);
        }
    }
    if(request.getParameter("delete") != null) {
        CrawlJob defaultJob = handler.getJob(request.getParameter("delete"));
        if(defaultJob != null && defaultJob.isProfile()) {
            handler.deleteProfile(defaultJob);
        }
    }

    String title = "Profiles";
    int tab = 2;
%>

<%@include file="/include/head.jsp"%>
<% if(request.getParameter("message")!=null && request.getParameter("message").length() >0){ %>
    <p>
        <span class="flashMessage"><b><%=request.getParameter("message")%></b></span>
<% } %>
<table border="0" cellspacing="0" cellpadding="1">
    <tr>
        <th>
            Profile name
        </th>
        <th>
            Actions
        </th>
    </tr>
    <%
        List profiles = handler.getProfiles();
        CrawlJob defaultProfile = handler.getDefaultProfile();
        
        boolean alt = true;
        for(int i=0 ; i<profiles.size() ; i++){
            CrawlJob profile = (CrawlJob)profiles.get(i);
    %>
            <tr <%=alt?"bgcolor='#EEEEFF'":""%>>
                <td width="150">
                    <%if(defaultProfile.getJobName().equals(profile.getJobName())){out.println("<b>");}%>
                    <%=profile.getJobName()%>&nbsp;&nbsp;
                    <%if(defaultProfile.getJobName().equals(profile.getJobName())){out.println("</b>");}%>
                </td>
                <td>
                    <a href="<%=request.getContextPath()%>/jobs/configure.jsp?job=<%=profile.getUID()%>" style="color: #003399;" class="underLineOnHover">Edit</a>
                    &nbsp;
                    <a href="<%=request.getContextPath()%>/jobs/new.jsp?job=<%=profile.getUID()%>" style="color: #003399;" class="underLineOnHover">New job based on it</a>
                    &nbsp;
                    <a href="<%=request.getContextPath()%>/jobs/new.jsp?job=<%=profile.getUID()%>&profile=true" style="color: #003399;" class="underLineOnHover">New profile based on it</a>
                    &nbsp;
                    <%if(defaultProfile.getJobName().equals(profile.getJobName())==false){%>
                        <a href="<%=request.getContextPath()%>/profiles.jsp?default=<%=profile.getUID()%>" style="color: #003399;" class="underLineOnHover">Set as default</a>
                        &nbsp;
                        <a href="<%=request.getContextPath()%>/profiles.jsp?delete=<%=profile.getUID()%>" style="color: #003399;" class="underLineOnHover">Delete</a>
                    <%}%>
                </td>
            </tr>
    <%
            alt = !alt;
        }
    %>
</table>
<%@include file="/include/foot.jsp"%>
