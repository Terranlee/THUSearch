import java.util.regex.*;
import java.io.*;

public class FileParser{
	
	// base directory of all picture files
	private static String baseName = "F:\\pro\\workspace\\MyHeritrix\\jobs\\files-20150611080340429\\mirror\\news.tsinghua.edu.cn";
	
	private String title;
	private String content;
	
	public FileParser(){
		title = null;
		content = null;
	}
	
	public String getTitle(){
		return title;
	}
	
	public String getContent(){
		return content;
	}
	
	public void initFile(String path){
		String absolutePath = baseName + path;
		File file = null;
		BufferedReader br = null;
		// all the parsed content file is UTF-8 codec
		String codec = "UTF-8";
		try{
			file = new File(absolutePath);
			InputStreamReader isr = new InputStreamReader(new FileInputStream(file), codec);
			br = new BufferedReader(isr);
			title = br.readLine();
			content = br.readLine();
			if(content == null){
				content = "";
				System.out.println("error getting content:" + absolutePath);
			}
			br.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		String filename = "/publish/files/02.2012-2013jpxmrdff.content";
		FileParser fp = new FileParser();
		fp.initFile(filename);
		System.out.println(fp.getTitle());
		System.out.println(fp.getContent());
	}
}
