package thu.search.resource;

import java.io.File;

public class Walker {
	
	private static String[] names = {"pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx"};
	
	public static void walk(String filename){
        File root = new File(filename);
        File[] files = root.listFiles();
        for(File file : files){
            if(file.isDirectory()){
                walk(file.getAbsolutePath());
            }
            else{
            	String name = file.getAbsolutePath();
            	String prefix = name.substring(name.lastIndexOf(".")+1);
            	for(String pre : names){
            		if( prefix.equals(pre)){
            			ResourceParser.parse(name);
            		}
            	}
            }
        }
    }

	public static void main(String[] args){
		String root_dir = "F:\\pro\\workspace\\MyHeritrix\\jobs\\files-20150611080340429\\mirror\\news.tsinghua.edu.cn\\publish\\files";
		walk(root_dir);
	}
}
