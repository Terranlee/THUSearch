<%@ page language="java" import="java.util.*" pageEncoding="utf-8"%>
<%
request.setCharacterEncoding("utf-8");
System.out.println(request.getCharacterEncoding());
response.setCharacterEncoding("utf-8");
System.out.println(response.getCharacterEncoding());
String path = request.getContextPath();
String basePath = request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+path + "/";
System.out.println(path);
System.out.println(basePath);

// add this to fit different type of query
String currentType = (String) request.getParameter("type");
String souType = "搜索新闻";
if(currentType == null || currentType.equals("")){
	currentType = "html";
}
else if(currentType.equals("pic")){
	souType = "搜索图片";
}
else if(currentType.equals("doc")){
	souType = "搜索文档";
}
System.out.println("current type:" + currentType);
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title>THUso</title>
<style type="text/css">

body{
	width: auto;
	margin: 0;
	padding-bottom: 0;
	padding-left: 20px;
	padding-right: 20px;
	padding-top: 0;
}
#baiduimage{
	margin-left: 38%;
	margin-top: 10%;
}
ul
{
	list-style-type:none;
	margin-left:20%;
	padding:0;
	overflow:hidden;
}
li
{
	float: left;
}
li a:link,li a:visited
{
	display: block;
	width: 60px;
	font-weight: bold;
	color: #2625E5;
	text-align: center;
	text-decoration: underline;
	text-transform: uppercase;
	padding-bottom: 0px;
	padding-left: 0px;
	padding-right: 0px;
	padding-top: 0px;
}

li a:hover,li a:active
{
	background-color:#F00;
}
form{
	margin-left: 20%;
	margin-top:0px;
}
#text{
	width: 600px;
	height: 45px;
	border-color:#39F;
	border-style:solid;
	border-width:1px;
	font-size:24px;
}
#button{
	height:55px;
	width:120px;
	background-color:#39F;
	border-style:solid;
	font-size:24px;
	color:#FFF;
	margin-left:-10px;
}
div footer{
	margin-left: 35%;
	maigin-bottom: 0%;
	margin-top: 200px;
	margin-bottom: auto;
}
</style>
</head>

<div class="all">
	<div>
		<img src="/ImageSearch/pictures/logo.png" alt="" width="270" height="129" id="baiduimage"/>
	</div>
	<div>
		<ul>
			<li><a href="<%=basePath %>imagesearch.jsp?type=html"><b>新闻</b></a></li>
			<li><a href="<%=basePath %>imagesearch.jsp?type=pic">图片</a></li>
			<li><a href="<%=basePath %>imagesearch.jsp?type=doc">文库</a></li>
			<li><a href="tieba.baidu.com">贴吧</a></li>
			<li><a href="http://zhidao.baidu.com">知道</a></li>
			<li><a href="http://music.baidu.com">音乐</a></li>
			<li><a href="http://v.baidu.com">视频</a></li>
			<li><a href="http://map.baidu.com">地图</a></li>
			<li><a href="http://baike.baidu.com">百科</a></li>
			<li><a href="http://www.baidu.com/more/">更多>></a></li>
		</ul>
		
		<form id="form1" name="form1" method="get" action="servlet/ImageServer">
			<input type="text" id="text" name="query" value="真的好用，用了再用" maxlength="100">
			<input type="submit" id="button" name="Submit" value="<%=souType %>" title="<%=souType %>">
			<input type="hidden" id="what" name="type" value="<%=currentType %>">
		</form>
	</div>
	<div>
		<footer>
			<span>&copy;2015&nbsp;THUso&nbsp;
				<a href="http://www.baidu.com/duty/">By 李天润 刘鹤</a>&nbsp;京ICP证888888号
			</span>
		</footer>
	</div>
</div>

</body>
</html>
