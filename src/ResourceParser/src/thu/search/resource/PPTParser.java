package thu.search.resource;

import java.io.File;
import java.io.FileInputStream;

import org.apache.poi.hslf.extractor.PowerPointExtractor;
import org.apache.poi.xslf.extractor.XSLFPowerPointExtractor;
import org.apache.poi.xslf.usermodel.XMLSlideShow;

public class PPTParser extends ParserBase {
	
	public PPTParser(String fn){
		super(fn);
	}
	
	private String getContent2007() throws Exception{
		XMLSlideShow xmlslideshow = new XMLSlideShow(new FileInputStream(filename));
	    XSLFPowerPointExtractor ppt = new XSLFPowerPointExtractor(xmlslideshow);
	    String content = ppt.getText();
	    ppt.close();
	    return content;
	}
	
	private String getContent2003() throws Exception{
		PowerPointExtractor extractor=new PowerPointExtractor(new FileInputStream(filename));
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
