import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import java.util.*;

import java.math.*;
import java.net.*;
import java.io.*;

//http://localhost:8080/ImageSearch/imagesearch.jsp

public class ImageServer extends HttpServlet{
	
	// add absolute file path
	public static final String FILE_DIR = "";	
	//public static final String FILE_DIR="F:\\pro\\workspace\\MyHeritrix\\jobs\\files-20150611080340429\\mirror\\news.tsinghua.edu.cn";
	public static final int PRE_RESULT = 13;
	public static final int PAGE_RESULT = 10;
	public static final int RESULT_NUM = 100; 
	
	public static final String docIndexDir = "docIndex";
	public static final String picIndexDir = "picIndex";
	public static final String htmlIndexDir = "htmlIndex";
	
	private ImageSearcher docSearch = null;
	private ImageSearcher picSearch = null;
	private ImageSearcher htmlSearch = null;
	
	public ImageServer(){
		super();
		docSearch = new ImageSearcher(new String(docIndexDir+"/index"));
		docSearch.loadGlobals(new String(docIndexDir+"/global.txt"));
		
		picSearch = new ImageSearcher(new String(picIndexDir + "/index"));
		picSearch.loadGlobals(new String(picIndexDir+"/global.txt"));
		
		htmlSearch = new ImageSearcher(new String(htmlIndexDir + "/index"));
		htmlSearch.loadGlobals(new String(htmlIndexDir+"/global.txt"));
	}
	
	public ScoreDoc[] showList(ScoreDoc[] results,int page){
		if(results==null || results.length<(page-1)*PRE_RESULT){
			return null;
		}
		int start=Math.max((page-1)*PRE_RESULT, 0);
		int docnum=Math.min(results.length-start,PRE_RESULT);
		ScoreDoc[] ret=new ScoreDoc[docnum];
		for(int i=0;i<docnum;i++){
			ret[i]=results[start+i];
		}
		return ret;
	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("text/html;charset=utf-8");
		request.setCharacterEncoding("utf-8");
		String queryString = request.getParameter("query");
		String pageString = request.getParameter("page");
		String searchType = request.getParameter("type");
		int page=1;
		if(pageString!=null){
			page=Integer.parseInt(pageString);
		}
		if(queryString==null){
			System.out.println("null query");
			//request.getRequestD ispatcher("/Image.jsp").forward(request, response);
		}else{
			System.out.println(queryString);
			//System.out.println(URLDecoder.decode(queryString,"utf-8"));
			//System.out.println(URLDecoder.decode(queryString,"gb2312"));
			ArrayList<String> contents = new ArrayList<String>();
			ArrayList<String> paths = new ArrayList<String>();
			ArrayList<String> titles = new ArrayList<String>();
			ArrayList<String> anchors = new ArrayList<String>();
			
			ImageSearcher search = null;
			if(searchType.equals("pic"))
				search = picSearch;
			else if(searchType.equals("doc"))
				search = docSearch;
			else if(searchType.equals("html"))
				search = htmlSearch;
			else{
				search = htmlSearch;
				System.out.println("!!!!! wrong type of search !!!!!!");
			}
			
			HTMLGenerator g = new HTMLGenerator();
			TopDocs results=search.searchQuery(queryString, RESULT_NUM, g);
			if (results != null) {
				ScoreDoc[] hits = showList(results.scoreDocs, page);
				if (hits != null) {
					HashSet<String> same = new HashSet<String>();
					for (int i = 0; i < hits.length && i < PAGE_RESULT; i++) {
						Document doc = search.getDoc(hits[i].doc);
						String allPath = doc.get("acturalPath");
						int where = allPath.lastIndexOf("/");
						String realName = allPath.substring(where);
						String sameTitle = doc.get("title");
						boolean flag = false;
						if(i != 0){
							for(int j=0; j<i; j++){
								if(realName.indexOf("index") != -1)
									break;
								if(same.contains(realName) || (searchType.equals("html") && same.contains(sameTitle))){
									flag = true;
									break;
								}
							}
						}
						if(flag)
							continue;
						else{
							same.add(realName);
							if(searchType.equals("html"))
								same.add(sameTitle);
						}
						
						String tempContent = doc.get("content");
						contents.add(g.generateContent(tempContent));
						String tempTitle = doc.get("title");
						titles.add(g.generateTitle(tempTitle));
						paths.add(FILE_DIR + doc.get("acturalPath"));
						anchors.add(doc.get("anchor"));
						System.out.println(hits[i].score);
					}

				} else {
					System.out.println("page null");
				}
			}else{
				System.out.println("result null");
			}
			System.out.println("Done");
			request.setAttribute("type", searchType);
			request.setAttribute("currentQuery",queryString);
			request.setAttribute("currentPage", page);
			request.setAttribute("imgContents", contents.toArray(new String[0]));
			request.setAttribute("imgPaths", paths.toArray(new String[0]));
			request.setAttribute("imgTitles", titles.toArray(new String[0]));
			request.setAttribute("imgAnchors", anchors.toArray(new String[0]));
			
			String direction = "/imageshow.jsp";
			if(searchType.equals("pic"))
				direction = "/picshow.jsp";
			request.getRequestDispatcher(direction).forward(request,
					response);
		}
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		this.doGet(request, response);
	}
}
