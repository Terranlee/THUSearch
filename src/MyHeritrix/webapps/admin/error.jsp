<%@ page language="java" isErrorPage="true" %>
<!DOCTYPE HTML PUBLIC "-//w3c//dtd html 4.0 transitional//en">
<html>
<head>
<title>ERROR</title>
</head>
<body bgcolor="#FFFFFF">

<h2>An error occured</h2>

<pre><h3 style="color: red"><%= exception %></h3></pre>

<pre><% exception.printStackTrace(new java.io.PrintWriter(out)); %></pre>

<a href="javascript:history.back()">You may be able to recover by going back</a>

</body>
</html>
