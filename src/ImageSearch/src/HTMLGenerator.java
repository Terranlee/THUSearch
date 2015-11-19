import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class HTMLGenerator{

	private static String prefix = "<font color=\'red\'>";
	private static String suffix = "</font>";
	private static int maxLength = 150;
	
	private ArrayList<Pattern> patterns;
	private ArrayList<String> currentQuery;
	
	public String generateTitle(String origin){
		for(String q : currentQuery){
			origin = origin.replaceAll(q, prefix + q + suffix);
		}
		return origin;
	}
	
	public String generateContent(String origin){
		StringBuffer sb = new StringBuffer();
		for(Pattern p : patterns){
			Matcher m = p.matcher(origin);
			while(m.find()){
				sb.append(m.group(1));
				sb.append("...");
				if(sb.length() > maxLength)
					break;
			}
			if(sb.length() > maxLength)
				break;
		}
		
		if(sb.length() < maxLength){
			int delta = maxLength - sb.length();
			int begin = origin.length() - delta;
			if(begin < 0)
				begin = 0;
			sb.append(origin.substring(begin));
		}
		String content = sb.toString();
		for(String q : currentQuery){
			content = content.replaceAll(q, prefix + q + suffix);
		}
		return content;
	}
	
	public void setQuery(List<String> query){
		patterns = new ArrayList<Pattern>();
		currentQuery = new ArrayList<String>();
		for(String q : query){
			//patterns.add(Pattern.compile("[ \t\\x00-\\x80\uFE30-\uFFA0]([^ \\x00-\\x80\t\uFE30-\uFFA0]*?" + q + "[^ \\x00-\\x80\t\uFE30-\uFFA0]*?)[ \\x00-\\x80\t\uFE30-\uFFA0]"));
			patterns.add(Pattern.compile("[ \t。，]([^ \t。，]*?" + q + "[^ \t。，]*?)[ \t。，]"));
			currentQuery.add(q);
		}
	}
	
	public static void main(String[] args){
		String[] query = {"搜索"}; 
		String content = "百度盲搜的推出让看不见光明的盲人也有了走遍天下的希望。百度联合清华大学正在研发的百度盲搜被称为暖科技。盲人朋友可以通过使用盲文与语音两种输入模式进行搜索。并将结果通过智能转化为触点图形。盲人只需通过触摸了解该物体的形状、通过语音、盲文辅助介绍了解该物体详细介绍内容。有了暖科技百度盲搜后";
		HTMLGenerator hg = new HTMLGenerator();
		
		hg.setQuery(Arrays.asList(query));
		String title = hg.generateTitle("强：点燃学生的热情智慧求知之火 - 清华大学新闻网");
		String doc = hg.generateContent(content);
		System.out.println(doc);
	}
}
