package com.tririga.custom;

import com.tririga.pub.workflow.CustomBusinessConnectTask;
import com.tririga.pub.workflow.Record;
import com.tririga.ws.TririgaWS;
import com.tririga.ws.dto.Association;
import com.tririga.ws.dto.AssociationRecord;
import com.tririga.ws.dto.Filter;
import com.tririga.ws.dto.QueryMultiBoResponseColumn;
import com.tririga.ws.dto.QueryMultiBoResponseHelper;
import com.tririga.ws.dto.QueryMultiBoResult;
import com.tririga.ws.dto.QueryResponseColumn;
import com.tririga.ws.dto.QueryResponseHelper;
import com.tririga.ws.dto.QueryResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sql.DataSource;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;

public class APRentRollInvoice implements CustomBusinessConnectTask 
{
  private static final Logger log = LogManager.getLogger(APRentRollInvoice.class);
  
  //private static final String APEIRON_PATH = "/opt/IBM/WebSphere/AppServer/profiles/ctgAppSrv/GlobalDir/CustomerFiles/Outbound/PDI/APEIRON/POINVOICE/toAPEIRON";
  
  private static final String APEIRON_PATH = "/opt/IBM/WebSphere/AppServer/profiles/ctgAppSrv//GlobalDir/CustomerFiles/Outbound/PDI/EFORM";
  
  private HashMap<String, String> data = new HashMap<>();
  
  ArrayList<String> records = new ArrayList<>();
 
  DecimalFormat df = new DecimalFormat("###.##");
  
  public boolean execute(TririgaWS arg0, long arg1, Record[] arg2) 
  {
	
    log.info(" ---> RENT ROLL PROCESS START: RUN");
    
    try 
    {
    	Long RecordId=  arg2[0].getRecordId();
    	
    	//log.info("record id : RUN - "+ RecordId);
    	
    	String project_name = "", module = "triPayment", bo_name= "triProcessPayments",query_name="cstRentRoll - Integration - Retrieve Rent Roll Record";
    	Filter[] filters=new Filter[1];
    	Filter query_filter = new Filter();
    	query_filter.setDataType(320);
    	query_filter.setFieldName("triRecordIdSY");
    	query_filter.setOperator(10);
    	query_filter.setSectionName("General");
    	query_filter.setValue(RecordId +"");
    	filters[0]=query_filter;
    	
    	QueryResult query_result =  arg0.runNamedQuery(project_name,module, bo_name, query_name, filters, 1, 10);
    	
    	//log.info("TRIRIGAWS-"+arg0+" Total Results - "+query_result.getTotalResults());
    	
    	QueryResponseHelper[] queryResponse_helper =  query_result.getQueryResponseHelpers();
    	QueryResponseColumn[] queryResponse_column =  queryResponse_helper[0].getQueryResponseColumns();
    	String RENTROLLID = queryResponse_column[0].getValue();
    	
    	//log.info("RENTROLL ID - "+ RENTROLLID);
    	
    	Association[] Ass_record= arg0.getAssociatedRecords(RecordId, "Has RE Invoice", -1);
    	int i=0;
    	for(i=0;i<Ass_record.length;i++)
    	{
    		CreateAPRentRollInvoice(Ass_record[i].getAssociatedRecordId(), Ass_record[i].getAssociatedRecordName(), arg0);
    	}
		//XMLParser();
	} 
    catch (Exception e)
    {
    	log.error("exception - ", e);
   	}
    
    log.info(" ---> RENT ROLL PROCESS END: RUN");
    return true; 
   }
  
