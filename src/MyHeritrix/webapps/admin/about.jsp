<%@ page import="java.io.PrintWriter" %>
<%@include file="/include/handler.jsp"%>

<%
    String title = "About Heritrix";
    int tab = 6;
%>

<%@include file="/include/head.jsp"%>

<div class="margined">

<h1>About Heritrix</h1>

For more information, see 
<a href="http://crawler.archive.org">crawler.archive.org</a><br/>
<br/>
        
	<fieldset><legend>Versions</legend>
	Heritrix <%=Heritrix.getVersion()%><br/><br/>
	
	<%=System.getProperties().getProperty("java.runtime.name") %>
	<%=System.getProperties().getProperty("java.vm.version") %><br/><br/>
	
	<%=System.getProperties().getProperty("os.name") %>
	<%=System.getProperties().getProperty("os.version") %>
	
	</fieldset>
	<br/>
    <fieldset><legend>License</legend>
    
    <pre>
Copyright (C) 2003-2006 Internet Archive.

Heritrix is free software; you can redistribute it and/or modify
it under the terms of the GNU Lesser Public License as published by
the Free Software Foundation; either version 2.1 of the License, or
any later version.

Heritrix is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser Public License for more details.

You should have received a copy of the GNU Lesser Public License
along with Heritrix; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

Heritrix contains many other free and open source libraries; they are
governed by their respective licenses.</pre>
    </fieldset>
    <br/>
    <fieldset>
    <legend> System Properties </legend>
    <pre>
    <%
    	PrintWriter writer = new PrintWriter(out);
    	System.getProperties().list(writer);
    	writer.flush();
    %></pre>
    </fieldset>

</div>
<%@include file="/include/foot.jsp"%>
