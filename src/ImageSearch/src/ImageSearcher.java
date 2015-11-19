import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.Weight;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.wltea.analyzer.lucene.IKAnalyzer;
import org.wltea.analyzer.lucene.IKQueryParser;
import org.apache.lucene.queryParser.MultiFieldQueryParser;

import java.util.ArrayList;

public class ImageSearcher {
	private IndexReader reader;
	private IndexSearcher searcher;
	private Analyzer analyzer;
	private float avgLength=1.0f;
	
	public ImageSearcher(String indexdir){
		analyzer = new IKAnalyzer();
		try{
			reader = IndexReader.open(FSDirectory.open(new File(indexdir)));
			searcher = new IndexSearcher(reader);
			searcher.setSimilarity(new SimpleSimilarity());
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public TopDocs searchQuery(String queryString, int maxnum, HTMLGenerator g){
		try {
			/*
			Analyzer anal=new IKAnalyzer(true);       
	        StringReader reader=new StringReader(queryString);  
	        TokenStream ts=anal.tokenStream("", reader);  
	        CharTermAttribute term=ts.getAttribute(CharTermAttribute.class);
	        Float value = 1.0f;
	        StringBuffer sb = new StringBuffer();
	        try{
	        	while(ts.incrementToken()){
	        		  String content = term.toString() + "^" + Float.toString(value);
	        		  sb.append(content);
	        		  if(value > 0.5)
	        			  value = value - 0.1f;
	        	}
	        }catch(IOException e){
	        	e.printStackTrace();
	        }
	        */
			
			// initialize HTML generator
			ArrayList<String> queryTerms = splitQuery(queryString);
			g.setQuery(queryTerms);
			MultiFieldQueryParser parser = new MultiFieldQueryParser(Version.LUCENE_CURRENT, new String[]{"title","content", "anchor"}, new IKAnalyzer(true));
			Query q = parser.parse(queryString);
			System.out.println(q.toString());
			TopDocs results = searcher.search(q, maxnum);
			System.out.println(results);
			return results;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public ArrayList<String> splitQuery(String query) {
		ArrayList<String> queryTerms = new ArrayList<String>();
		Analyzer anal = new IKAnalyzer(true);       
        StringReader reader = new StringReader(query);  
        TokenStream ts = anal.tokenStream("", reader);  
        CharTermAttribute term = ts.getAttribute(CharTermAttribute.class);
        try{
        	while(ts.incrementToken()){  
        		  queryTerms.add(term.toString());
        	}
        }catch(IOException e){
        	e.printStackTrace();
        }
        System.out.println(queryTerms);
        return queryTerms;
	}
	
	public Document getDoc(int docID){
		try{
			return searcher.doc(docID);
		}catch(IOException e){
			e.printStackTrace();
		}
		return null;
	}
	
	public void loadGlobals(String filename){
		try{
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
			String line=reader.readLine();
			avgLength=Float.parseFloat(line);
			reader.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public float getAvg(){
		return avgLength;
	}
	
	public static void main(String[] args){
		ImageSearcher search=new ImageSearcher("forIndex/index");
		/*
		search.loadGlobals("forIndex/global.txt");
		System.out.println("avg length = "+search.getAvg());
		
		TopDocs results=search.searchQuery("�����", 100);
		ScoreDoc[] hits = results.scoreDocs;
		for (int i = 0; i < hits.length; i++) { // output raw format
			Document doc = search.getDoc(hits[i].doc);
			System.out.println("doc=" + hits[i].doc + " score="
					+ hits[i].score+" picPath= "+doc.get("picPath"));
		}
		*/
		//search.test();
	}
}
