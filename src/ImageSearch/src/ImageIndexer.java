import java.io.*;
import java.util.*;


import org.w3c.dom.*;   
import org.wltea.analyzer.lucene.IKAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import javax.xml.parsers.*; 

public class ImageIndexer {
	private Analyzer analyzer; 
    private IndexWriter indexWriter;
    private float averageLength=1.0f;
    
	// static boost parameter, added by Terranlee
	// the value may not be suitable
	private static final float TITLE_BOOST = 10.0f;
	private static final float ANCHOR_BOOST = 50.0f;
	private static final float CONTENT_BOOST = 1.0f;
	
    public ImageIndexer(String indexDir){
    	analyzer = new IKAnalyzer();
    	try{
    		IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_35, analyzer);
    		Directory dir = FSDirectory.open(new File(indexDir));
    		indexWriter = new IndexWriter(dir,iwc);
    		indexWriter.setSimilarity(new SimpleSimilarity());
    	}catch(IOException e){
    		e.printStackTrace();
    	}
    }
    
    
    public void saveGlobals(String filename){
    	try{
    		PrintWriter pw=new PrintWriter(new File(filename));
    		pw.println(averageLength);
    		pw.close();
    	}catch(IOException e){
    		e.printStackTrace();
    	}
    }
	
	/** 
	 * <p>
	 * index sogou.xml 
	 * 
	 */
	public void indexSpecialFile(String filename){
		try{
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();   
			DocumentBuilder db = dbf.newDocumentBuilder();    
			org.w3c.dom.Document doc = db.parse(new File(filename));
			NodeList nodeList = doc.getElementsByTagName("pic");
			for(int i=0;i<nodeList.getLength();i++){
				// changed by Terranlee
				Node node = nodeList.item(i);
				NamedNodeMap map = node.getAttributes();
				
				Node anchorNode = map.getNamedItem("anchor");
				Node contentNode = map.getNamedItem("content");
				Node prNode = map.getNamedItem("pr");
				Node typeNode = map.getNamedItem("type");
				
				// parse the .content file
				// get title and content
				String contentPath = contentNode.getNodeValue();
				FileParser fp = new FileParser();
				fp.initFile(contentPath);
				String title = fp.getTitle();
				String content = fp.getContent();
				
				// get the actural path of this file
				String typeSuffix = typeNode.getNodeValue();
				int where = contentPath.lastIndexOf(".");
				String acturalPath = contentPath.substring(0, where+1) + typeSuffix;
				
				// for doc/pdf/html files, anchor is the related content
				// for pictures, anchor is the url that can direct to the original website
				String anchor = anchorNode.getNodeValue();
				String pagerank = prNode.getNodeValue();
				if(pagerank.equals("")){
					pagerank = "0.000005234415667";
				}
				double temp = Float.parseFloat(pagerank) / 0.000005234415667f;
				float prFloat = (float) (Math.log(temp)) + 1.0f;

				// create fields and add boost
				Document document = new Document();
				Field acturalPathField = new Field("acturalPath" , acturalPath, Field.Store.YES, Field.Index.NO);
				Field titleField = new Field("title", title, Field.Store.YES, Field.Index.ANALYZED);
				titleField.setBoost(TITLE_BOOST);
				Field anchorField = new Field("anchor", anchor, Field.Store.YES, Field.Index.ANALYZED);
				anchorField.setBoost(ANCHOR_BOOST);
				Field contentField = new Field("content", content, Field.Store.YES, Field.Index.ANALYZED);
				contentField.setBoost(CONTENT_BOOST);
				
				averageLength += (title.length() + anchor.length() + content.length());
				
				document.add(acturalPathField);
				document.add(titleField);
				document.add(anchorField);
				document.add(contentField);
				document.setBoost(prFloat);
				indexWriter.addDocument(document);
				if(i%1000==0){
					System.out.println("process "+i);
				}
			}
			averageLength /= indexWriter.numDocs();
			System.out.println("average length = "+averageLength);
			System.out.println("total "+indexWriter.numDocs()+" documents");
			indexWriter.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		ImageIndexer indexer=new ImageIndexer("forIndex/index");
		indexer.indexSpecialFile("input/html.xml");
		indexer.saveGlobals("forIndex/global.txt");
	}
}
