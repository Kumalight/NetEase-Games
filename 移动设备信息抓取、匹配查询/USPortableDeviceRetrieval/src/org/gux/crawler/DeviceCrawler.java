package org.gux.crawler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

abstract class  Crawler extends Thread {

	 protected String host = "";
	 
	 protected String startpage = "";
	 
	 protected int thread_index = 0;
	 
	 protected int thread_num = 1;
	 
	 public abstract String nextPageUrl(String html);//翻页时，得到下一页URL
	 
	 public abstract HashSet<String> extractURLs(String html);//抽取页面上的设备列表
	 
	 public abstract String extractDeviceDetails(String html);//抽取设备的详细信息
	 
	 public abstract Elements getMakers(String html);//从startpage抓取品牌列表
	 
	 public abstract String getdevice_page_encoding();//得到设备页面的编码格式
	 
	 public abstract String getdevicelist_page_encoding();//得到设备列表页面的编码格式
	 
	 public abstract HashSet<String> getvisiteddeviceurls();//得到已经访问过的URL列表，在一次爬取过程中，意外中断可以重启爬虫
	 
	 public abstract String getsitename();//得到站点名
	 
	 public abstract Lock getlock();//得到同步锁对象
	 
	 public abstract String getdeviceType();//得到设备类型，phone，pad，pda一共三种
	 
	 public String getHTML(String pageURL, String encoding){//通过URl和编码格式下载页面
	        StringBuilder pageHTML = new StringBuilder();
	        HttpURLConnection connection = null;
	        BufferedReader br = null;
	        try {
	            URL url = new URL( pageURL ); 
	          
	            connection = (HttpURLConnection) url.openConnection();
	            connection.setRequestProperty("charset", encoding);
	            connection.setRequestMethod("GET");
	            connection.setConnectTimeout( 100000 );
	            connection.setReadTimeout( 100000 );
	            connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
	            br = new BufferedReader(new InputStreamReader(connection.getInputStream(), encoding)); 
	            String line = null; 
	            while ((line = br.readLine()) != null) {
	                pageHTML.append(line); 
	                pageHTML.append("\r\n"); 
	            }
	            connection.disconnect(); 
	        } catch (Exception e) {
	        	
	            e.printStackTrace();
	        }
	        finally{
	        	try{
	        		if ( null != connection )
		        		connection.disconnect();
		        	if( null != br ) br.close();
	        	}catch(IOException e)
	        	{
	        		e.printStackTrace();
	        	}
	        	
	        }
	        return pageHTML.toString();
	    }
	 