  private void CreateAPRentRollInvoice(long InvoiceRec_ID, String Invoice_RecordName,TririgaWS arg0) throws Exception
  {
	  try
	  {
		  //Invoice variable
		  String RecordId ="",  RecordName = "",  InvoiceNumber = "", InvoiceType = "";
	      String InvoiceStatus = "", InvoiceCurrency = "", PaymentTerm = "", InvoiceDate = "", InvoiceDueDA = "";
	      String RemitToOrgId = "", RemitToOrgName = "", InvoiceVendorID="", VendorNumber = "", VendorRemitRefID = "";
	      String InvoiceAmount = "", PayableTotal  = "",  ReceivableTotal = "", InvoiceTotal = "", 	 InvoiceAccType = "";
		  
	      //Lease Contract variable
		  String LeaseRecordID1 = "",  LeaseRecordName1 = "",  LeaseID	= "",LeaseName =	""; 
		  String BusinessUnit =	"",BUOrganization =	"",CompanyCode = "",CompanyZBE = "",CompanyEntityCode ="";	
		  String LeaseAccCostCenter =	"", LeaseAccCostCenterZBE =	"",RVendorNumber	= "",RemitRefID = "";
		  String RemitRefNumber = "", LeaseStatus = "";
		  
		    
		  // PLI Static String
		  
		  String PLIRecordID = "", PLIRecordName = "", PLIID = "",	PLINAME = "",  PLIStatus = "" , PSRecordID = "";
		  String PSRecordName = "", PSID = "", PSName	= "", PSNumber	= "", PSStatus	= "", PSTaxType = "", PSTaxCodeType = ""; 
		  String PSTaxRate = "", PLIPaymentType	= "", PLISummaryType	= "", PLIAccountingType = "",  PLIAccountingCostCenter	= "";
		  String PLIAccountingCostCode	= "", PLIDueDate = "";
		  String PLILockedforRentRoll = "", PSPaymentType = "", PSSummaryType = "",  PSAccountingType	= "";
		  String PSAccountingCostCode = "", PSExpectedCashAmount = "", PSExpectedAccrualAmount	= "", PSExpectedExpense = "";
		  String PSAmountperYear	= "", PSCurrency = "", PSTaxable = "", PSTaxRate1 = "", PSTotalTaxAmount	= "";
		  String PSTotalCashWithTax = "", PSGLAccountingAssignment	= "", PSGLActivityCode	= "", PSGLCommodityCode = "";	
		  String PSGLCostCenter	= "", PSGLPaymentGLCategory = "", PSGLPaymentType	 = "", PSGLSummaryType	= "";
		  String PSGLName	= "", PSGLID = "";
		  
		  double PLIScheduledCashPayment	= 0.0, PLIAdjustmentAmount = 0.0, PLICreditAmount	= 0.0;
		  double PLIExpectedCashBeforeTax	= 0.0, PLITotalTaxRate	= 0.0, PLITotalTaxAmount1 = 0.0, PLITotalTaxAmount2 = 0.0, PLIExpectedCashwithTax = 0.0;
		  
		  String INVTaxCodeType ="";
		  int INVTaxRate = 0;
		  int INVTaxAmount = 0;
		  int PLI_Count = 0;
		  int PLIExpCashBeforeTax = 0;
		  int PLIExpCashWithTax = 0;
		  int PLITAXAMOUNT = 0;
		  int PLITAXRATE =0;
		  
		   double tPLIAmount1	= 0.0;
		   double tPLIAmount2	= 0.0;
		   double tPLIAmount3	= 0.0;
		   double tPLIAmount4	= 0.0;
		   double tPLIAmount5	= 0.0;
		   double tPLIAmount6	= 0.0;
		   double tPLIAmount7	= 0.0;
		   double tPLIAmount8	= 0.0; 
		  
		  log.info("-----------INVOICE CODE START-------------");
		  //log.info("Invoice Record ID - "+ InvoiceRec_ID);
		  //log.info("Invoice Name - "+ Invoice_RecordName);
		    
		  	String XMLLine1 ="";
		  	String XMLLine2 ="";
		  	String XMLLine3 ="";
		    XMLLine1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+  "\r\n";
	    	XMLLine1 = XMLLine1 + "<INVOICE02><IDOC BEGIN=\"\">" +  "\r\n";
	    	 
	    	 
		    String project_name = "", module = "triPayment", bo_name= "triREInvoice",query_name="cstRentRollInvoice - Integration - Retrieve Rent Roll Invoices";
	    	Filter[] filters=new Filter[1];
	    	Filter query_filter = new Filter();
	    	query_filter.setDataType(320);
	    	query_filter.setFieldName("triRecordIdSY");
	    	query_filter.setOperator(10);
	    	query_filter.setSectionName("General");
	    	query_filter.setValue(InvoiceRec_ID +"");
	    	filters[0]=query_filter;
	    	
	    	QueryResult query_result =  arg0.runNamedQuery(project_name,module, bo_name, query_name, filters, 1, 1000);
	    		    	   	
	    	QueryResponseHelper[] queryResponse_helper =  query_result.getQueryResponseHelpers();
	  
	    	QueryResponseColumn[] queryResponse_column =  queryResponse_helper[0].getQueryResponseColumns();
	    	   	
	    	
	    	 RecordId = queryResponse_column[0].getValue(); 	    	 RecordName = queryResponse_column[1].getValue();
	    	 InvoiceNumber = queryResponse_column[2].getValue(); 	    	 InvoiceType = queryResponse_column[3].getValue();
	    	 InvoiceStatus = queryResponse_column[4].getValue(); 	    	 InvoiceCurrency = queryResponse_column[5].getValue();
	    	 PaymentTerm = queryResponse_column[6].getValue(); 	    	 InvoiceDate = queryResponse_column[7].getValue();
	    	 RemitToOrgId = queryResponse_column[9].getValue();
	    	 RemitToOrgName = queryResponse_column[10].getValue(); 	    	 VendorNumber = queryResponse_column[11].getValue();
	    	 InvoiceAmount = queryResponse_column[13].getValue();
	    	 PayableTotal  = queryResponse_column[14].getValue(); 	    	 ReceivableTotal = queryResponse_column[15].getValue();
	    	 InvoiceTotal = queryResponse_column[16].getValue(); 	    	 InvoiceAccType = queryResponse_column[17].getValue();
	    		 
	    	 
	    	 if(queryResponse_column[7].getValue() == null )
	    	 {
	    		 InvoiceDate = "";
	    		 
	    	 }
	    	 else
	    	 {
	    		 InvoiceDate = queryResponse_column[7].getDisplayValue();
	    		 
	    	 }
	    	 	    	 
	    	 if(queryResponse_column[8].getDisplayValue() == null)
	    	 {
	    		 
	    		 InvoiceDueDA = "";
	    	 }
	    	 else
	    	 {
	    		 InvoiceDueDA = queryResponse_column[8].getDisplayValue();
	    	 }
	    	 
	    	 if(queryResponse_column[12].getDisplayValue() == null)
	    	 {
	    		 
	    		 VendorRemitRefID = "";
	    	 }
	    	 else
	    	 {
	    		 VendorRemitRefID = queryResponse_column[12].getDisplayValue();
	    	 }
	    	 
	    	   
	    	 /* --------------------------------------------
	    	log.info("------INVOICE FIELD LIST START-------");
	    	log.info("RecordId- "+ RecordId );
	    	log.info("RecordName- "+ RecordName );
	    	log.info("InvoiceNumber- "+ InvoiceNumber );
	    	log.info("InvoiceType- "+  InvoiceType );
	    	log.info("InvoiceStatus- "+ InvoiceStatus );
	    	log.info("InvoiceCurrency- "+ InvoiceCurrency );
	    	log.info("PaymentTerm- "+ PaymentTerm );
	    	log.info("InvoiceDate- "+  getDate(InvoiceDate));
	    	log.info("InvoiceDueDA- "+  getDate(InvoiceDueDA));
	    	log.info("RemitToOrgId- "+ RemitToOrgId );
	    	log.info("RemitToOrgName- "+ RemitToOrgName );
	    	log.info("VendorNumber- "+ VendorNumber );
	    	log.info("VendorRemitRefID- "+ VendorRemitRefID );
	    	log.info("InvoiceAmount- "+ InvoiceAmount );
	    	log.info("PayableTotal- "+  PayableTotal );
	    	log.info("ReceivableTotal- "+ ReceivableTotal );
	    	log.info("InvoiceTotal- "+  InvoiceTotal );
	    	log.info("InvoiceAccountingType- "+  InvoiceAccType );
	    	log.info("------INVOICE FIELD LIST END -----");
	    	-----------------------------------------*/
	    	log.info("------INVOICE CODE END-------------");
	    	log.info("------RE LEASE RECORD CODE Start-----");
	    	
	    	Association[] Ass_record= arg0.getAssociatedRecords(InvoiceRec_ID, "Has Contract", -1);
	    	Long LeaseRecordID = Ass_record[0].getAssociatedRecordId();
	    	String LeaseRecordName = Ass_record[0].getAssociatedRecordName();
	    	
	    	//log.info("Lease Record ID-" +LeaseRecordID);
	    	//log.info("Lease Record Name-" +LeaseRecordName);
	    	
	    	String project_name1 = "", module1 = "triContract", bo_name1= "triRealEstateContract",query_name1="cstRentRoll - Integration - Retrieve Real Estate Lease Record";
	    	Filter[] filters1=new Filter[1];
	    	Filter query_filter1 = new Filter();
	    	query_filter1.setDataType(320);
	    	query_filter1.setFieldName("triRecordIdSY");
	    	query_filter1.setOperator(10);
	    	query_filter1.setSectionName("General");
	    	query_filter1.setValue(LeaseRecordID +"");
	    	filters1[0]=query_filter1;
	    	
	    	//QueryResult query_result1 =  arg0.runNamedQuery(project_name1,module1, bo_name1, query_name1, filters1, 1, 10);
	    	
	    	QueryMultiBoResult queryMultiBoResult = arg0.runNamedQueryMultiBo(project_name1, module1, bo_name1, query_name1,filters1, 1, 10000);
	    	int total_count = queryMultiBoResult.getTotalResults();
	        if(total_count==0) 
	        {
	            log.info("No Migrated RE Leases");
	        }
	        else 
	        {
	        	QueryMultiBoResponseHelper[] queryMBOResponse_helpers = queryMultiBoResult.getQueryMultiBoResponseHelpers();
	        	for(QueryMultiBoResponseHelper queryMBOResponse_helper : queryMBOResponse_helpers) 
				{
	        		QueryMultiBoResponseColumn[] queryMBOResponse_columns = queryMBOResponse_helper.getQueryMultiBoResponseColumns();
	        		String temp = "";
					for(QueryMultiBoResponseColumn queryMBOResponse_column : queryMBOResponse_columns) 
					{
						temp = temp + queryMBOResponse_column.getDisplayValue()+"::";
						
					}
	        		
					//log.info(temp);
					
					String [] splitDetails = temp.split("::");
					LeaseRecordID1 = splitDetails[0]; 	    	    	LeaseRecordName1 = splitDetails[1];
	    	    	LeaseID	= splitDetails[2]; 	    	    	LeaseName =	 splitDetails[3];
	    	    	BusinessUnit =	splitDetails[4]; 	    	    	BUOrganization =	splitDetails[5];
	    	    	CompanyCode = splitDetails[6];	    	    	CompanyZBE = splitDetails[7];
	    	    	CompanyEntityCode =	splitDetails[8];	    	    	LeaseAccCostCenter =	 splitDetails[9];
	    	    	LeaseAccCostCenterZBE =	splitDetails[10];	    	    	RVendorNumber	= splitDetails[11];
	    	    	LeaseStatus = splitDetails[14];
	    	    	
	    	    	if(splitDetails[12].trim().contains("null"))
	    	    	{
	    	    		RemitRefID = "";
	    	    	}
	    	    	else
	    	    	{
	    	    		RemitRefID = splitDetails[12];
	    	    		
	    	    	}
	    	    	
	    	    	if(splitDetails[13].trim().contains("null"))
	    	    	{
	    	    		RemitRefNumber = "";
	    	    	}
	    	    	else
	    	    	{
	    	    		RemitRefNumber = splitDetails[13];
	    	    		
	    	    	}
	    	    	
	    	    	if(VendorRemitRefID == "")
	    	    	{
	    	    		if(RemitRefID == "")
	    	    		{
	    	    			if(RemitRefNumber == "")
	    	    			{
	    	    				VendorRemitRefID = "";
	    	    				//log.info("---->  if 3");
	    	    			}
	    	    			else
	    	    			{
	    	    				VendorRemitRefID = RemitRefNumber;
	    	    				//log.info("---->  else 3");
	    	    			}
	    	    		}
	    	    		else
	    	    		{
	    	    			VendorRemitRefID = RemitRefID;
	    	    			//log.info("---->  else 4");
	    	    		}
	    	    	}
	    	    	
	    	    	/* -------------------
	    	    	log.info("-------LEASE RECORD FIELD LIST START -----");
	    	    	log.info("LeaseRecordID - "+ LeaseRecordID1);
	    	    	log.info("LeaseRecordName - "+  LeaseRecordName1);
	    	    	log.info("LeaseID - "+  LeaseID);	
	    	    	log.info("LeaseName - "+ LeaseName);	
	    	    	log.info("BusinessUnit - "+ BusinessUnit);	
	    	    	log.info("BUOrganization - "+BUOrganization);	
	    	    	log.info("CompanyCode - "+ CompanyCode);
	    	    	log.info("CompanyZBE - "+  CompanyZBE);
	    	    	log.info("CompanyEntityCode - "+  CompanyEntityCode);	
	    	    	log.info("LeaseAccCostCenter - "+  LeaseAccCostCenter);	
	    	    	log.info("LeaseAccCostCenterZBE - "+  LeaseAccCostCenterZBE );	
	    	    	log.info("RVendorNumber - "+  RVendorNumber );	
	    	    	log.info("RemitRefID - "+  RemitRefID );
	    	    	log.info("RemitRefNumber - "+ RemitRefNumber  );
	    	    	log.info("LeaseStatus - "+ LeaseStatus );
	    	    	log.info("-------LEASE RECORD FIELD LIST END -----");
	    	    	-------------------------*/
				
				}
	        	       	
	        }
	        
	    	log.info("------RE LEASE RECORD CODE End-----");
	    	log.info("------PLI AND PAYSCHEDULE CODE START-----");
	    	
	    	Association[] Ass_PLIrecord= arg0.getAssociatedRecords(InvoiceRec_ID, "Has Line Item", -1);
	    	int j=0;
	    	for(j=0;j<Ass_PLIrecord.length;j++)
	    	{
	    		//log.info (Ass_PLIrecord[j].getAssociatedRecordId()+" --- "+ Ass_PLIrecord[j].getAssociatedRecordName());
	    		
	    		Long PLI_RECORDID = Ass_PLIrecord[j].getAssociatedRecordId();
	    		String project_name3 = "", module3 = "triCostItem", bo_name3= "triPaymentLineItem",query_name3="cstRentRoll - Integration - Retrieve Invoice PLI and Payment Schedule";
		    	Filter[] filters3=new Filter[1];
		    	Filter query_filter3 = new Filter();
		    	query_filter3.setDataType(320);
		    	query_filter3.setFieldName("triRecordIdSY");
		    	query_filter3.setOperator(10);
		    	query_filter3.setSectionName("General");
		    	query_filter3.setValue(PLI_RECORDID +"");
		    	filters3[0]=query_filter3;
		    	
		    	//QueryResult query_result1 =  arg0.runNamedQuery(project_name1,module1, bo_name1, query_name1, filters1, 1, 10);
		    	
		    	QueryMultiBoResult queryMultiBoResult3 = arg0.runNamedQueryMultiBo(project_name3, module3, bo_name3, query_name3,filters3, 1, 10000);
		    	int total_count3 = queryMultiBoResult3.getTotalResults();
		        if(total_count3==0) 
		        {
		            log.info("No Migrated RE Leases");
		        }
		        else 
		        {
		        	QueryMultiBoResponseHelper[] queryMBOResponse_helpers3 = queryMultiBoResult3.getQueryMultiBoResponseHelpers();
		        	        	
		        	for(QueryMultiBoResponseHelper queryMBOResponse_helper3 : queryMBOResponse_helpers3) 
					{
		        		
		        		QueryMultiBoResponseColumn[] queryMBOResponse_columns3 = queryMBOResponse_helper3.getQueryMultiBoResponseColumns();
		        		String temp = "";
		        		String temp1 = "";
						for(QueryMultiBoResponseColumn queryMBOResponse_column : queryMBOResponse_columns3) 
						{
							
							temp = temp + queryMBOResponse_column.getDisplayValue()+"::";
							temp1 = temp1 + queryMBOResponse_column.getValue()+"::";  
						
						}
		        		
						String [] splitDetails3 = temp.split("::");
						String [] splitDetails4 = temp1.split("::");
						
						 PLIRecordID = splitDetails3[0]; PLIRecordName = splitDetails3[1]; PLIID = splitDetails3[2]; PLINAME = splitDetails3[3];
						 PLIStatus = splitDetails3[4]; PSRecordID = splitDetails3[5]; PSRecordName = splitDetails3[6]; PSID = splitDetails3[7];	
						 PSName	= splitDetails3[8]; PSNumber	= splitDetails3[9]; PSStatus	= splitDetails3[10]; PSTaxType = splitDetails3[11];	
						 PSTaxRate = splitDetails3[13];	 PLIPaymentType	= splitDetails3[14]; PLISummaryType	= splitDetails3[15]; PLIAccountingType = splitDetails3[16];	
						 PLIAccountingCostCenter	= splitDetails3[17]; PLIAccountingCostCode	= splitDetails3[18];
						 PLIDueDate = splitDetails3[19]; PLILockedforRentRoll = splitDetails3[28];
						 PSPaymentType = splitDetails3[29]; PSSummaryType = splitDetails3[30];	
						 PSAccountingType	= splitDetails3[31]; PSAccountingCostCode = splitDetails3[32];
						 PSExpectedCashAmount = splitDetails3[33]; PSExpectedAccrualAmount	= splitDetails3[34];
						 PSExpectedExpense = splitDetails3[35];	PSAmountperYear	= splitDetails3[36];
						 PSCurrency = splitDetails3[37]; PSTaxable = splitDetails3[38];
						 PSTaxRate1 = splitDetails3[39]; PSTotalTaxAmount	= splitDetails3[40];
						 PSTotalCashWithTax = splitDetails3[41]; PSGLAccountingAssignment	= splitDetails3[42];
						 PSGLActivityCode	= splitDetails3[43]; PSGLCommodityCode = splitDetails3[44];	
						 PSGLCostCenter	= splitDetails3[45]; PSGLPaymentGLCategory = splitDetails3[46];	
						 PSGLPaymentType	 = splitDetails3[47]; PSGLSummaryType	= splitDetails3[48];
						 PSGLName	= splitDetails3[49]; PSGLID = splitDetails3[50];
						 
						 PLIScheduledCashPayment	= getNumberOnly(splitDetails4[20]);
						 PLIAdjustmentAmount = getNumberOnly(splitDetails4[21]);  
						 PLICreditAmount	= getNumberOnly(splitDetails4[22]);
						 PLIExpectedCashBeforeTax	= getNumberOnly(splitDetails4[23]); 
						 PLITotalTaxRate	= getNumberOnly(splitDetails4[24]);
						 PLITotalTaxAmount1 = getNumberOnly(splitDetails4[25]); 
						 PLITotalTaxAmount2 = getNumberOnly(splitDetails4[26]);	
						 PLIExpectedCashwithTax = getNumberOnly(splitDetails4[27]); 
						 
						 /*--------------------------
						log.info("PLIRecordID -" +PLIRecordID  ); 	
						log.info("PLIRecordName	-" +PLIRecordName  );
						log.info("PLIID	-" + PLIID  );
						log.info("PLINAME-" + PLINAME  );	
						log.info("PLIStatus-" + PLIStatus );	
						log.info("PSRecordID-" +PSRecordID  );	
						log.info("PSRecordName-" +PSRecordName  );	
						log.info("PSID-" +PSID  );	
						log.info("PSName-" + PSName );
						log.info("PSNumber-" +PSNumber  );	
						log.info("PSStatus-" +PSStatus  );	
						log.info("PSTaxType-" +PSTaxType  );	
						log.info("PSTaxCodeType-" +PSTaxCodeType);	
						log.info("PSTaxRate-" +PSTaxRate  );	
						log.info("PLIPaymentType-" +PLIPaymentType  );	
						log.info("PLISummaryType-" +PLISummaryType  );	
						log.info("PLIAccountingType-" +PLIAccountingType  );	
						log.info("PLIAccountingCostCenter-" +PLIAccountingCostCenter  );	
						log.info("PLIAccountingCostCode-" +PLIAccountingCostCode  );	
						log.info("PLIDueDate-" + PLIDueDate  );	
						log.info("PLIScheduledCashPayment-" + (PLIScheduledCashPayment)  );	
						log.info("PLIAdjustmentAmount-" + (PLIAdjustmentAmount)  );	
						log.info("PLICreditAmount-" + (PLICreditAmount)  );	
						log.info("PLIExpectedCashBeforeTax-" + (PLIExpectedCashBeforeTax)  );	
						log.info("PLITotalTaxRate-" + PLITotalTaxRate );	
						log.info("PLITotalTaxAmount1-" + (PLITotalTaxAmount1)  );	
						log.info("PLITotalTaxAmount2-" + (PLITotalTaxAmount2) );	
						log.info("PLIExpectedCashwithTax-" + (PLIExpectedCashwithTax) );	
						log.info("PLILockedforRentRoll-" + PLILockedforRentRoll  );	
						log.info("PSPaymentType-" + PSPaymentType  );	
						log.info("PSSummaryType-" + PSSummaryType  );	
						log.info("PSAccountingType-" + PSAccountingType  );	
						log.info("PSAccountingCostCode-" +PSAccountingCostCode  );	
						log.info("PSExpectedCashAmount-" + PSExpectedCashAmount  );	
						log.info("PSExpectedAccrualAmount-" +PSExpectedAccrualAmount  );	
						log.info("PSExpectedExpense-" + PSExpectedExpense  );	
						log.info("PSAmountperYear-" +PSAmountperYear  );	
						log.info("PSCurrency-" +PSCurrency  );	
						log.info("PSTaxable-" +PSTaxable  );	
						log.info("PSTaxRate-" +PSTaxRate1  );	
						log.info("PSTotalTaxAmount-" +PSTotalTaxAmount  );	
						log.info("PSTotalCashWithTax-" +PSTotalCashWithTax  );	
						log.info("PSGLAccountingAssignment-" + PSGLAccountingAssignment );	
						log.info("PSGLActivityCode-" +PSGLActivityCode  );	
						log.info("PSGLCommodityCode-" +PSGLCommodityCode  );	
						log.info("PSGLCostCenter-" +PSGLCostCenter  );	
						log.info("PSGLPaymentGLCategory-" +PSGLPaymentGLCategory  );	
						log.info("PSGLPaymentType-" +PSGLPaymentType  );	
						log.info("PSGLSummaryType-" +PSGLSummaryType  );	
						log.info("PSGLName-" +PSGLName  );	
						log.info("PSGLID-" +PSGLID  );
						
						//log.info("PSGLID-" +Integer.parseInt(PLIExpectedCashBeforeTax)  );
						---------------------------------------------*/				
									
						if(PSTaxRate== null || PSTaxRate.isEmpty())
						INVTaxRate = (INVTaxRate + 0);
						else
						INVTaxRate = INVTaxRate + Integer.parseInt(PSTaxRate);
						
						 					 
						 tPLIAmount1 = tPLIAmount1 + PLIScheduledCashPayment;
						 tPLIAmount2 = tPLIAmount2 + PLIAdjustmentAmount;
						 tPLIAmount3 = tPLIAmount3 + PLICreditAmount;
						 tPLIAmount4 = tPLIAmount4 + PLIExpectedCashBeforeTax;
						 tPLIAmount5 = tPLIAmount5 + PLITotalTaxRate;
						 tPLIAmount6 = tPLIAmount6 + PLITotalTaxAmount1;
						 tPLIAmount7 = tPLIAmount7 + PLITotalTaxAmount2;
						 tPLIAmount8 = tPLIAmount8 + PLIExpectedCashwithTax;
						
						
											
					}
		        }
		        PLI_Count = j +1;
		        
		        //--------------------Accounting Assignment T OR K---------------
		        String AccAssignmentNum="";
		        if(PSGLAccountingAssignment.equals("T"))
		        {
		        	AccAssignmentNum = "9999999910";
		        }
		        else
		        {
		        	AccAssignmentNum = "9999999920";
		        }
		        
		        //--------------------TAX Code and Tax Rate---------------
		        String TaxCodeType ="G0";
		        int TaxRate = 0 ;     
		        
		        if(PSTaxCodeType.equals("G1"))
		        {
		        	TaxCodeType = "G1";
		        	TaxRate =10;
		        }
		        else
		        {
		        	TaxCodeType = "G0";
		        	TaxRate =0;
		        }
		        
		        /*-----------------------
		        log.info(" PLI AMOUNt 1 --> " + tPLIAmount1);
		        log.info(" PLI AMOUNt 2 --> " + tPLIAmount2);
		        log.info(" PLI AMOUNt 3 --> " + tPLIAmount3);
		        log.info(" PLI AMOUNt 4 --> " + tPLIAmount4);
		        log.info(" PLI AMOUNt 5 --> " + tPLIAmount5);
		        log.info(" PLI AMOUNt 6 --> " + tPLIAmount6);
		        log.info(" PLI AMOUNt 7 --> " + tPLIAmount7);
		        log.info(" PLI AMOUNt 8 --> " + tPLIAmount8);
		        ------------------------*/
		        
		        XMLLine2 = XMLLine2 + "\r\n";	
		        XMLLine2 = XMLLine2 + "<E1EDP01 SEGMENT=\"\"><POSEX>"+ PLI_Count +"</POSEX></E1EDP01>\r\n";
		        XMLLine2 = XMLLine2 + "<E1EDP19 SEGMENT=\"\"><QUALF>002</QUALF><IDTNR>" + AccAssignmentNum + "</IDTNR></E1EDP19>\r\n";
		        XMLLine2 = XMLLine2 + "<E1EDP26 SEGMENT=\"\"><QUALF>003</QUALF><BETRG>" + PLIExpectedCashBeforeTax +"</BETRG></E1EDP26>\r\n";
		        XMLLine2 = XMLLine2 + "<E1EDP04 SEGMENT=\"\"><MWSKZ>"+ TaxCodeType +"</MWSKZ><MSATZ>" + TaxRate +"</MSATZ><MWSBT>" + PLITotalTaxAmount1 +"</MWSBT></E1EDP04>\r\n";
		        XMLLine2 = XMLLine2 + "<E1EDP30 SEGMENT=\"\"><QUALF>901</QUALF><IVKON>"+ PSAccountingCostCode +"</IVKON></E1EDP30>\r\n";
		        XMLLine2 = XMLLine2 + "<E1EDP30 SEGMENT=\"\"><QUALF>902</QUALF><IVKON>"+ PSAccountingCostCode +"</IVKON></E1EDP30>\r\n";
		        XMLLine2 = XMLLine2 + "<E1EDP30 SEGMENT=\"\"><QUALF>903</QUALF><IVKON>" + PSGLName +"</IVKON></E1EDP30>\r\n";
		        XMLLine2 = XMLLine2 + "<E1EDP30 SEGMENT=\"\"><QUALF>905</QUALF><IVKON>"+ PSGLActivityCode +"</IVKON></E1EDP30>\r\n";
		        XMLLine2 = XMLLine2 + "<E1EDPT1 SEGMENT=\"\"><TDID>SYNT</TDID><TSSPRAS>EN</TSSPRAS>\r\n";
		        XMLLine2 = XMLLine2 + "<E1EDPT2 SEGMENT=\"\"><TDLINE></TDLINE></E1EDPT2></E1EDPT1>\r\n";
		        XMLLine2 = XMLLine2 + "<E1EDPT1 SEGMENT=\"\"><TDID>CONT</TDID><TSSPRAS>EN</TSSPRAS>\r\n";
		        XMLLine2 = XMLLine2 + "<E1EDPT2 SEGMENT=\"\"><TDLINE>"+PLIPaymentType+"</TDLINE></E1EDPT2></E1EDPT1>\r\n";
	    		    	
	    	}
	    	
	    	
	    	log.info("--------PLI AND PS SCHEDULE CODE END-------");
	    	
	    	
	    	if(INVTaxRate==0)
	    	{
	    		INVTaxCodeType="G0";
	    		INVTaxRate =0;
	    	}
	    	else
	    	{
	    		INVTaxCodeType="G1";
	    		INVTaxRate=10;
	    	}
	    	
	    	
	    	    	
	    	if(InvoiceCurrency.equals("Australia Dollars") || InvoiceCurrency.trim().equals("Australia Dollars") )
	    	{
	    		InvoiceCurrency="AUD" ;
	    	}
	    	
	       	if(CompanyCode == null || CompanyCode.isEmpty() || CompanyCode.trim().isEmpty()|| CompanyCode.equals("") || CompanyCode.trim().equals(""))
		  		CompanyCode="TELH";
	       	
	       	if(RemitToOrgId == null || RemitToOrgId.isEmpty() || RemitToOrgId.trim().isEmpty()|| RemitToOrgId.equals("") || RemitToOrgId.trim().equals(""))
	       		RemitToOrgId="00000000";
	       	
	       	if(PaymentTerm == null || PaymentTerm.isEmpty() || PaymentTerm.trim().isEmpty()|| PaymentTerm.equals("") || PaymentTerm.trim().equals(""))
	       		PaymentTerm="ZB30";
	    	
	    	log.info("----XML File Start -----");	
	    	 XMLLine1 = XMLLine1 + "<E1EDK01 SEGMENT=\"\"><CURCY>"+ InvoiceCurrency +"</CURCY><WKURS>1</WKURS><ZTERM>"+ PaymentTerm + "</ZTERM><BSART>INVO</BSART></E1EDK01>\r\n";
	    	 XMLLine1 = XMLLine1 + "<E1EDKA1 SEGMENT=\"\"><PARVW>LI</PARVW><PARTN>"+ RemitToOrgId +"</PARTN><NAME1>" + RemitToOrgName + "</NAME1></E1EDKA1>\r\n";
	    	 XMLLine1 = XMLLine1 + "<E1EDK02 SEGMENT=\"\"><QUALF>009</QUALF><BELNR>" + InvoiceNumber +"</BELNR></E1EDK02> \r\n";
	    	 XMLLine1 = XMLLine1 + "<E1EDK03 SEGMENT=\"\"><IDDAT>012</IDDAT><DATUM>" + getDate(InvoiceDate) +"</DATUM></E1EDK03>\r\n";
	    	 XMLLine1 = XMLLine1 + "<E1EDK05 SEGMENT=\"\"><ALCKZ>+</ALCKZ></E1EDK05>\r\n";
	    	 XMLLine1 = XMLLine1 + "<E1EDK04 SEGMENT=\"\"><MWSKZ>"+ INVTaxCodeType + "</MWSKZ><MSATZ>"+ INVTaxRate +"</MSATZ><MWSBT>60.00</MWSBT></E1EDK04>\r\n";
	    	 XMLLine1 = XMLLine1 + "<E1EDKT1 SEGMENT=\"\"><TDID>SYNT</TDID><TSSPRAS>EN</TSSPRAS>\r\n";
	    	 XMLLine1 = XMLLine1 + "<E1EDKT2 SEGMENT=\"\"><TDLINE>"+ VendorRemitRefID +"</TDLINE></E1EDKT2></E1EDKT1>\r\n";
	    	 XMLLine1 = XMLLine1 + "<E1EDK28 SEGMENT=\"\"><BCOUN>AU</BCOUN></E1EDK28>\r\n";
	    	 XMLLine1 = XMLLine1 + "<E1EDK14 SEGMENT=\"\"><QUALF>011</QUALF><ORGID>"+CompanyCode +"</ORGID></E1EDK14>\r\n";
	    	 XMLLine1 = XMLLine1 + "\r\n";
	    	 XMLLine1 = XMLLine1 + XMLLine2;
	    	 XMLLine1 = XMLLine1 + "\r\n";	    	
	    	 XMLLine3 = XMLLine3 +"<E1EDS01 SEGMENT=\"\"><SUMID>001</SUMID><SUMME>" + PLI_Count + "</SUMME><WAERQ>"+ InvoiceCurrency +"</WAERQ></E1EDS01>\r\n";
	    	 XMLLine3 = XMLLine3 +"<E1EDS01 SEGMENT=\"\"><SUMID>012</SUMID><SUMME>" + tPLIAmount4 + "</SUMME><WAERQ>AUD</WAERQ></E1EDS01>\r\n";
	    	 XMLLine3 = XMLLine3 +"<E1EDS01 SEGMENT=\"\"><SUMID>005</SUMID><SUMME>" + tPLIAmount6 + "</SUMME><WAERQ>AUD</WAERQ></E1EDS01>\r\n";
	    	 XMLLine3 = XMLLine3 +"<E1EDS01 SEGMENT=\"\"><SUMID>011</SUMID><SUMME>" + tPLIAmount8 + "</SUMME><WAERQ>AUD</WAERQ></E1EDS01>\r\n";
	    	 XMLLine3 = XMLLine3 +"<E1EDS01 SEGMENT=\"\"><SUMID>010</SUMID><SUMME>" + tPLIAmount4 + "</SUMME><WAERQ>AUD</WAERQ></E1EDS01>\r\n";
	       	 XMLLine1 = XMLLine1 + XMLLine3;
	    	 XMLLine1 = XMLLine1 + "</IDOC></INVOICE02>";
	    	 
	    	 
	    	 
	    	 records.clear();
	    	 records.add(XMLLine1);
	    	 writeARRReportFile(InvoiceNumber);
	    	 log.info("----XML File End -----");	
	  }
	  catch (Exception e)
	  {
		  log.error("exception - ", e);
	  }
  }
  
  
  
