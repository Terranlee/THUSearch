package thu.search.resource;

import java.io.*;

public class ResourceParser {
	
	private static String output = ".content"; 
	
	private static String[] pdf = {"pdf"};
	private static String[] word = {"doc", "docx"};
	private static String[] ppt = {"ppt", "pptx"};
	private static String[] excel = {"xls", "xlsx"};
	private static String[] html = {"html"};
	
	// get a parser according to file type
	public static ParserBase getParser(String filename){
		int index = filename.lastIndexOf(".")+1;
		if(index == -1){
			System.out.println("do not have prefix:" + filename);
			return null;
		}
		String prefix = filename.substring(index);
		for(String i : pdf)
			if(i.equals(prefix))
				return new PDFParser(filename);
		for(String i : word)
			if(i.equals(prefix))
				return new WORDParser(filename);
		for(String i : ppt)
			if(i.equals(prefix))
				return new PPTParser(filename);
		for(String i : excel)
			if(i.equals(prefix))
				return new EXCELParser(filename);
		/*
		for(String i : html)
			if(i.equals(prefix))
				return new HTMLFileParser(filename);
		*/
		System.out.println("filetype not supported:" + filename);
		return null;
	}
	
	// save the parsed contents to file
	public static void toFile(String filename, String content, String title){
		try{
			content = content.replaceAll("\r|\n", " ");
			title = title.replaceAll("\r|\n", " ");
			FileOutputStream fos = new FileOutputStream(filename); 
		    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos, "UTF-8")); 
			bw.write(title);
			bw.write("\n");
			bw.write(content);
			bw.close();
		}catch(IOException e){
			System.out.println(e);
			System.out.println("error saving content file:" + filename);
		}
	}
	
	public static void parse(String filename){
		ParserBase p = getParser(filename);
		if(p != null){
			int where = filename.lastIndexOf(".");
			String out = filename.substring(0, where) + output;
			String content = p.getContent();
			String title = p.getTitle().substring(1);
			toFile(out, content, title);
		}
	}
	
	public static void main(String[] args){
		//parse("2007.docx");
		//parse("test.doc");
		//parse("DBSCAN Summary.pdf");
		//parse("test.pdf");
		//parse("¹¤×÷²¾.xls");
		//parse("2007.xlsx");
		//parse("9-1 Ò³ÃæÖÃ»»Ëã·¨µÄ¸ÅÄî.pptx");
		//parse("²âÊÔ.ppt");
		//parse("0.html");
		//parse("1.html");
		//parse("6.html");
	}
}
