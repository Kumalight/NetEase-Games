package org.gux.pdaretrieval.init;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.gux.pdaretrieval.dao.Dao;

public class Init {
	
    private static final HashSet<String> DIC = new HashSet<String>();
    private static int MAX_LENGTH = 1;
    public static Init wordseg;
    
    private HashSet<String> models = new HashSet<String>();
    private HashSet<String> brands = new HashSet<String>();
    private HashMap<String, List<String>> Synonyms = new HashMap<String, List<String>>();
    
    public static synchronized Init getInstance() { 
        if (wordseg == null) {
            wordseg = new Init();  
        	}
        return wordseg;
    }
    
    public Init(){
    	
    	Connection con = null;
    	PreparedStatement pstmt = null;

        try {
            System.out.println("开始初始化词典");
            int max=1;
            con = Dao.connect();
            pstmt = con.prepareStatement("select distinct( word ) from device_wordseg_dict;");
            ResultSet res = pstmt.executeQuery();
            while(res.next())
            {	
            	String line = new String(res.getBytes(1), "GBK").toLowerCase();
            	DIC.add( line );
            	if( line.length() > max ){
                    max=line.length();
                }
            }
            pstmt = con.prepareStatement("select distinct( word ) from device_models;");
            res = pstmt.executeQuery();
            while(res.next()){	
            	String line = new String(res.getBytes(1), "GBK").toLowerCase();
            	DIC.add( line );
            	models.add( line );
            	if( line.length() > max ){
                    max=line.length();
                }
            }
            pstmt = con.prepareStatement("select distinct( word ) from device_brands;");
            res = pstmt.executeQuery();
            while(res.next()){	
            	String line = new String(res.getBytes(1), "GBK").toLowerCase();
            	DIC.add( line );
            	brands.add( line );
            	if( line.length() > max ){
                    max=line.length();
                }
            }
            MAX_LENGTH = max;
            System.out.println("完成初始化词典，词数目："+DIC.size());
            System.out.println("最大分词长度："+MAX_LENGTH);
            
            //初始化品牌词的同义词表
            pstmt = con.prepareStatement("select * from device_brand_synonyms;");
            res = pstmt.executeQuery();
            while( res.next() ){
				String key = new String(res.getBytes(1),"GBK").toLowerCase();
				//System.out.println("key="+key);
				List<String> list = new ArrayList<String>( Arrays.asList( new String(res.getBytes(2), "GBK" ).toLowerCase().split(",") ) );
				list.add(key);
				Synonyms.put( key , list);
				//System.out.println(key +"  "+ Synonyms.get(key).toString() );
			}
        } catch (IOException ex) {
            System.err.println("词典装载失败:"+ex.getMessage());
        }catch(SQLException ex){
        	System.err.println("词典装载失败:"+ex.getMessage());
        }
        finally{
        	
        	try {
        		if( null != pstmt ){
        			pstmt.close();
        		}	
				if( null != con ){
					con.close();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }
    
    public HashSet<String> getmodels(){
    	return models;
    }
    public HashSet<String> getbrands(){
    	return brands;
    }
    public HashMap<String, List<String>> getSynonyms(){
    	return Synonyms;
    }

    public List<String> seg(String text){//基于正向最长匹配的分词
        List<String> result = new ArrayList<String>();
        while(text.length()>0){
            int len=MAX_LENGTH;
            if( text.length() < len ){
                len=text.length();
            }
            String tryWord = text.substring(0, 0+len);
            while(!DIC.contains(tryWord)){
                if(tryWord.length()==1){
                    break;
                }
                tryWord=tryWord.substring(0, tryWord.length()-1);
            }
            if(tryWord.trim().length() >= 1){
            	result.add(tryWord);
            }
            text=text.substring(tryWord.length());
        }
        return result;
    }
    
    public static void main(String[] args){
    	Init init = Init.getInstance();
        String text = "samsung   i9300";
        for(String s : init.seg(text.toLowerCase() ))
        System.out.println( s );
        //for(String s : DIC)
        	//System.out.println(s);
    }
    
}