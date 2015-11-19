<%@include file="/include/handler.jsp"%>

<%
    String title = "Help";
    int tab = 6;
    
    String regexpr = request.getParameter("regexpr");
    String testLines = request.getParameter("testLines");
%>

<%@include file="/include/head.jsp"%>

<%@page import="java.util.ArrayList,java.util.regex.PatternSyntaxException"%>
<%@page import="org.archive.util.TextUtils"%>

<p>
    <b>Heritrix online help:</b> Regular expressions in java
<p>
<table width="600">
    <tr>
        <td>
            All regular expressions used by Heritrix are Java regular expressions.
            <p>
            Java regular expressions differ from those used in Perl, for example, in 
            several ways. For detailed info on Java regular expressions see the 
            Java API for <code>java.util.regex.Pattern</code> on Sun's homepage 
            (<a href="http://java.sun.com">java.sun.com</a>). 
            <p>
            For API of Java SE v1.4.2 see
            <a href="http://java.sun.com/j2se/1.4.2/docs/api/index.html">
            http://java.sun.com/j2se/1.4.2/docs/api/index.html</a>,
            it is recommended you lookup the API for the version of Java that is
            being used to run Heritrix.
        </td>
    </tr>
    <tr>
        <td><p>&nbsp;</p></td>
    </tr>
    <tr>
        <td nowrap>
            <form method="Post">
	            Test reg.expr.: <input name="regexpr" size="78" value="<%=regexpr==null?"":regexpr%>"><br>
	            <textarea name="testLines" rows="10" cols="70"><% 
	               if(testLines != null){
	                   out.print(testLines);
	               }
	            %></textarea><br>
	            <input type="submit" value="Test reg.expr.">
            </form>
        </td>
    </tr>
    <%
        if(regexpr != null && testLines != null){
            // First, cut the testLines into lines.
            java.util.ArrayList lines = new ArrayList();
            int newLine = testLines.indexOf("\n");
            while( newLine > -1 ){
                lines.add(testLines.substring(0,newLine-1));
                testLines = testLines.substring(newLine+1);
                newLine = testLines.indexOf("\n");
            }
            lines.add(testLines); //the last line.
    %>
        <tr>
            <td>
                &nbsp;<p>
                <b>Results:</b> (bold lines were matched)
                <p>
                <pre><%
                    try{
	                    for(int i=0 ; i < lines.size() ; i++){
	                        String tmp = (String)lines.get(i);
	                        if(TextUtils.matches(regexpr, tmp)){
	                            // Matches, make bold
	                            tmp = "<b>"+tmp+"</b>";
	                        }
	                        out.println(tmp);
	                    }
			        } catch (PatternSyntaxException e){
			            out.println(e.getMessage());
			        }
                %></pre>
            </td>
        </tr>
    <%
        }
    %>
</table>

<%@include file="/include/foot.jsp"%>
