<%@ page language="java" import="java.util.*" pageEncoding="utf-8"%>
<%
request.setCharacterEncoding("utf-8");
response.setCharacterEncoding("utf-8");
String path = request.getContextPath();
String basePath = request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+path+"/";
String imagePath = request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+"/";
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title>搜索结果</title>
<style type="text/css">
<!--
#baiduimage{
	margin-left: 38%;
	margin-top: 10%;
}
#Layer1 {
	width:100%;
	height:100px;
	margin:0 auto;
}
#mylogo {
	float:left;
	color:#FFF;
	width:240px;
	height:80px;
}

#request {
 	float:left;
 	width:900px;
 	height:100px;
}
form{
	margin-left: 0%;
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
ul
{
	list-style-type:none;
	margin-left:0%;
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
#Layer2 {
	position:absolute;
	left:29px;
	top:110px;
	width:648px;
	height:602px;
	z-index:2;
}
#Layer3 {
	position:absolute;
	left:28px;
	top:697px;
	width:652px;
	height:67px;
	z-index:3;
}
-->
</style>
</head>

<body>
<%
	String currentQuery=(String) request.getAttribute("currentQuery");
	int currentPage=(Integer) request.getAttribute("currentPage");
	String searchType = (String) request.getAttribute("type");
	String souType = "搜索新闻";
	if(searchType == null || searchType.equals("")){
		searchType = "html";
	}
	else if(searchType.equals("pic")){
		souType = "搜索图片";
	}
	else if(searchType.equals("doc")){
		souType = "搜索文档";
	}
	
%>
<div id="Layer1">
  <div id="mylogo">
		<a href="<%=imagePath%>ImageSearch/imagesearch.jsp">
			<img src="/ImageSearch/pictures/logo.png" alt="回到首页"id="baiduimage2" height="80px" title="回到首页"></img>
		</a>
	</div>
	<div id="request">
  		<form id="form1" name="form1" method="get" action="ImageServer">
			<input type="text" id="text" name="query" value="<%=currentQuery %>" maxlength="100">
			<input type="submit" id="button" name="Submit" value="<%=souType%>" title="<%=souType%>">
			<input type="hidden" id="what" name="type" value="<%=searchType %>">
		</form>
		<ul>
			<li><a href="<%=basePath %>servlet/ImageServer?type=html&query=<%=currentQuery %>"><b>新闻</b></a></li>
			<li><a href="<%=basePath %>servlet/ImageServer?type=pic&query=<%=currentQuery %>">图片</a></li>
			<li><a href="<%=basePath %>servlet/ImageServer?type=doc&query=<%=currentQuery %>">文库</a></li>
			<li><a href="tieba.baidu.com">贴吧</a></li>
			<li><a href="http://zhidao.baidu.com">知道</a></li>
			<li><a href="http://music.baidu.com">音乐</a></li>
			<li><a href="http://v.baidu.com">视频</a></li>
			<li><a href="http://map.baidu.com">地图</a></li>
			<li><a href="http://baike.baidu.com">百科</a></li>
			<li><a href="http://www.baidu.com/more/">更多>></a></li>
		</ul>
  	</div>
</div>
<div id="Layer2" style="height: 585px;">
  <div id="imagediv">结果显示如下：
  <br>
  <Table style="left: 0px; width: 594px;">
  <% 
  	String[] imgContents = (String[]) request.getAttribute("imgContents");
  	String[] imgPaths = (String[]) request.getAttribute("imgPaths");
  	String[] imgTitles = (String[]) request.getAttribute("imgTitles");
  	String[] imgAnchors = (String[]) request.getAttribute("imgAnchors");
  	
  	if(imgContents!=null && imgContents.length>0){
  		for(int i=0;i<imgContents.length;i++){
  			// change imgTags to imgTitle, because imgTags now contains html content
  			String imgTitle = imgTitles[i];
  			/*
  			int l = 250;
  			if(imgContents[i].length() < 250){
  				l = imgContents[i].length();
  			}
  			*/
  			String imgContent = imgContents[i];
  			String imgPath = imgPaths[i];
  			String imgAnchor = imgAnchors[i]; 
  		%>
  		<p>
  		<tr><h3><a href="<%=imagePath+imgPath %>" target="_blank"><%--<%=(currentPage-1)*10+i+1.--%> <%=imgTitle %></a></h3></tr>
  		<tr><h5><%=imgContent %>></h5></tr>
  		<tr><h5><%=imagePath+imgPath %></h5></tr>
  		<%--<tr><img src="<%=imagePath+imgPaths[i]%>" alt="<%=imagePath+imgPaths[i]%>" width=200 height=100 /></tr> --%>
  		</p>
  		<%}; %>
  	<%}else{ %>
  		<p><tr><h3>no such result</h3></tr></p>
  	<%}; %>
  </Table>
  </div>
  <div>
  	<p>
		<%if(currentPage>1){ %>
			<a href="ImageServer?query=<%=currentQuery%>&page=<%=currentPage-1%>&type=<%=searchType %>>">上一页</a>
		<%}; %>
		<%for (int i=Math.max(1,currentPage-5);i<currentPage;i++){%>
			<a href="ImageServer?query=<%=currentQuery%>&page=<%=i%>&type=<%=searchType %>"><%=i%></a>
		<%}; %>
		<strong><%=currentPage%></strong>
		<%for (int i=currentPage+1;i<=currentPage+5;i++){ %>
			<a href="ImageServer?query=<%=currentQuery%>&page=<%=i%>&type=<%=searchType %>"><%=i%></a>
		<%}; %>
		<a href="ImageServer?query=<%=currentQuery%>&page=<%=currentPage+1%>&type=<%=searchType %>">下一页</a>
	</p>
  </div>
</div>
<div id="Layer3" style="top: 839px; left: 27px;">
	
</div>
<div>
</div>
</body>
