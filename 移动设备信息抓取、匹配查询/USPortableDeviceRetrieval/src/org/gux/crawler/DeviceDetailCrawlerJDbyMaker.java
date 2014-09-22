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

public class DeviceDetailCrawlerJDbyMaker extends Crawler {

	private static HashSet<String> visiteddeviceurls = new HashSet<String>();
	
	private static Lock lock = new ReentrantLock();
	
	private String device_page_encoding = "GBK";
	private String devicelist_page_encoding = "UTF-8";
	private String sitename = "JD";
	private String deviceType = "";
	
	public DeviceDetailCrawlerJDbyMaker(String host_m, String startpage_m, int thread_index_m, int thread_num_m, String deviceType_m)
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
    	Elements nexts = doc.select("a.next");
    	if(null == nexts || nexts.size() == 0)
    	{
    		System.out.println(" 该品牌爬行结束 ！" );
    		return "end";
    	}
    	String nextpageurl = host+nexts.get(0).attr("href");
    	
    	System.out.println( "nextpageurl = "+nextpageurl );
    	return  nextpageurl;
    }
	@Override
    public HashSet<String> extractURLs(String html)
    {
    	HashSet<String> urls_set = new HashSet<String>();

    	Elements urls = Jsoup.parse(html).select("div#plist div.p-name a");
    	lock.lock();
    	
    	for( Element url : urls)
    	{
			String href = url.attr("href");
			if( !visiteddeviceurls.contains(href) )
				{
					visiteddeviceurls.add(href);
					urls_set.add(href);
				}
    			
    	}
    	lock.unlock();
    	
    	System.out.println(sitename +" visited: "+ visiteddeviceurls.size());
    	return urls_set;
    }
    
    @Override
    public String extractDeviceDetails(String html)
    {
    	Document doc = Jsoup.parse(html);
    	
    	Elements title = doc.select("div#product-intro div#name");

    	String detail = "标题:"+ ( ( title == null ) ? "无":title.text().trim() ) +"\r\n";
    	detail += "时间:"+ new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()) +"\r\n";
    	detail += "设备类型:"+deviceType+"\r\n";
    	
    	if( doc.select("strong#jd-price") != null )
    		detail += "产品价格:" + doc.select("strong#jd-price").text() + "\r\n";
    	
    	Element product_detail = doc.getElementById("product-detail-2");
    	if(product_detail == null )
    		return detail;
    	String detailspart = product_detail.toString();
    	Elements trs = Jsoup.parse(detailspart).getElementsByTag("td");
    	if( null == trs )
    		return detail += "找不到参数标签，有可能网站模板更换\r\n"; 
    	int counter = 1;
    	for(Element e : trs)
    	{   
    		if(counter % 2 == 1 )
    			detail += e.text()+":";
    		else if(counter % 2 == 0)
    			detail += e.text()+"\r\n";
    		++counter;
    	}
    	
    	return detail;
    }
    
    @Override
    public Elements getMakers(String html)
    {
    	return Jsoup.parse(html).select("div[id*=brand_id_] a");
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
