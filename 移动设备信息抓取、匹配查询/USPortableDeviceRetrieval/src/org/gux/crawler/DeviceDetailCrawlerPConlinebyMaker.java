package org.gux.crawler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class DeviceDetailCrawlerPConlinebyMaker extends Crawler{

	private static HashSet<String> visiteddeviceurls = new HashSet<String>();
	private static Lock lock = new ReentrantLock();
	private String device_page_encoding = "GBK";
	private String devicelist_page_encoding = "GBK";
	private String sitename = "PConline";
	private String deviceType = "";

	public DeviceDetailCrawlerPConlinebyMaker(String host_m, String startpage_m, int thread_index_m, int thread_num_m, String deviceType_m)
	{
		host = host_m;
		startpage = startpage_m;
		thread_index = thread_index_m;
		deviceType = deviceType_m;
		thread_num = thread_num_m;
	}
	
	@Override
	public String getHTML(String url, String encoding)
	{
		lock.lock();
		String content = "";
		try{
			Connection conn = Jsoup.connect(url).timeout(10000);
			conn = conn.userAgent("Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
			conn = conn.header("contentType", "charset="+encoding);
			content = conn.get().toString();
			
		}catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			lock.unlock();
		}
		
		return content;
	}
	@Override
    public  String nextPageUrl(String html)
    { 	
    	Document doc = Jsoup.parse(html);
    	Elements currentpage = doc.select("div.pconline_page span");
    	if( 0 == currentpage.size() || currentpage.get(0).nextElementSibling() == null )//没有下一页了
    	{
    		System.out.println(" 该品牌爬行结束 ！" );
    		return "end";
    	}
    	String nextpageurl = host + currentpage.get(0).nextElementSibling().attr("href");
    	System.out.println("nextpageurl = " + nextpageurl );
    	return nextpageurl;
    	
    }
    @Override
    public  HashSet<String> extractURLs(String html)
    {
    	HashSet<String> urls_set = new HashSet<String>();
    	Document doc = Jsoup.parse(html);

    	Elements urls = doc.select("div.hd a.name");
    	
    	if( urls == null )
    		return urls_set;
    	
    	System.out.println("productlist size = "+urls.size());

    	lock.lock();
    	
    	for( Element url : urls)
    	{
			System.out.println(url.attr("href") );
			String href = url.attr("href");
			if( !visiteddeviceurls.contains(href) )
			{
				visiteddeviceurls.add(href);
				urls_set.add(href);
			}
    	}
    	
    	System.out.println(sitename +"visited: "+visiteddeviceurls.size());
    	
    	lock.unlock();
    	
    	return urls_set;
    }
    
    @Override
    public  String extractDeviceDetails(String html)
    {
    	Document doc = Jsoup.parse(html);
    	Elements title_elements = doc.select("div.title");
    	String detail = "标题:"+( ( null == title_elements )? "无":title_elements.text() )+"\r\n";
    	detail += "时间:"+ new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()) +"\r\n";
    	detail += "设备类型:" + deviceType + "\r\n";
    	Elements price = doc.select("em#curPrice");
    	if( price != null )
    		detail += "产品价格:" + price.text() + "\r\n";
    	Elements paramurl_elements = doc.select("div.orgBg ul li:eq(1) a");
    	if( paramurl_elements == null )
    		return detail+= "找不到参数所在页面的URL\r\n";
    	
    	String paramurl = paramurl_elements.attr("href");
    	
    	System.out.println( "param url = " + paramurl );
    	doc = Jsoup.parse( getHTML( paramurl , device_page_encoding ) );
    	
    	Elements itemname = doc.select("table#JparamTable tbody tr th");
    	if( null == itemname )
    		return detail += "找不到参数html元素，有可能是网站模板更换或者参数页面下载失败\r\n";
    	
    	doc.select("em p").remove();
    	
    	for(Element item : itemname)
    	{   
    		detail += item.text()+":"+item.nextElementSibling().text()+"\r\n";
    	}
    	return detail;
    }
    
    @Override
    public Elements getMakers(String html)
    {
    	return Jsoup.parse(html).select("div#J-allBrandList a");
    }
    
    @Override
    public String getdevice_page_encoding()
    {
    	return device_page_encoding;
    }
    @Override
    public String getdevicelist_page_encoding()
    {
    	return devicelist_page_encoding;
    }
    @Override
    public HashSet<String> getvisiteddeviceurls()
	{
		 return visiteddeviceurls;
	}
    @Override
    public String getsitename()
	{
		 return sitename;
	}
    @Override
    public Lock getlock()
    {
    	return lock;
    }
    @Override
    public String getdeviceType()
    {
    	return deviceType;
    }

}
