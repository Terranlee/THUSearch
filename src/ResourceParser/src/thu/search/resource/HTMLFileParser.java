package thu.search.resource;

import thu.search.util.Chardet;

import java.io.*;
import java.util.regex.*;

public class HTMLFileParser extends ParserBase {
	
	private String contentTitle = "";

	public HTMLFileParser(String fn){
		super(fn);
	}
	
	public void getContentTitle(String htmlStr){
		String titleRegex = "<title>(.*)</title>";
		Pattern reg = Pattern.compile(titleRegex);
		Matcher matcher = reg.matcher(htmlStr);
		if(matcher.find())
			contentTitle = matcher.group(1);
	}
	
	// chinese only
	// can add more limits later
	public String convertHTML(String htmlStr) {
		String chineseRegex = "([\u4e00-\u9fa5]+)";
		Pattern reg = Pattern.compile(chineseRegex);
		Matcher matcher = reg.matcher(htmlStr);
		StringBuffer buffer = new StringBuffer();
		while(matcher.find()){
			buffer.append(matcher.group(1) + " ");
		}
		return buffer.toString();
    }  
	
	@Override
	public String getContent() {
		try{
			String charset = Chardet.codec(filename);
			System.out.println(charset);
			
			BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(filename),charset));
			StringBuffer sb = new StringBuffer();
			String s = br.readLine();
			while(s != null){
				sb.append(s);
				s = br.readLine();
			}
			br.close();
			String htmlStr = sb.toString();
				        
			String content = convertHTML(htmlStr);
			getContentTitle(htmlStr);
			return content;
		}catch(Exception e){
			System.out.println(e);
			System.out.println("error when parsing:" + filename);
			return "";
		}
	}
	
	private String getRawName(){
		int end = filename.lastIndexOf(".");
		int begin = filename.lastIndexOf(File.separator);
		if(begin == -1)
			begin = 0;
		if(end > begin)
			return filename.substring(begin, end);
		else
			return "";
	}
	
	@Override
	public String getTitle(){
		if(contentTitle.equals(""))
			return getRawName();
		return contentTitle;
	}
}
