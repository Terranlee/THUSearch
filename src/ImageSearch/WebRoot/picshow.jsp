<%@ page language="java" import="java.util.*" pageEncoding="utf-8"%>
<%
request.setCharacterEncoding("utf-8");
System.out.println(request.getCharacterEncoding());
response.setCharacterEncoding("utf-8");
System.out.println(response.getCharacterEncoding());
String path = request.getContextPath();
String basePath = request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+path + "/";
String imagePath = request.getScheme() + "://"+request.getServerName()+":"+request.getServerPort() + '/';
System.out.println(path);
System.out.println(basePath);

// add this to fit different type of query
String currentType = (String) request.getParameter("type");
if(! currentType.equals("pic")){
	System.out.println("!!!!!type error, should be pic!!!!!");
	System.out.println("!!!!!!!!!!!" + currentType);
}
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title>图片结果</title>
<style type="text/css">

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
#gallery {
	width:750px;
	height:650px;
	position:relative;
	margin-top:20px;
	margin-left:auto;
	margin-right:auto;
	font-family:verdana, arial, sans-serif;
	background:#fff;
}

#gallery #slides {position:absolute; right:10px; top:0; height:550px; width:92px; overflow:hidden; text-align:center; z-index:500;}
#gallery #slides div {width:92px; height:550px; position:relative; padding-top:10px;}
#gallery #slides div ul {padding:0; margin:0; list-style:none; width:92px; height:550px}
#gallery #slides div ul li {float:left; padding:1px 0; width:92px; height:69px;}
#gallery #slides div ul li a {display:block; width:92px; height:69px; float:left;}
#gallery #slides div ul li a b {display:none;}
#gallery #slides div ul li a.previous {background:url(/ImageSearch/pictures/up.png) no-repeat center center;}
#gallery #slides div ul li a.next {background:url(/ImageSearch/pictures/down.png) no-repeat center center;}

#gallery #slides div ul li a img {display:block; width:92px; height:69px; border:0;}
#gallery #slides div ul li img.blank {margin:0 auto; padding-top:11px;}
#gallery #slides div ul li a:focus {outline:0;}


#gallery #fullsize {position:absolute; left:0; top:0; height:650px; width:750px; overflow:hidden; text-align:center; z-index:200;}

#gallery #fullsize div {width:750px; height:700px; padding-top:70px; position:relative;}
#gallery #fullsize div img {clear:both; display:block; margin:0 auto; border:1px solid #eee; width:480px; height:360px; position:relative; background:#fff; padding:10px;}
#gallery #fullsize div h3 {padding:10px 0 0 0; margin:0; font-size:18px;}
#gallery #fullsize div p {padding:5px 135px; margin:0; font-size:12px; line-height:18px;}

</style>
</head>

<body>
<%
	String currentQuery=(String) request.getAttribute("currentQuery");
	int currentPage=(Integer) request.getAttribute("currentPage");
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
			<input type="submit" id="button" name="Submit" value="搜索图片" title="搜索图片">
			<input type="hidden" id="what" name="type" value="pic">
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

	<!-- get answer from the searcher -->
	<% 
  	String[] imgContents = (String[]) request.getAttribute("imgContents");
  	String[] imgPaths = (String[]) request.getAttribute("imgPaths");
  	String[] imgTitles = (String[]) request.getAttribute("imgTitles");
  	String[] imgAnchors = (String[]) request.getAttribute("imgAnchors");
  	%>
	
