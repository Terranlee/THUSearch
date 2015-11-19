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
    <b>Heritrix online help:</b> URI Fetch Status Codes
</p>
<p>
    The fetch status codes associated with each URI that is processed, are either 
    defined by Heritrix or Heritrix uses the HTTP status codes. See the 
    <a target="_blank" href="<%=request.getContextPath()%>/docs/articles/user_manual.html">User
    Manual</a> for more on the status codes defined by Heritrix. Addtional information
    about HTTP status codes can be found <a target="_blank" href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html">here</a>
</p>
<p>
    Note that sometimes webservers will return a HTTP status code that is undefined.
</p>
<table width="600">
    <tr>
        <th width=40>
            Code
        </th>
        <th align=left>
            Meaning            
        </th>
    </tr>
    <tr><td></td><td><i>Heritrix defined codes</i></tr>
	<tr><td align=right valign=top>1</td><td>Successful DNS lookup</td></tr>
	<tr><td align=right valign=top>0</td><td>Fetch never tried (perhaps protocol unsupported or illegal URI)</td></tr>
	<tr><td align=right valign=top>-1</td><td>DNS lookup failed</td></tr>
	<tr><td align=right valign=top>-2</td><td>HTTP connect failed</td></tr>
	<tr><td align=right valign=top>-3</td><td>HTTP connect broken</td></tr>
	<tr><td align=right valign=top>-4</td><td>HTTP timeout (before any meaningful response received)</td></tr>
	<tr><td align=right valign=top>-5</td><td>Unexpected runtime exception; see runtime-errors.log</td></tr>
	<tr><td align=right valign=top>-6</td><td>Prerequisite domain-lookup failed, precluding fetch attempt</td></tr>
	<tr><td align=right valign=top>-7</td><td>URI recognized as unsupported or illegal</td></tr>
	<tr><td align=right valign=top>-8</td><td>Multiple retries all failed, retry limit reached</td></tr>
	<tr><td align=right valign=top>-50</td><td>Temporary status assigned URIs awaiting preconditions; appearance in logs may be a bug</td></tr>
	<tr><td align=right valign=top>-60</td><td>Failure status assigned URIs which could not be queued by the Frontier (and may in fact be unfetchable)</td></tr>
	<tr><td align=right valign=top>-61</td><td>Prerequisite robots.txt-fetch failed, precluding a fetch attempt</td></tr>
	<tr><td align=right valign=top>-62</td><td>Some other prerequisite failed, precluding a fetch attempt</td></tr>
	<tr><td align=right valign=top>-63</td><td>A prerequisite (of any type) could not be scheduled, precluding a fetch attempt</td></tr>
	<tr><td align=right valign=top>-3000</td><td>Severe Java 'Error' conditions (OutOfMemoryError, StackOverflowError, etc.) during URI processing.</td></tr>
	<tr><td align=right valign=top>-4000</td><td>'chaff' detection of traps/content of negligible value applied</td></tr>
	<tr><td align=right valign=top>-4001</td><td>Too many link hops away from seed</td></tr>
	<tr><td align=right valign=top>-4002</td><td>Too many embed/transitive hops away from last URI in scope</td></tr>
	<tr><td align=right valign=top>-5000</td><td>Out of scope upon reexamination (only happens if scope changes during crawl)</td></tr>
	<tr><td align=right valign=top>-5001</td><td>Blocked from fetch by user setting</td></tr>
	<tr><td align=right valign=top>-5002</td><td>Blocked by a custom processor</td></tr>
	<tr><td align=right valign=top>-5003</td><td>Blocked due to exceeding an established quota</td></tr>
	<tr><td align=right valign=top>-5004</td><td>Blocked due to exceeding an established runtime</td></tr>
	<tr><td align=right valign=top>-6000</td><td>Deleted from Frontier by user</td></tr>
	<tr><td align=right valign=top>-7000</td><td>Processing thread was killed by the operator (perhaps because of a hung condition)</td></tr>
	<tr><td align=right valign=top>-9998</td><td>Robots.txt rules precluded fetch</td></tr>
    <tr><td></td><td><i>HTTP codes</i></tr>
    <tr>
        <td align=right valign=top><i>1xx</i></td>
        <td><i>Informational</i></td>
    </tr>
    <tr>
        <td align=right valign=top>100</td>
        <td>Continue</td>
    </tr>
    <tr>
        <td align=right valign=top>101</td>
        <td>Switching Protocols</td>
    </tr>
    <tr>
        <td align=right valign=top><i>2xx</i></td>
        <td><i>Successful</i></td>
    </tr>
    <tr>
        <td align=right valign=top>200</td>
        <td>OK</td>
    </tr>
    <tr>
        <td align=right valign=top>201</td>
        <td>Created</td>
    </tr>
    <tr>
        <td align=right valign=top>202</td>
        <td>Accepted</td>
    </tr>
    <tr>
        <td align=right valign=top>203</td>
        <td>Non-Authoritative Information</td>
    </tr>
    <tr>
        <td align=right valign=top>204</td>
        <td>No Content</td>
    </tr>
    <tr>
        <td align=right valign=top>205</td>
        <td>Reset Content</td>
    </tr>
    <tr>
        <td align=right valign=top>206</td>
        <td>Partial Content</td>
    </tr>
    <tr>
        <td align=right valign=top><i>3xx</i></td>
        <td><i>Redirection</i></td>
    </tr>
    <tr>
        <td align=right valign=top>300</td>
        <td>Multiple Choices</td>
    </tr>
    <tr>
        <td align=right valign=top>301</td>
        <td>Moved Permanently</td>
    </tr>
    <tr>
        <td align=right valign=top>302</td>
        <td>Found</td>
    </tr>
    <tr>
        <td align=right valign=top>303</td>
        <td>See Other</td>
    </tr>
    <tr>
        <td align=right valign=top>304</td>
        <td>Not Modified</td>
    </tr>
    <tr>
        <td align=right valign=top>305</td>
        <td>Use Proxy</td>
    </tr>
    <tr>
        <td align=right valign=top>307</td>
        <td>Temporary Redirect</td>
    </tr>
    <tr>
        <td align=right valign=top><i>4xx</i></td>
        <td><i>Client Error</i></td>
    </tr>
    <tr>
        <td align=right valign=top>400</td>
        <td>Bad Request</td>
    </tr>
    <tr>
        <td align=right valign=top>401</td>
        <td>Unauthorized</td>
    </tr>
    <tr>
        <td align=right valign=top>402</td>
        <td>Payment Required</td>
    </tr>
    <tr>
        <td align=right valign=top>403</td>
        <td>Forbidden</td>
    </tr>
    <tr>
        <td align=right valign=top>404</td>
        <td>Not Found</td>
    </tr>
    <tr>
        <td align=right valign=top>405</td>
        <td>Method Not Allowed</td>
    </tr>
    <tr>
        <td align=right valign=top>406</td>
        <td>Not Acceptable</td>
    </tr>
    <tr>
        <td align=right valign=top>407</td>
        <td>Proxy Authentication Required</td>
    </tr>
    <tr>
        <td align=right valign=top>408</td>
        <td>Request Timeout</td>
    </tr>
    <tr>
        <td align=right valign=top>409</td>
        <td>Conflict</td>
    </tr>
    <tr>
        <td align=right valign=top>410</td>
        <td>Gone</td>
    </tr>
    <tr>
        <td align=right valign=top>411</td>
        <td>Length Required</td>
    </tr>
    <tr>
        <td align=right valign=top>412</td>
        <td>Precondition Failed</td>
    </tr>
    <tr>
        <td align=right valign=top>413</td>
        <td>Request Entity Too Large</td>
    </tr>
    <tr>
        <td align=right valign=top>414</td>
        <td>Request-URI Too Long</td>
    </tr>
    <tr>
        <td align=right valign=top>415</td>
        <td>Unsupported Media Type</td>
    </tr>
    <tr>
        <td align=right valign=top>416</td>
        <td>Requested Range Not Satisfiable</td>
    </tr>
    <tr>
        <td align=right valign=top>417</td>
        <td>Expectation Failed</td>
    </tr>
    <tr>
        <td align=right valign=top><i>5xx</i></td>
        <td><i>Server Error</i></td>
    </tr>
    <tr>
        <td align=right valign=top>500</td>
        <td>Internal Server Error</td>
    </tr>
    <tr>
        <td align=right valign=top>501</td>
        <td>Not Implemented</td>
    </tr>
    <tr>
        <td align=right valign=top>502</td>
        <td>Bad Gateway</td>
    </tr>
    <tr>
        <td align=right valign=top>503</td>
        <td>Service Unavailable</td>
    </tr>
    <tr>
        <td align=right valign=top>504</td>
        <td>Gateway Timeout</td>
    </tr>
    <tr>
        <td align=right valign=top>505</td>
        <td>HTTP Version Not Supported</td>
    </tr>

</table>

<%@include file="/include/foot.jsp"%>
