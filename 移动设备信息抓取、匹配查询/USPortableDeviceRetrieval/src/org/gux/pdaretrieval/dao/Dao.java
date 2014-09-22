package org.gux.pdaretrieval.dao;

import java.sql.DriverManager;
import java.sql.SQLException;

import com.mysql.jdbc.Connection;

public class Dao {

	/**
	 * @param args
	 */
	public static Connection connect() {
		// 驱动程序名
		String driver = "com.mysql.jdbc.Driver";

		// URL指向要访问的数据库名scutcs
		String url = "jdbc:mysql://192.168.34.161/gdas";

		// MySQL配置时的用户名
		String user = "yangwenfeng";

		// MySQL配置时的密码
		String password = "ywf";
		Connection conn = null;
		try{
			Class.forName(driver);

			// 连续数据库
			conn = (Connection) DriverManager.getConnection(url, user,
					password);

			if (!conn.isClosed())
				System.out.println("Succeeded connecting to the Database!");

		}catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return conn;

	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
			Dao.connect();
		
	}

}
