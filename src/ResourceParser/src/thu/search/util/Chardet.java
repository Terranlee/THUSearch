package thu.search.util;

import java.io.FileInputStream;
import java.io.IOException;

public class Chardet {
	
	public static String codec(String filename){
		String charset = "GBK";
		boolean st = isUtf(filename);
		if(st == true)
			charset = "UTF-8";
		return charset;
	}
	
	private static boolean isUtf(String filePath){
		try{
			FileInputStream fis=new FileInputStream(filePath);
			byte[] bbuf=new byte[1024];
			int L=-1;
			int status=0;
			int oneByteCount=0;
			int twoByteCount=0;
			int threeByteCount=0;
			int fourByteCount=0;
			int errorCount=0;
			while((L=fis.read(bbuf))!=-1){
				for (int i = 0; i <L; i++) {
					byte b=bbuf[i];
					switch (status) {
						case 0:
							if(b>=0&&b<=(byte)0x7F)
									oneByteCount++;
							else if(b>=(byte)0xC0&&b<=(byte)0xDF)
								status=2;
							else if(b>=(byte)0xE0&&b<=(byte)0XEF)
								status=4;
							else if(b>=(byte)0xF0&&b<=(byte)0xF7)
								status=7;
							else 
								errorCount++;
							break;
						case 1:
							break;
						case 2:
							if(b>=(byte)0x80&&b<=(byte)0xBF){
								twoByteCount++;
								status=0;
							}
							else{
								errorCount+=2;
								status=0;
							}
							break;
						case 3:
							break;
						case 4:
							if(b>=(byte)0x80&&b<=(byte)0xBF)
								status=5;
							else{
								errorCount+=2;
								status=0;
							}
							break;
						case 5:
							if(b>=(byte)0x80&&b<=(byte)0xBF){
								threeByteCount++;
								status=0;
							}
							else{
								errorCount+=3;
								status=0;
							}
							break;
						case 7:
							if(b>=(byte)0x80&&b<=(byte)0xBF){
								status=8;
							}
							else{
								errorCount+=2;
								status=0;
							}
							break;
						case 8:
							if(b>=(byte)0x80&&b<=(byte)0xBF){
								status=9;
							}
							else{
								errorCount+=3;
								status=0;
							}
							break;
						case 9:
							if(b>=(byte)0x80&&b<=(byte)0xBF){
								fourByteCount+=4;
								status=0;
							}
							else{
								errorCount++;
								status=0;
							}
							break;
						default:
							break;
					}
				}
			}
			fis.close();
			if(errorCount==0)
				return true;
			return false;
		}catch(IOException e){
			System.out.println(e);
			System.out.println("error when detect codec:" + filePath);
			return true;
		}
	}
}

