package thu.search.resource;

import java.io.File;
import java.io.FileInputStream;

import org.apache.poi.POIXMLDocument;
import org.apache.poi.POIXMLTextExtractor;
import org.apache.poi.hwpf.extractor.WordExtractor;  
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;

public class WORDParser extends ParserBase {
	
	public WORDParser(String fn){
		super(fn);
	}
	
	private String getContent2003() throws Exception{
		WordExtractor ex = new WordExtractor(new FileInputStream(new File(filename)));
		String content = ex.getText();
		ex.close();
		return content;
	}
	
	private String getContent2007() throws Exception{
		OPCPackage opcPackage = POIXMLDocument.openPackage(filename);  
        POIXMLTextExtractor extractor = new XWPFWordExtractor(opcPackage);  
        String content = extractor.getText();
        extractor.close();
        return content;
	}
	
	@Override
	public String getContent() {
		try{
			int l = filename.length();
			if(filename.charAt(l-1) == 'x')
				return getContent2007();
			else
				return getContent2003();
		}catch(Exception e){
			System.out.println(e);
			System.out.println("error when parsing:" + filename);
			return "";
		}
	}
	
	@Override
	public String getTitle(){
		int end = filename.lastIndexOf(".");
		int begin = filename.lastIndexOf(File.separator);
		if(begin == -1)
			begin = 0;
		if(end > begin)
			return filename.substring(begin, end);
		else
			return "";
	}
}