<div id="gallery">
	<div id="slides">
	<%
	if(imgContents != null && imgContents.length >= 5){
		int length = imgContents.length;
		for(int i=1; i<=(length-4); i++){
			String slideName = "slide" + new Integer(i).toString();
	%>
			<div id="<%=slideName %>">
				<ul>
					<%
					if(i == 1){
				 	%>
						<li><img class="blank" src="/ImageSearch/pictures/upx.png" alt="" /></li>
					<%
					}
					else{
						String previewsSlide = "#slide" + new Integer(i-1).toString();
				 	%>
				 		<li><a href="<%=previewsSlide %>" class="previous"><b>Previous</b></a></li>
				 	<%
				 	}
				 	for(int j=i; j<=i+4; j++){
				 		String picName = "#pic" + new Integer(j).toString();
				 		String picSrc = imgPaths[j-1];
				 		String picAlt = imgTitles[j-1];
				 	%>
						<li><a href="<%=picName %>"><img src="<%=picSrc %>" alt="<%=picAlt %>"/></a></li>
				 	<%
				 	}
				 	if(i != (length-4)){
				 		String nextSlide = "#slide" + new Integer(i+1).toString();
				 	%> 
						<li><a href="<%=nextSlide %>" class="next"><b>Next</b></a></li>
				 	<%
				 	}
				 	else{
				 	%>
						<li><img class="blank" src="/ImageSearch/pictures/downx.png" alt="" /></li>
				 	<%
				 	}
				 	%>
				</ul>
			</div>  <!-- end of slides(i) -->
	<%
		}
	}	// end of if(lenth > 5)
	
	else if(imgContents != null && imgContents.length <= 4 && imgContents.length > 0){
		int length = imgContents.length;
	%>
		<div id="slide1">
			<ul>
				<li><img class="blank" src="/ImageSearch/pictures/upx.png" alt="" /></li>
				<%
				for(int i=1; i<=length; i++){
					String picName = "#pic" + new Integer(i).toString();
					String picSrc = imgPaths[i-1];
					String picAlt = imgTitles[i-1];
				%>
					<li><a href="<%=picName %>"><img src="<%=picSrc %>" alt="<%=picAlt %>" /></a></li>
				<%
				}	
				%>
				<li><img class="blank" src="/ImageSearch/pictures/downx.png" alt="" /></li>
			</ul>
		</div>	<!-- end of slide1 -->
	<%
	}
	else if(imgContents == null || imgContents.length == 0){
		out.println("<p>没有图片(ㄒoㄒ)</p>");
		System.out.println("nothing");
	}
	%>
	</div>	<!-- end of <div id="slides"> -->
	
	<div id="fullsize">
		<%
		if(imgContents != null){
			int length = imgContents.length;
			for(int i=1; i<=length; i++){
				String picName = "pic" + new Integer(i).toString();
				String picSrc = imgPaths[i-1];
				String picAlt = imgTitles[i-1];
				/*
				int l = 250;
				if(imgContents[i-1].length() < 250){
					l = imgContents[i-1].length();
				}
				*/
				String picContent = imgContents[i-1];
				String picAnchor = "/" + imgAnchors[i-1];
		%>
		<div id="<%=picName%>">
			<img src="<%=picSrc %>" alt="<%=picAlt %>" />
      		<h3><a href="<%=picAnchor %>"><%=picAlt %></a></h3>
      		<p><%=picContent %></p>
      		<p></p>
		</div>
		<%
			}
		}
		else{
			out.println("没有图片(ㄒoㄒ)</p>");
		}
		%>
	</div>   <!-- end of div fullsize -->
</div>  <!--  end of div gallary -->

<!--  for the page number -->
<div style="width:95%; height:auto; display:block; margin:0 auto; margin-top:0px; font-size:10pt; line-height:150%;">
	<div>
  	<p>
		<%if(currentPage>1){ %>
			<a href="ImageServer?query=<%=currentQuery%>&page=<%=currentPage-1%>&type=<%=currentType %>">上一页</a>
		<%}; %>
		<%for (int i=Math.max(1,currentPage-5);i<currentPage;i++){%>
			<a href="ImageServer?query=<%=currentQuery%>&page=<%=i%>&type=<%=currentType %>"><%=i%></a>
		<%}; %>
		<strong><%=currentPage%></strong>
		<%for (int i=currentPage+1;i<=currentPage+5;i++){ %>
			<a href="ImageServer?query=<%=currentQuery%>&page=<%=i%>&type=<%=currentType %>"><%=i%></a>
		<%}; %>
		<a href="ImageServer?query=<%=currentQuery%>&page=<%=currentPage+1%>&type=<%=currentType %>">下一页</a>
	</p>
  </div>
</div>

</body>
</html>