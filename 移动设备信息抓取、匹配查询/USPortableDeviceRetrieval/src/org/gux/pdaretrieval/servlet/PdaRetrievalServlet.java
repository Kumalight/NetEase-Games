package org.gux.pdaretrieval.servlet;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.gux.pdaretrieval.dao.Dao;
import org.gux.pdaretrieval.init.Init;
import org.json.simple.JSONArray;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * Servlet implementation class PdaRetrieval
 */
public class PdaRetrievalServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public PdaRetrievalServlet() {
        super();
        // TODO Auto-generated constructor stub
    }
    public String[] filter(String manufacturer, String model, HashMap<String, List<String>> Synonyms) throws UnsupportedEncodingException
    {
    	if(manufacturer.contains("verizon") && (model.contains("sch") || model.contains("sph") || model.contains("sm")))
			manufacturer = "samsung";
		
		if (Synonyms.get("samsung").contains( manufacturer ) || Synonyms.get("motorola").contains(manufacturer))
			model = model.substring(
					model.indexOf("-") + 1).toLowerCase();
		else
			model = model.toLowerCase();

		if (model.startsWith("htc") && manufacturer.startsWith("htc")) {
			model = model.replace("htc", "").replace("one", "one ");
			manufacturer = "htc";
		}
		
		if(Synonyms.get("htc").contains(manufacturer)) model = model.replace("one", "one ").replace("  ", " ");
		
		if(model.contains("incredibles")) manufacturer = "htc";
		
		if (model.startsWith("lenovo") && manufacturer.startsWith("lenovo")) {
			model = model.replace("lenovo", "");
		}
		if(Synonyms.get("lenovo").contains( manufacturer ))
			model = model.replace("lnv-", "");
		if(manufacturer.equals("lge"))
			model = model.replace("lg-", "");
		if(Synonyms.get("hisense").contains(manufacturer))
			model = model.replace("hs-", "");
		if( Synonyms.get( "xiaomi" ).contains(manufacturer) ) 
		{
			model = model.replace("mi", "");
			model = model.replace("hm", ""); 
			model = model.replace("oneplus", "one plus");
		}
		if( Synonyms.get("huawei").contains(manufacturer)) model = model.replace("mediapad", "mediapad ").replace("x1", "x1 ");
		if(Synonyms.get("huawei").contains(manufacturer) && model.contains("-")) model = model.substring(0,model.indexOf("-"));
		if( manufacturer.startsWith("tcl") ) model = model.replace("tcl_", "");
		if(model.contains("desires")) model = model.replace("desires", "desire s");
		if(model.contains("desirehd") && manufacturer.startsWith("htc")) 
		{
			model = model.replace("desirehd", "desire hd");
			manufacturer = "htc";
		}
		if(Synonyms.get("google").contains(manufacturer))
			{
				model = model.replace("nexus", "nexus ");
				model = model.trim().replace("galaxynexus", "galaxy nexus");
			}
		if(Synonyms.get("sony").contains(manufacturer)) model = model.replace(new String("索尼".getBytes("GBK"),"GBK"), "").replace("xperia", "xperia ").replace("  ", " ");
		if(Synonyms.get("coolpad").contains(manufacturer))
		{
			model = model.replace("coolpad", "");
			manufacturer = "coolpad";
		}
		model = model.replace("_", "");
		
		return new String[]{ manufacturer, model};
    }
    
    public String getsynonymskey(HashMap<String, List<String>> Synonyms, String manufacturer)//根据品牌得到品牌主键
    {
    	for(String key : Synonyms.keySet() )
    	{
    		if(Synonyms.get(key).contains(manufacturer)){
    			return key;
    		}
    	}
    	return null;
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		
		String manufacturer = "", model = "";
		String query = new String(new String(request.getParameter("query").getBytes("iso-8859-1"), "UTF-8").getBytes("GBK"),"GBK").toLowerCase();
		System.out.println("query="+query );
		Init init = Init.getInstance();
		List<String> words = init.seg(query); 
		
		System.out.println("分词结果："+words.toString());
		
		for(String word : words){
			if(init.getbrands().contains(word)){
				manufacturer = word;
				System.out.println("word="+word);
				break;
			}
		}
		for(String word : words){
			if(init.getmodels().contains(word)){
				model = word;
				break;
			}
		}
		
		System.out.println("wordseg, manufacturer="+manufacturer+",model="+model);
		Connection conn = Dao.connect();
		
		HashMap<String, List<String>> Synonyms = init.getSynonyms();//同义词表，key：list格式
		
		String sql = "desc device_cache_info;";
		PreparedStatement pstmt = null;
		ResultSet res = null;
		try {
	
			String[] filtered = filter( manufacturer, model , Synonyms);
			manufacturer      = filtered[0];
			model             = filtered[1];
			System.out.println("filetered, manufacturer="+manufacturer+",model="+model);
			sql = "desc device_cache_info;";
			
			pstmt = conn.prepareStatement(sql);
			res = pstmt.executeQuery();
			ArrayList<String> headers = new ArrayList<String>();
			
			while(res.next())
			{
				headers.add( res.getString(1) );
			}

			String synkey = getsynonymskey( Synonyms, manufacturer);
			
			if( null != synkey ) {
				sql = "select * from device_cache_info where manufacturer = '"+ synkey +"' " +
				          "and model = '" + model +"';";
			}else{
				sql = "select * from device_cache_info where  manufacturer = '"+ manufacturer +"' " +
				          "and model = '" + model +"';";
			}

			System.out.println(new String(sql.getBytes(), "GBK" ));
			
			pstmt = conn.prepareStatement( sql );
			res = pstmt.executeQuery();
			
			JSONObject json = new JSONObject();
			
			JSONObject obj = new JSONObject();
			obj.put("headers",headers);
			
			if( res.next() ){//如果无结果则 摘要和明细均为null
				
				for(int i = 0; i < headers.size(); ++i ){//摘要信息
					String colvalue = res.getBytes(i+1)== null ? "" :new String(new String(res.getBytes(i+1),"GBK").getBytes("UTF-8"),"UTF-8"); 
					obj.put( headers.get(i) , colvalue);
				}
				
				json.put("summary", obj);
				if( null != res.getBytes(headers.size()) ){//如果有url信息，UE人工找到的200个设备里没有url信息，因此对于这的设备只能显示摘要
					
					String[] urls = new String(res.getBytes(headers.size())).split("\\|\\|\\|");
					sql = "desc device_raw_info;";
					
					pstmt = conn.prepareStatement(sql);
					res = pstmt.executeQuery();
					headers.clear();
					while(res.next()){
						headers.add( res.getString(1) );
					}
					
					JSONArray arr = new JSONArray();
					
					for( String url : urls )//从device_raw_info表中查出原始数据
					{
						sql = "select * from device_raw_info where url = '"+ url +"';";
						pstmt = conn.prepareStatement( sql );
						res = pstmt.executeQuery();
						obj = new JSONObject();
						if( res.next() ){
							for(int i = 0; i < headers.size(); ++i )
							{
								obj.put( headers.get(i) , new String(new String(res.getBytes(i+1),"GBK").getBytes("UTF-8"),"UTF-8"));
							}
						}
						arr.add(obj);
					}
					
					json.put("detail", arr);
					
				}else{
					json.put("detail", null);
				}
			}else{
				json.put("summary", null);
				json.put("detail", null);
			}

			System.out.println("json="+json.toJSONString());
			
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");
			response.getWriter().write( json.toJSONString() );
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally{
			try {
				if( null != pstmt )
					pstmt.close();
				if( null != conn)
					conn.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}//end of finally
		System.out.println("get");
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		System.out.println("post");
		
	}

}