	 @Override
	 public void run()
	 {
		 	BufferedWriter bw = null;
	    	BufferedReader br = null;
	    	BufferedWriter devicepagewriter = null;
	    	
	    	try
	    	{
	            this.getlock().lock();
	            
	    		File f = new File("DeviceURLs__"+ this.getsitename() +"__byMaker.txt");
	            if( !f.exists() ) f.createNewFile();
	            File deviceDetailDir = new File("DeviceDetails__"+ this.getsitename() +"__byMaker");
		        if( !deviceDetailDir.exists() ) deviceDetailDir.mkdir();
		        
	        	bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f,true) ,"utf-8"));

	            br = new BufferedReader(new InputStreamReader(new FileInputStream(f) ,"utf-8") );
	            
	            if( this.getvisiteddeviceurls().size() == 0 )//每个网站的第一个线程已经初始化了已访问集合
	            {
	            	String line = "";
		            while(( line = br.readLine() ) != null)
		            {
		            	this.getvisiteddeviceurls().add(line);
		            }
		            br.close();
	            }
	            
	            this.getlock().unlock();
	            
	        	String html = getHTML( startpage , this.getdevicelist_page_encoding() );
	        	
	        	Elements makers = getMakers( html );
	        	System.out.println("品牌数量为："+makers.size());
	        	
	        	for(Element maker: makers)
	        		System.out.println( host + maker.attr("href") + " "+maker.text() );
	        	
	        		
	        	int min_index = ( makers.size() / thread_num ) * thread_index;
	        	int max_index = min_index + (makers.size() / thread_num);
	        	
	        	max_index = max_index > makers.size() ? makers.size() : max_index;
	        	
	        	if( thread_num - 1 == thread_index )
	        	{
	        		max_index = makers.size();
	        	}
	        	
	        	System.out.println("min_index = "+min_index+" max_index = "+max_index);
	        	
	        	for( int i = min_index; i < max_index; ++i )
	        	{
	        		Element maker = makers.get(i);
	        		System.out.println("Current maker is :"+maker.text()+"@site: "+this.getsitename());
	        		System.out.println("Current maker url is :"+maker.attr("href"));
	        		
	        		if(maker.attr("href").endsWith("TOOKY/")) continue;//此品牌下无数据,网站的问题，需特殊处理
	        		
	        		try{
	        			String tmpurl = maker.attr("href").contains(host) ? maker.attr("href") : (host + maker.attr("href"));
	        			html = getHTML( tmpurl.replaceAll("amp;", ""), this.getdevicelist_page_encoding() );
	        			
	        		}catch(Exception e)
		    		{
		    			e.printStackTrace();
		    			continue;
		    		}
	        		
	            	while( true )
	            	{
           		
	            		HashSet<String> urls_set = extractURLs( html );
	            		for(String url : urls_set)
	            		{
	            			
	            			this.getlock().lock();
	            			bw.write(url+"\r\n");//记录已经爬过的手机URL
	            			bw.flush();
	            			this.getlock().unlock();
	            			
	            			System.out.println("device url = "+ url );
	            			
	            			File devicedetailfile = new File( deviceDetailDir.getName()+ "/" + URLEncoder.encode(url,"UTF-8") + ".txt");
	            			try{
	            				devicedetailfile.createNewFile();
	            			}catch(Exception e)
	            			{
	            				e.printStackTrace();
	            				System.err.println( "错误文件名：" + devicedetailfile.getAbsolutePath() );
	            				continue;
	            			}

	            			devicepagewriter = new BufferedWriter(new OutputStreamWriter( new FileOutputStream( devicedetailfile ), "UTF-8"));
	            			
	            			String devicepage = getHTML( url , this.getdevice_page_encoding() );
	            			
	            			this.getlock().lock();
	            			devicepagewriter.write(extractDeviceDetails( devicepage ) );
	            			devicepagewriter.flush();
	            			devicepagewriter.close();
	            			this.getlock().unlock();
	            			
	            			Thread.sleep( 2000 );
	            		}
	            		
	            		String nextpageurl = nextPageUrl(html);
	            		
	            		if( nextpageurl.startsWith("end") )	break;

	            		try
	            		{
	            			html = getHTML( nextpageurl , this.getdevicelist_page_encoding() );
	            			
	            		}catch(Exception e)
	            		{
	            			e.printStackTrace();
	            			break;
	            		}
	                	System.out.println( nextpageurl );
	                	Thread.sleep( 2000 );
	            	}
	        	}
	        	
	        	bw.write("[sitename= "+this.getsitename()+"][deviceType= "+this.getdeviceType()+"][thread_index= "+ thread_index + "] end![time="+new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date())+"]\r\n");
	        	bw.flush();
	        	bw.close();
	        	
	    	}catch(Exception e)
	    	{
	    		e.printStackTrace();
	    	}
	    	finally
	    	{
	    		try {
	    			
					bw.close();
					br.close();
					devicepagewriter.close();
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    		
	    	}
	 }

}

public class DeviceCrawler {
	
	public static void fork( String className, String host, String home, int thread_index, int thread_num, String deviceType) throws SecurityException, NoSuchMethodException, ClassNotFoundException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException
	{
		Class[] paramsType = { String.class, String.class, int.class, int.class, String.class };
		Object[] params = { host, home, thread_index, thread_num, deviceType };
		Constructor con = Class.forName( className ).getConstructor(paramsType);
		Thread th = (Thread)con.newInstance(params);
		th.start();
	}
	
