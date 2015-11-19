package thu.search.resource;

import java.io.File;  
import java.io.IOException; 

import org.apache.pdfbox.pdmodel.PDDocument;  
import org.apache.pdfbox.util.PDFTextStripper;  

public class PDFParser extends ParserBase {
	
	public PDFParser(String fn){
		super(fn);
	}
	
	@Override
	public String getContent() {
		PDDocument current;
		try{
			current = PDDocument.load(new File(filename));
			PDFTextStripper textStripper = new PDFTextStripper();
			String content = textStripper.getText(current);
			current.close();
			return content;
		}catch(IOException e){
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
