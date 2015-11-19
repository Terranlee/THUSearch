<%@include file="/include/handler.jsp"%>

<%
    String title = "Help";
    int tab = 6;
%>

<%@include file="/include/head.jsp"%>

<div class="margined">
    <h1>Heritrix online help</h1>
<p>
    <b><a href="<%=request.getContextPath()%>/about.jsp">About Heritrix</a></b></br>
    Includes license and current environment information.
</p>
<p>
    <b><a target="_blank" 
    href="<%=request.getContextPath()%>/docs/articles/user_manual/index.html">User
        Manual</a></b><br> Covers creating, configuring, launching,
        monitoring and analysing crawl jobs. For all users.
</p>
<p>
    <b><a target="_blank" 
        href="<%=request.getContextPath()%>/docs/articles/developer_manual/index.html">Developer Manual</a></b><br> Covers how to write add on modules for Heritrix
        and provides in depth coverage of Heritrix's architecture. For
        advanced users.
</p>
<p>
    <b><a target="_blank" 
        href="<%=request.getContextPath()%>/docs/articles/releasenotes/index.html">Release Notes</a></b><br>
</p>
<p>
	<b><a href="http://crawler.archive.org/issue-tracking.html" target="_blank">Issue Tracking</a></b><br />
	If you have found a bug or would like to see new features in Heritrix, check the following links:
	<ul>
		<li><a href="http://sourceforge.net/tracker/?atid=539099&amp;group_id=73833&amp;func=browse" target="_blank">Bugs</a></li>
		<li><a href="http://sourceforge.net/tracker/?atid=539102&amp;group_id=73833&amp;func=browse" target="_blank">Feature Requests</a></li>
	</ul>
</p>
<p>
    <b><a href="http://crawler.archive.org/mail-lists.html" target="_blank">Mailing Lists</a></b><br />
    For general discussion on Heritrix, use our <a href="http://groups.yahoo.com/group/archive-crawler/" target="_blank">Crawler Discussion List</a>.
</p>
<p>
    <b><a href="<%=request.getContextPath()%>/help/regexpr.jsp">Regular Expressions</a></b><br />
    Information about the regular expressions used in Heritrix and a tool to double check that your regular expressions are valid and that they correctly identify the desired strings.
</p>
<p>
    <b><a href="<%=request.getContextPath()%>/help/codes.jsp">URI Fetch Status Codes</a></b><br />
    This reference details what each of the fetch status codes assigned to URIs means.
</p>
<hr />
<font size="-1">Heritrix version @VERSION@</font>
</div>
<%@include file="/include/foot.jsp"%>
