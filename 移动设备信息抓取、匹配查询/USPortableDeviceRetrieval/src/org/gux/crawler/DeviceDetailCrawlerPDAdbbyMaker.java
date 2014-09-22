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

public class DeviceDetailCrawlerPDAdbbyMaker extends Crawler {

	private static HashSet<String> visiteddeviceurls = new HashSet<String>();
	
	private static Lock lock = new ReentrantLock();
	
	private String device_page_encoding = "utf-8";
	private String devicelist_page_encoding = "utf-8";
	private String sitename = "PDAdb";
	private String deviceType = "";
	
	public DeviceDetailCrawlerPDAdbbyMaker(String host_m, String startpage_m, int thread_index_m, int thread_num_m, String deviceType_m )
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
    	Elements nextpageurl = doc.getElementsByAttributeValue("title", "Next page");
    	if(nextpageurl == null || nextpageurl.size() == 0 )
    	{
    		System.out.println(" 该品牌爬行结束 ！" );
    		return "end";
    	}
    		
    	System.out.println("nextpageurl = " + (host + nextpageurl.attr("href")));
    	return host+ nextpageurl.attr("href");
    }
    
    @Override
    public HashSet<String> extractURLs(String html)
    {
    	HashSet<String> urls_set = new HashSet<String>();
    	Elements td = Jsoup.parse(html).getElementsByAttributeValue("colspan", "2");
    	if( null == td )
    		return urls_set;
    	Elements urls = Jsoup.parse(td.get(1).toString()).getElementsByTag("a");
    	if( null == urls )
    		return urls_set;
    	
    	lock.lock();
    	for( Element url : urls)
    	{
			System.out.println("href = "+ host + url.attr("href") );
			String href = host + url.attr("href");
			if( !visiteddeviceurls.contains(href) ){
				visiteddeviceurls.add(href);
				urls_set.add(href);
			}
    	}
    	System.out.println(sitename +"visited:"+ visiteddeviceurls.size());
    	lock.unlock();
    	
    	return urls_set;
    }
    
    @Override
    public String extractDeviceDetails(String html)
    {
    	Document doc = Jsoup.parse(html);
    	Elements tds = doc.getElementsByAttributeValue("valign", "top");
    	if( null == tds || tds.size() == 0 ) return "";
    	String content = tds.get( tds.size() - 1 ).toString();
    	
    	Elements title = Jsoup.parse(content).getElementsByTag("h2");
    	Elements items = Jsoup.parse( content ).getElementsByTag("td");
    	
    	String detail = "标题:"+ ( ( title == null ) ? "无":title.text().trim() ) +"\r\n";
    	
    	detail += "时间:"+ new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()) +"\r\n";
    	detail += "设备类型:"+deviceType+"\r\n";
    	
    	if(null == items)
    		return detail += "找不到参数标签，有可能网站模板更换\r\n"; 
    	
    	for(Element i : items )
    	{
    		if( !i.attr("colspan").equals("2") )
    		{
    			detail += i.text();
    			if( !detail.endsWith(":") ) detail += "\r\n";
    		}
    	}
    	
    	return detail;
    }
    
    @Override
    public Elements getMakers(String html)
    {
    	Elements list = Jsoup.parse(html).getElementsByAttributeValue("valign", "top");
    	Elements makers = Jsoup.parse(list.get(3).toString()).getElementsByTag("a");
    	return makers;
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
