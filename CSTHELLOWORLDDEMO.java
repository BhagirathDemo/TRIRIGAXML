package com.tririga.custom;

import com.tririga.pub.workflow.CustomBusinessConnectTask;
import com.tririga.pub.workflow.Record;
import com.tririga.ws.TririgaWS;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.awt.List;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;


public class CSTHELLOWORLDDEMO implements CustomBusinessConnectTask
{

	public static final Logger log= LogManager.getLogger(CSTHELLOWORLDDEMO.class);
	
	private static final String APEIRON_PATH = "C:/TRIRIGA";
	ArrayList<String> records = new ArrayList<>();
	 private HashMap<String, String> data = new HashMap<>();
	
	 public boolean execute(TririgaWS arg0, long arg1,Record[] arg2)
	 {
		 log.info("Hello World ---> Execute Method Start");
		 try 
		 {
			 log.info("Hello World ---> TRY BLOCL");
			 log.info("Hello World ---> Day 2 Database Connect and Execute SQL query Start");
			 getdetails();
			 
			 log.info("Hello World ---> Day 2 Database Connect and Execute SQL query End");
		 }
		 catch(Exception e)
		 {
			 log.info("Hello World ---> Exception ", e);
		 }
		 
		 log.info("Hello World ---> Execute Method End");
		 return true;
	 }
	 
	 private void getdetails()
	 {
		Connection conn= null;
		Statement stmt =null;
		ResultSet rs = null;
		
		try
		{
			conn=getDBConectionFromPool();
			stmt=conn.createStatement();
			rs=stmt.executeQuery("SELECT COUNT(*) FROM T_TRIREALESTATECONTRACT");
			while(rs.next())
			{
			 log.info("SQl Query Output --> TOTAL Real Estate Record Count : " + rs.getString(1));
			 records.clear();
	    	 records.add("Total_RecordCount "+ rs.getString(1));
			continue;
			}
			
			String records_size=""+records.size();
			String fileName = "FileExtractDemo.csv";
			
				File source = new File(APEIRON_PATH+"/"+fileName);
				File dest = new File(APEIRON_PATH+"/Backup/"+fileName);
				
				//records.add(0,"H,TRIRIGA,"+strDate+","+records_size+",,,,\r");
				//records.add(records.size(),"F,"+strDate+","+records_size+",,,,\r");
				
				
		    	ArrayList<String> lines = records;
				Path file = Paths.get(APEIRON_PATH+"/"+fileName);
				Files.write(file, lines, Charset.forName("UTF-8"));
							
			 	log.info("File written successfully in TLEASE SFTP LOCATION");
			 	Thread.sleep(1000);
			 	
		}
		catch(Exception e)
		{
			log.info("getdetails : " + e);	
		}
	 }
	 
	 private static Connection getDBConectionFromPool()
	 {
		 Connection conn = null;
		 try 
		 {
			 Context ctx = new InitialContext();
			 DataSource ds=(DataSource)ctx.lookup("jdbc/local/DataSource-TRIRIGA-data");
			 conn = ds.getConnection();
		 }
		 catch(Exception e)
		 {
			 log.info("getDBConectionFromPool : " + e);
		 }
		 return conn; 
	 }
	 
	
}