  public void writeARRReportFile(String INVNUMBERData) 
  {
		String records_size=""+records.size();
		String fileName = "TLINV_"+INVNUMBERData+"_"+getDateFormat("yyyyMMdd")+".xml";
		try{
			File source = new File(APEIRON_PATH+"/"+fileName);
			File dest = new File(APEIRON_PATH+"/Backup/"+fileName);
			
			//records.add(0,"H,TRIRIGA,"+strDate+","+records_size+",,,,\r");
			//records.add(records.size(),"F,"+strDate+","+records_size+",,,,\r");
			
			
	    	List<String> lines = records;
			Path file = Paths.get(APEIRON_PATH+"/"+fileName);
			Files.write(file, lines, Charset.forName("UTF-8"));
						
		 	log.info("File written successfully in TLEASE SFTP LOCATION");
		 	Thread.sleep(1000);
		 	
		}catch(Exception e)
		{
			log.error("Exception while writing TLEASE AP INVOICE .XML File:",e);
		}
		INVNUMBERData ="";
		
	}
  
  public String getDateFormat(String dateFormat)
  {
		Date date = new Date();  
	    SimpleDateFormat formatter = new SimpleDateFormat(dateFormat); 
	    String strDate= formatter.format(date); 
	    return strDate;
   }
  
  public String  getDate(String tDate)
  {
	  SimpleDateFormat dateformat = new SimpleDateFormat("yyyyMMdd");
	  tDate = tDate.toString();
	  String strDate ="";
	  if(tDate.equals("") || tDate.trim().equals("") || tDate.equals("null") || tDate.isEmpty() || tDate =="null" || tDate == null)
	  {
		  Date date = new Date();
		  strDate= dateformat.format(date);  
	  }
	  else
	  {
		  long longtime= Long.parseLong(tDate);
		  Date date = new Date(longtime);
		  strDate= dateformat.format(date); 
	  }
	  return strDate;
  }
  
  public double  getNumberOnly(String tAmount)
  {
	  String str="";
	  str= tAmount.replaceAll("[A-Z,]", "");
	  if(str.equals(".00")||str.trim().equals(".00"))
	  {
		  str= "0.0";
	  }
		
	  return Double.parseDouble(str);
  }
  
}
