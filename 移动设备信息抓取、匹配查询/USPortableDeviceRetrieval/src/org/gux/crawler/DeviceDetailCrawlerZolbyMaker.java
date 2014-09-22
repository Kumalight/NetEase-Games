package org.gux.crawler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class DeviceDetailCrawlerZolbyMaker extends Crawler{

	
	private static HashSet<String> visiteddeviceurls = new HashSet<String>();
	
	private static Lock lock = new ReentrantLock();
	
	private String sitename = "Zol";
	private String device_page_encoding = "GBK";
	private String devicelist_page_encoding = "GBK";
	private String deviceType = "";
	
	public DeviceDetailCrawlerZolbyMaker(String host_m, String startpage_m , int thread_index_m, int thread_num_m, String deviceType_m)
	{
		host = host_m;
		startpage = startpage_m;
		thread_index = thread_index_m;
		deviceType = deviceType_m;
		thread_num = thread_num_m;
	}
    
    public String nextPageUrl(String html)
    {
    	Document doc = Jsoup.parse(html);
    	Elements currentpage = doc.select("div.pagebar span.sel");
    	if( 0 == currentpage.size() || currentpage.get(0).nextElementSibling() == null )//没有下一页了
    	{
    		System.out.println(" 该品牌爬行结束 ！" );
    		return "end";
    	}
    	String nextpageurl = host + currentpage.get(0).nextElementSibling().attr("href");
    	System.out.println("nextpageurl = " + nextpageurl );
    	return nextpageurl;
    }
    
    public HashSet<String> extractURLs(String html)
    {
    	HashSet<String> urls_set = new HashSet<String>();
    	Document doc = Jsoup.parse(html);

    	Elements urls = doc.select("div.pro-intro h3 a:eq(0)");
    	if( null == urls )
    		return urls_set;
    	
    	lock.lock();
    	for( Element url : urls)
    	{
    			System.out.println(url.attr("href") );
    			String href = host + url.attr("href");
    			if( !visiteddeviceurls.contains(href) )
    				{
    					visiteddeviceurls.add(href);
    					urls_set.add(href);
    				}
    	}
    	
    	System.out.println(sitename + " visited: "+visiteddeviceurls.size());
    	lock.unlock();
    	return urls_set;
    }
    
    public String extractDeviceDetails(String html)
    {
    	Document doc = Jsoup.parse(html);
    	
    	Elements title = doc.select("div.ptitle");
    	
    	String detail = "标题:"+ ( ( title == null ) ? "无":title.text().trim() ) +"\r\n";
    	
    	detail += "时间:"+ new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()) +"\r\n";
    	detail += "设备类型:"+deviceType+"\r\n";
    	
    	if(doc.select("#J_PriceTrend") != null )
    		detail += "产品价格:"+doc.select("#J_PriceTrend").text()+"\r\n";
    	
    	Elements paramurl_elements = doc.select("ul.nav li:eq(3) a");
    	if ( null == paramurl_elements)
    		return detail += "参数页面找不到，有可能网站模板更换\r\n";
    	String paramurl = host + paramurl_elements.attr("href");
    	String parampage = getHTML( paramurl , device_page_encoding );

    	Document parampage_doc = Jsoup.parse(parampage);
    	Elements paramnames = parampage_doc.select("span.param-name");
    	
    	if( null == paramnames)
    		return detail += "参数页面找不到参数标签，有可能网站模板更换或者参数页面下载失败\r\n";
    	
    	for(Element e : paramnames)
    	{   
    		detail += e.text()+":"+e.nextElementSibling().text()+"\r\n";
    	}
    	
    	return detail;
    	
    }
    
    @Override
    public Elements getMakers(String html)
    {
    	return Jsoup.parse(html).select("div.all-brand-list a");
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
