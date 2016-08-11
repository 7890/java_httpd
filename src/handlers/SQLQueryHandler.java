package handlers;
import util.*;
import util.formatter.*;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Properties;

import java.io.IOException;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

import com.mckoi.util.ResultOutputUtil;

//tb/1608

//========================================================================
//========================================================================
public class SQLQueryHandler extends AbstractHandler
{
	//this file is loaded if found in current directory
	private static String propertiesFileUri="SQLQueryHandler.properties";

	//===configurable parameters (here: default values)
	//connect to database using TCP
	public String db_connection_url = "jdbc:mckoi://localhost";
	//connect to database locally (file access)
	//public String db_connection_url = "jdbc:mckoi:local://./db.conf"

	public String db_username = "admin";
	public String db_password = "admin";

	public String Access_Control_Allow_Origin	="*";
	//public String Access-Control-Allow-Origin	="null";

	public String sql_query_html_form="./resources/sql_query_form.html";
	//===end configurable parameters

	private Connection db_connection=null;

	private CSVRSFormatter csv = new CSVRSFormatter();
	private HTMLRSFormatter html = new HTMLRSFormatter(true); ///
	private HTMLStyledRSFormatter html_styled = new HTMLStyledRSFormatter(true); ///

//========================================================================
	public SQLQueryHandler()
	{
		if(!LProps.load(propertiesFileUri,this))
		{
			System.err.println("/!\\ could not load properties");
		}
	}

//========================================================================
	public void sendFile(HttpServletResponse res, String file_uri) throws IOException
	{
		System.err.println("sending file "+file_uri);
		//deliver all-in-one html form
		res.setHeader("Content-Type", "text/html");
		OutputStream os=res.getOutputStream();
		InputStream is=new FileInputStream(file_uri);

		byte[] buf = new byte[4096];
		for (int nChunk = is.read(buf); nChunk!=-1; nChunk = is.read(buf))
		{
			os.write(buf, 0, nChunk);
		}
		os.close();
		is.close();
	}

//========================================================================
	private void sendSQLQueryForm(HttpServletResponse res) throws IOException
	{
		sendFile(res,sql_query_html_form);
	}

//========================================================================
	public void handle(String target,
			Request baseRequest,
			HttpServletRequest request,
			HttpServletResponse response ) throws IOException
	{
		String ruri=request.getPathInfo();

		if(request.getPathInfo().equals("/")
			|| request.getPathInfo().equals("/query_form")
		)
		{
			sendSQLQueryForm(response);
			baseRequest.setHandled(true);
			return;
		}

		if(request.getMethod()=="OPTIONS" && ruri.equals("/query"))
		{
			System.err.println("got OPTIONS request");
			response.setHeader("Access-Control-Allow-Origin", Access_Control_Allow_Origin);
//			response.setHeader("Access-Control-Allow-Headers", Access_Control_Allow_Headers);
			response.setHeader("Access-Control-Allow-Methods", "POST,OPTIONS");
			baseRequest.setHandled(true);
			return;
		}
		else if(request.getMethod()=="POST" && ruri.equals("/query"))
		{
			response.setHeader("Access-Control-Allow-Origin", Access_Control_Allow_Origin);

			String sql=request.getParameter("text-sql");
			if(sql==null)
			{
				///return error...
				baseRequest.setHandled(true);
				return;
			}

			try
			{
				System.err.println("sql: "+sql);
				///need to parse, filter comments etc like execsql...

				connectDb();
				ResultSet rs=db_connection.createStatement().executeQuery(sql);
				ResultSetMetaData rsmd = rs.getMetaData();
				int columnCount = rsmd.getColumnCount();

				boolean inline=false;
				String displayName="Output";

				String output=request.getParameter("select-output");
				if(output==null)
				{
					output="0";
				}

				if(output.equals("0"))//same page
				{
				}
				else if(output.equals("1"))//new window
				{
					inline=true;
					String dispo=(inline ? "inline" : "attachment");
					response.setHeader("Content-Disposition", dispo+";filename=\"" + displayName + "\"");
				}
				else if(output.equals("2"))//download
				{
					inline=false;
					String dispo=(inline ? "inline" : "attachment");
					response.setHeader("Content-Disposition", dispo+";filename=\"" + displayName + "\"");
				}

				OutputStreamWriter osw=new OutputStreamWriter(response.getOutputStream());

				String format=request.getParameter("select-format");
				if(format==null)
				{
					format="0";
				}

				if(format.equals("0"))//text table
				{
					response.setHeader("Content-Type", "text/plain");
					PrintWriter pw=new PrintWriter(osw);
					ResultOutputUtil.formatAsText(rs, pw);
					pw.close();
				}
				else if(format.equals("1"))//html table
				{
					response.setHeader("Content-Type", "text/html");
					html.formatRS(rs,osw);
				}
				else if(format.equals("2"))//html styled div table
				{
					response.setHeader("Content-Type", "text/html");
					html_styled.formatRS(rs,osw);
				}
				else if(format.equals("3"))//csv
				{
					if(output.equals("2"))//download
					{
						response.setHeader("Content-Type", "text/csv");
					}
					else
					{
						response.setHeader("Content-Type", "text/plain");
					}

					csv.formatRS(rs,osw);
				}

				else if(format.equals("4"))//xml
				{
					response.setHeader("Content-Type", "text/xml");
					///dummy
				}
				rs.close();
				osw.close();
				close();
				baseRequest.setHandled(true);
			}
			catch(Exception e)
			{
				e.printStackTrace();
				throw new IOException(e);
			}
		}//end if method POST
	}//end handle()

//=============================================================================
	private void connectDb() throws Exception
	{
		//register the Mckoi JDBC driver. mckoidb.jar must be in classpath
		Class.forName("com.mckoi.JDBCDriver").newInstance();
		System.err.println("connecting to database...");
		db_connection = DriverManager.getConnection(db_connection_url, db_username, db_password);

		//manually commit. this allows to try commiting until success
		db_connection.setAutoCommit(false);

		System.err.println("have database connection.");

	}//end connectDb()

//=============================================================================
	public void close() throws Exception
	{
		if(db_connection!=null)
		{
			db_connection.close();
			System.err.println("connection to database closed.");
		}
		///ev. close more
	}
}//end class SQLQueryHandler
//EOF