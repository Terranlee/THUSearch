package thu.search.resource;

public abstract class ParserBase {
	
	protected String filename;
	
	public ParserBase(String fn){
		filename = fn; 
	}
	
	public String getFilename(){
		return filename;
	}
	
	public abstract String getContent();
	public abstract String getTitle();
}