	public static void startcrawl(String className, String host, String home, int thread_num, String deviceType) throws SecurityException, IllegalArgumentException, NoSuchMethodException, ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException
	{
		for(int thread_index = 0; thread_index < thread_num ; ++thread_index)
		{
			fork(className,host, home, thread_index, thread_num, deviceType);//启动一个线程
		}
	}
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SecurityException, IllegalArgumentException, NoSuchMethodException, InvocationTargetException
	{
	
		int thread_num = 1;
		//太平洋电脑网
		String pconlinePhonehome = "http://product.pconline.com.cn/mobile/";
		String pconlinePhonehost = "http://product.pconline.com.cn";
		
		String pconlineTabletpchome = "http://product.pconline.com.cn/tabletpc/";
		String pconlineTabletpchost = "http://product.pconline.com.cn";
		
		thread_num = 1;
		
		startcrawl("org.gux.crawler.DeviceDetailCrawlerPConlinebyMaker", pconlinePhonehost, pconlinePhonehome, thread_num, "phone");
		
		thread_num = 1;
		startcrawl("org.gux.crawler.DeviceDetailCrawlerPConlinebyMaker", pconlineTabletpchost, pconlineTabletpchome, thread_num, "pad");
		//---------------------------------------------------------------------------------
		//中关村
		String zolPhonehome = "http://detail.zol.com.cn/cell_phone_index/subcate57_list_1.html";
		String zolPhonehost = "http://detail.zol.com.cn";
		
		String zolTablethome = "http://detail.zol.com.cn/tablepc/";
		String zolTablethost = "http://detail.zol.com.cn";
		
		thread_num = 1;
		
		startcrawl("org.gux.crawler.DeviceDetailCrawlerZolbyMaker", zolPhonehost, zolPhonehome, thread_num, "phone");
		
		thread_num = 1;
		startcrawl("org.gux.crawler.DeviceDetailCrawlerZolbyMaker", zolTablethost, zolTablethome, thread_num, "pad");
		
		//---------------------------------------------------------------------------------
		//京东网
		String jdPhonehome = "http://list.jd.com/list.html?cat=9987%2C653%2C655&page=1";
		String jdPhonehost = "http://list.jd.com/list.html";
		
		String jdTablethome = "http://list.jd.com/list.html?cat=670,671,2694";
		String jdTablethost = "http://list.jd.com/list.html";
		
		thread_num = 1;
		
		startcrawl("org.gux.crawler.DeviceDetailCrawlerJDbyMaker", jdPhonehost, jdPhonehome, thread_num, "phone");
		
		thread_num = 1;
		
		startcrawl("org.gux.crawler.DeviceDetailCrawlerJDbyMaker", jdTablethost, jdTablethome, thread_num, "pad");
		
		//---------------------------------------------------------------------------------
		//泡泡网
		String pcpopPhonehome = "http://product.pcpop.com/Mobile/";
		String pcpopPhonehost = "http://product.pcpop.com/";
		
		String pcpopTablethome = "http://product.pcpop.com/tabletpc/";
		String pcpopTablethost = "http://product.pcpop.com/";
		
		thread_num = 1;
		
		startcrawl("org.gux.crawler.DeviceDetailCrawlerPCpopbyMaker", pcpopPhonehost, pcpopPhonehome, thread_num, "phone");
		
		thread_num = 1;
		
		startcrawl("org.gux.crawler.DeviceDetailCrawlerPCpopbyMaker", pcpopTablethost, pcpopTablethome, thread_num, "pad");

		//---------------------------------------------------------------------------------
		//pdadb.net 国外网站
		String pdadbhome = "http://pdadb.net/index.php?m=pdalist";
		String pdadbhost = "http://pdadb.net/";
		
		thread_num = 10;
		startcrawl("org.gux.crawler.DeviceDetailCrawlerPDAdbbyMaker", pdadbhost, pdadbhome, thread_num, "pda");

	}
}