package thu.search.resource;

import java.io.File;
import java.io.FileInputStream;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.extractor.ExcelExtractor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xssf.extractor.XSSFExcelExtractor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class EXCELParser extends ParserBase {
	
	public EXCELParser(String fn){
		super(fn);
	}
	
	private String getContent2003() throws Exception{
		HSSFWorkbook wb=new HSSFWorkbook(new POIFSFileSystem(new FileInputStream(filename)));
		ExcelExtractor extractor=new ExcelExtractor(wb);
		extractor.setFormulasNotResults(false);
		extractor.setIncludeSheetNames(true);
		String content = extractor.getText();
		extractor.close();
		return content;
	}
	
	private String getContent2007() throws Exception{
		XSSFWorkbook wb = new XSSFWorkbook(new FileInputStream(filename));
		XSSFExcelExtractor extractor = new XSSFExcelExtractor(wb);
		extractor.setFormulasNotResults(false);  
        extractor.setIncludeSheetNames(true);  
        String content = extractor.getText();  
        wb.close();
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
