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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class DeviceDetailCrawlerPCpopbyMaker extends Crawler{

	private static HashSet<String> visiteddeviceurls = new HashSet<String>();
    
	private static Lock lock = new ReentrantLock();
	
    private String device_page_encoding = "GBK";
    private String devicelist_page_encoding = "GBK";
    private String sitename = "PCpop";
    private String deviceType = "";
	
	public DeviceDetailCrawlerPCpopbyMaker(String host_m, String startpage_m, int thread_index_m, int thread_num_m, String deviceType_m)
	{
		host = host_m;
		startpage = startpage_m;
		thread_index = thread_index_m;
		deviceType = deviceType_m;
		thread_num = thread_num_m;
	}
	
	@Override
    public String nextPageUrl(String html)
    {
		
		Document doc = Jsoup.parse(html);
    	Elements nexts = doc.select("div.page2 a.here");
    	if(null == nexts || 0 == nexts.size() || nexts.get(0).nextElementSibling() == null)
    	{
    		System.out.println(" 该品牌爬行结束 ！" );
    		return "end";
    	}
    	String nextpageurl = nexts.get(0).nextElementSibling().attr("href");
    	
    	System.out.println( "nextpageurl = " + nextpageurl );
    	return nextpageurl;
    }
	
	@Override
    public HashSet<String> extractURLs(String html)
    {
    	HashSet<String> urls_set = new HashSet<String>();
    	Document doc = Jsoup.parse(html);
    	
    	Elements urls = doc.select("div.p_list div.title a");
    	
    	if( null == urls )
    		return urls_set;
    	
    	System.out.println("productlist size = "+urls.size());
    	
    	lock.lock();
    	for( Element url : urls )
    	{
			String href = url.attr("href");
			if( !visiteddeviceurls.contains(href) )
			{
				visiteddeviceurls.add(href);
				urls_set.add(href);
			}
    	}
 
    	System.out.println(sitename +" visited:"+visiteddeviceurls.size());
    	lock.unlock();
    	
    	return urls_set;
    }
    
	@Override
    public String extractDeviceDetails(String html)
    {
    	Document doc = Jsoup.parse(html);
    	Elements title = doc.select("div#titlenew h1");
    	String detail = "标题:"+( (null == title) ? "" : title.text() )+"\r\n";
    	detail += "时间:"+ new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()) +"\r\n";
    	detail += "设备类型:"+deviceType+"\r\n";
    	
    	Elements paramurl_elements = doc.select("div.zs a:eq(4)");
    	if( null == paramurl_elements )
    		return detail += "参数页面链接元素找不到，有可能网站模板更换\r\n";
    	
    	String paramurl = paramurl_elements.attr("href");
    	
    	html = getHTML( paramurl , device_page_encoding );
    	doc = Jsoup.parse(html);

    	Elements trs = doc.select("table.tab1125 tr");
    	if( null == trs )
    		return detail += "设备参数标签找不到，有可能网站模板更换或者参数页面下载失败\r\n";
    	
    	for(Element tr : trs)
    	{   
    		detail += tr.child(0).text()+":"+tr.child(1).text()+"\r\n";
    	}
    	return detail;

    }
	
	@Override
    public Elements getMakers(String html)
    {
    	return Jsoup.parse(html).select("dl.bor_none dd a");
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
