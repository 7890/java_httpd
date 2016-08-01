package util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Properties;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

//tb/1607

//========================================================================
//========================================================================
public class FileDownload
{
	//this file is loaded if found in current directory
	private String propertiesFileUri="FileDownload.properties";

	//===configurable parameters (here: default values)
	//connect to database using TCP
	public String db_connection_url = "jdbc:mckoi://localhost";
	//connect to database locally (file access)
	//public String db_connection_url = "jdbc:mckoi:local://./db.conf"

	public String db_username = "admin";
	public String db_password = "admin";
	//===end configurable parameters

	private Connection db_connection;

	private PreparedStatement ps_get_file_link;

//========================================================================
	public FileDownload() throws Exception
	{
		if(!LProps.load(propertiesFileUri,this))
		{
			System.err.println("/!\\ could not load properties");
		}

		connectDb();
		prepareStatements();
	}

//=============================================================================
	public void close() throws Exception
	{
		if(db_connection!=null)
		{
			db_connection.close();
			System.err.println("connection to database closed.");
		}
		///close more
	}

//=============================================================================
	private void connectDb() throws Exception
	{
		//register the Mckoi JDBC driver. mckoidb.jar must be in classpath
		Class.forName("com.mckoi.JDBCDriver").newInstance();
		System.err.println("connecting to database...");
		db_connection = DriverManager.getConnection(db_connection_url, db_username, db_password);
		System.err.println("have database connection.");
	}//end connectDb()

//=============================================================================
	private void prepareStatements() throws Exception
	{
		ps_get_file_link = db_connection.prepareStatement(
			"SELECT * FROM v_file_link_simple "
			+"WHERE link = ?;"
		);
	}//end prepareStatements()

//=============================================================================
	public FileLinkItem ps_get_file_link_(
		String link
	) throws Exception
	{
		ps_get_file_link.clearParameters();
		ps_get_file_link.setString(1,link);

		ResultSet rs = ps_get_file_link.executeQuery();

		if(rs.next())
		{
			return new FileLinkItem(rs);
		}
		else{return null;}
	}
}//end class FileDownload
//EOF
