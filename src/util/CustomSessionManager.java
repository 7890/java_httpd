package util;
import interfaces.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.IOException;

import java.util.Properties;
import java.util.Vector;

import java.security.MessageDigest;
import java.security.DigestInputStream;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

//tb/1607

/*
session_id: string used to identify a session between browser and server
cookie: living in browser, containing session_id. browser sends cookie in header if not expired and domain matches.
username: a unique login handle to identify a user
password: username and password are the credentials for user authentication
request: every request is initiated by the browser containing none/some/all necessary information to get authorized access to server processing end point
hash: a calculated string, that for a given input will always give the same result 
static_random: a fixed (secret to the server) value
condition: a statement that describes if an authenticated user is authorized to gain access to a certain server processing end point

request_concat = 
	static_random 
	+ request.protocol 
	+ request.scheme 
	+ request.remote_addr 
	+ request.remote_host 
	+ request.is_secure
	+ reqeust.user_agent

password_hash =
	hash(static_random
	+ password)

session_id_pure = 
	hash(request_concat
	+ session.created
	+ password_hash)

session_id_check = 
	first_4(
	hash(static_random
	+ session_id_pure))

session_id =
	session_id_check
	+ session_id_pure


test if session_id is valid before any database access must be made:

	first_4(session_id) == first_4(hash(static_random + session_id_pure)?

if valid, a lookup in the db will tell if a session with that id does exist (and is not expired)
if existing, the user holding that session is fetched to do further checks for authorization

*/

//========================================================================
//========================================================================
public class CustomSessionManager implements interfaces.SessionManager
{
	//this file is loaded if found in current directory
	private String propertiesFileUri="CustomSessionManager.properties";

	//===configurable parameters (here: default values)
	/*
	used for hashing, i.e. user password 'abcd':
		echo -n 'aa987r234hap8=)(/nfd9f87abcd' | sha1sum | tr a-z A-Z
		997885ADEF635172CA11908A47698D10D639795C  -
	*/
	public String static_random="aa987r234hap8=)(/nfd9f87";

	public int cookie_lifetime_s=60; //client side "session"/cookie timeout

	public String jdbc_impl_class="com.mckoi.JDBCDriver";

	//connect to database using TCP
	public String db_connection_url = "jdbc:mckoi://localhost";
	//connect to database locally (file access)
	//public String db_connection_url = "jdbc:mckoi:local://./db.conf"

	public String db_username = "admin";
	public String db_password = "admin";

	public String login_form_file_uri="./resources/login_form.html";
	public String logout_redirect_file_uri="./resources/logout_redirect.html";

	//artificially delay the response by 5 seconds
	public int delay_unauthorized_ms=5000;
	//===end configurable parameters

	private MessageDigest md;

	private Connection db_connection=null;

	private v_user_condition_item user=null;
	private v_session_item session=null;

	private PreparedStatement ps_insert_session;
	private PreparedStatement ps_insert_request;
	private PreparedStatement ps_get_session;
	private PreparedStatement ps_get_session_by_hash;
	private PreparedStatement ps_update_session;
	private PreparedStatement ps_get_user;
	private PreparedStatement ps_get_user_by_login;
	private PreparedStatement ps_check_perms;
	private PreparedStatement ps_next_request_id;

	private PreparedStatement ps_delete_old_sessions;

//========================================================================
	public CustomSessionManager() throws Exception
	{
		if(!LProps.load(propertiesFileUri,this))
		{
			System.err.println("/!\\ could not load properties");
		}

		md=MessageDigest.getInstance("SHA-1"); ///
		DTime.setTimeZoneUTC();

		//we dont't connect here. assuming a handler will call CustomSessionManager.auth, 
		//prefer "late" db connection in method "auth"
		///connectDb();
		///prepareStatements();
	}

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

//=============================================================================
	private void connectDb() throws Exception
	{
		//register the Mckoi JDBC driver. mckoidb.jar must be in classpath
		Class.forName(jdbc_impl_class).newInstance();

		System.err.println("connecting to database...");
		db_connection = DriverManager.getConnection(db_connection_url, db_username, db_password);

		//manually commit. this allows to try commiting until success
		db_connection.setAutoCommit(false);

		System.err.println("have database connection.");

	}//end connectDb()

//=============================================================================
	private void prepareStatements() throws Exception
	{
		ps_next_request_id = db_connection.prepareStatement(
			"SELECT NEXTVAL('seq_tbl_request_id');"
		);

		ps_insert_session = db_connection.prepareStatement(
			"INSERT INTO tbl_session "
			+"(id,id_user,hash,created,last_access,logout) "
			+"VALUES (NEXTVAL('seq_tbl_session_id'),?,?,?,?,0);"
		);

		ps_get_session = db_connection.prepareStatement(
//			"SELECT * FROM v_session_current "
			"SELECT id,id_user,hash,created,last_access,logout,expires_in FROM v_session_current "
			+"WHERE id = ?;"
		);

		ps_get_session_by_hash = db_connection.prepareStatement(
			"SELECT id,id_user,hash,created,last_access,logout,session_expiration_duration_s,expires_in FROM v_session_current "
			+"WHERE hash = ?;"
		);

		ps_update_session = db_connection.prepareStatement(
			"UPDATE tbl_session "
			+"SET last_access = ?, "
			+"logout = ? "
			+"WHERE id = ?;"
		);

		ps_get_user = db_connection.prepareStatement(
			"SELECT id,username,password_hash,id_condition,condition FROM v_user_condition "
			+"WHERE id = ?;"
		);

		ps_get_user_by_login = db_connection.prepareStatement(
			"SELECT id,username,password_hash,id_condition,condition FROM v_user_condition "
			+"WHERE username = ? and password_hash = ?;"
		);

		ps_insert_request  = db_connection.prepareStatement(
			"INSERT INTO tbl_request "
			+"(id,id_user,created,protocol,scheme,remote_addr,remote_host,is_secure,user_agent,server_name,server_port,method,content_length,content_type,uri,context_path,path_info,query_string)"
			+"VALUES (?, ?, TONUMBER(CURRENT_TIMESTAMP), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
		);

		ps_check_perms = db_connection.prepareStatement(
			"SELECT ( ? ) AS result FROM tbl_request "
			+"WHERE id = ?;"
		);

		ps_delete_old_sessions = db_connection.prepareStatement(
			"DELETE FROM tbl_session "
			+"WHERE id IN (SELECT id FROM v_session_expired);"
		);
	}//end prepareStatements()

//=============================================================================
	private void generic_print_rs(ResultSet rs) throws Exception
	{
		ResultSetMetaData rsmd = rs.getMetaData();
		int columnCount = rsmd.getColumnCount();
		//print columns / header
		///...

		//print rows
		while (rs.next())
		{
			for(int i=1;i<=columnCount;i++)
			{
				System.out.print(rs.getString(i)+" ");
			}
			System.out.println("");
		}
	}

//=============================================================================
	private int ps_next_request_id_(
	) throws Exception
	{
		ps_next_request_id.clearParameters();
		ResultSet rs = ps_next_request_id.executeQuery();
		db_connection.commit();
		if(rs.next()){return rs.getInt(1);}else{throw new Exception("next_request_id: error");}
	}

//=============================================================================
	private int ps_insert_session_(
		int id_user
		,String hash
		,long created
		,long last_access
	) throws Exception
	{
		int index=1;
		ps_insert_session.clearParameters();
		ps_insert_session.setInt(index++,id_user);
		ps_insert_session.setString(index++,hash);
		ps_insert_session.setLong(index++,created);
		ps_insert_session.setLong(index++,last_access);

		ResultSet rs = ps_insert_session.executeQuery();
		db_connection.commit();

		if(rs.next()){return rs.getInt(1);}else{return 0;}
	}

//=============================================================================
	private int ps_insert_request_(
		int id
		,int id_user
		,String protocol
		,String scheme
		,String remote_addr
		,String remote_host
		,boolean is_secure
		,String user_agent
		,String server_name
		,int server_port
		,String method
		,int content_length
		,String content_type
		,String uri
		,String context_path
		,String path_info
		,String query_string
	) throws Exception
	{
		int index=1;
		ps_insert_request.clearParameters();
		ps_insert_request.setInt(index++,id);
		ps_insert_request.setInt(index++,id_user);
		ps_insert_request.setString(index++,protocol);
		ps_insert_request.setString(index++,scheme);
		ps_insert_request.setString(index++,remote_addr);
		ps_insert_request.setString(index++,remote_host);
		ps_insert_request.setBoolean(index++,is_secure);
		ps_insert_request.setString(index++,user_agent);
		ps_insert_request.setString(index++,server_name);
		ps_insert_request.setInt(index++,server_port);
		ps_insert_request.setString(index++,method);
		ps_insert_request.setInt(index++,content_length);
		ps_insert_request.setString(index++,content_type);
		ps_insert_request.setString(index++,uri);
		ps_insert_request.setString(index++,context_path);
		ps_insert_request.setString(index++,path_info);
		ps_insert_request.setString(index++,query_string);

		ResultSet rs = ps_insert_request.executeQuery();
		db_connection.commit();

		if(rs.next()){return rs.getInt(1);}else{return 0;}
	}//end ps_insert_request_()

//=============================================================================
	private v_session_item ps_get_session_by_hash_(
		String hash
	) throws Exception
	{
		ps_get_session_by_hash.clearParameters();
		ps_get_session_by_hash.setString(1,hash);

		ResultSet rs = ps_get_session_by_hash.executeQuery();
		db_connection.commit();

		if(rs.next())
		{
			return new v_session_item(rs);
		}
		else
		{
			return null;
		}
	}

//=============================================================================
	private int ps_update_session_(
		int id_session
		,boolean logout
	) throws Exception
	{
		int index=1;
		long nowMillis=DTime.nowMillis();
		ps_update_session.clearParameters();
		ps_update_session.setLong(index++,nowMillis);
		if(logout)
		{
			ps_update_session.setLong(index++,nowMillis);
		}
		else
		{
			ps_update_session.setLong(index++,0);
		}
		ps_update_session.setInt(index++,id_session);

		boolean committed = false;
		ResultSet rs =null;
		while (!committed)
		{
			rs = ps_update_session.executeQuery();
			try
			{
				db_connection.commit();
				committed = true;
			}
			catch (Exception e)
			{
				//e.printStackTrace();
				System.err.println("/!\\ transaction failed, trying again");
			}
			try{Thread.sleep(20);}catch(Exception f){}
		}

		if(rs.next()){return rs.getInt(1);}else{return 0;}
	}//end ps_update_session_()

//=============================================================================
	private int ps_delete_old_sessions_(
	) throws Exception
	{
		ResultSet rs = ps_delete_old_sessions.executeQuery();
		db_connection.commit();

		if(rs.next()){return rs.getInt(1);}else{return 0;}
	}

//=============================================================================
	private v_user_condition_item ps_get_user_(
		int id_user
	) throws Exception
	{
		ps_get_user.clearParameters();
		ps_get_user.setInt(1,id_user);

		ResultSet rs = ps_get_user.executeQuery();
		db_connection.commit();

		if(rs.next())
		{
			return new v_user_condition_item(rs);
		}
		else
		{
			return null;
		}
	}

//=============================================================================
	private v_user_condition_item ps_get_user_by_login_(
		String username
		,String password_hash
	) throws Exception
	{
		int index=1;
		ps_get_user_by_login.clearParameters();
		ps_get_user_by_login.setString(index++,username);
		ps_get_user_by_login.setString(index++,password_hash);

		ResultSet rs = ps_get_user_by_login.executeQuery();
		db_connection.commit();

		if(rs.next())
		{
			return new v_user_condition_item(rs);
		}
		else
		{
			return null;
		}
	}

//=============================================================================
	private boolean ps_check_perms_(
		String condition
		,int id_request
	) throws Exception
	{
		String q="SELECT ("+condition+") AS result FROM tbl_request WHERE id = "+id_request+";";

		System.err.println("check perms query: "+q);

		Statement stmt=db_connection.createStatement();
		ResultSet rs = stmt.executeQuery(q);
		db_connection.commit();

		if(rs.next()){return rs.getBoolean(1);}else{throw new Exception("check_perms: no boolean result found.");}
	}

//=============================================================================
	private boolean basicSessionIdCheck(String sid)
	{
		if(sid==null || sid.length()!=44)
		{
			return false;
		}
		String first4=sid.substring(0,4);
		String sid_pure=sid.substring(4,44);
//		System.err.println("SID FIRST "+first4+" PURE "+sid_pure);
		return first4.equals( toHashString( new StringBuffer(static_random+sid_pure) ).substring(0,4) );
	}

//http://stackoverflow.com/questions/8100634/get-the-post-request-body-from-httpservletrequest
//========================================================================
	public int auth(HttpServletRequest req,HttpServletResponse res) throws Exception
	{
		//from snoop servlet
		System.out.println("Protocol: " + req.getProtocol());
		System.out.println("Scheme: " + req.getScheme());
		System.out.println("Server Name: " + req.getServerName());
		System.out.println("Server Port: " + req.getServerPort());
		System.out.println("Remote Addr: " + req.getRemoteAddr());
		System.out.println("Remote Host: " + req.getRemoteHost());
		System.out.println("Character Encoding: " + req.getCharacterEncoding());
		System.out.println("Content Length: " + req.getContentLength());
		System.out.println("Content Type: "+ req.getContentType());
		System.out.println("Locale: "+ req.getLocale().toString());

		System.out.println("HTTP Method: " + req.getMethod());
		System.out.println("Request URI: " + req.getRequestURI());
		System.out.println("Context Path: " + req.getContextPath());
		System.out.println("Path Info: " + req.getPathInfo());
		System.out.println("Query String: " + req.getQueryString());
		System.out.println("Request Is Secure: " + req.isSecure());
		System.out.println("---");

		user=null;
		session=null;

		Cookie sessionCookie=null;
		String _csid_test=null;

		StringBuffer requestHash;
		String requestHashPart1;
		String encodedHash=null;

		//create hash from request
		requestHash=new StringBuffer(static_random);
		requestHash.append(req.getProtocol());
		requestHash.append(req.getScheme());
		requestHash.append(req.getRemoteAddr());
		requestHash.append(req.getRemoteHost());
		requestHash.append(req.isSecure());
		requestHash.append(req.getHeader("User-Agent"));

		requestHashPart1=requestHash.toString();

		//get session cookie
		Cookie[] cookies=req.getCookies();
		if(cookies !=null)
		{
			for(Cookie cookie : cookies)
			{
				if(cookie.getName().equals("_csid"))
				{
					sessionCookie=cookie;
					_csid_test=cookie.getValue();
					break;
				}
			}
		}

		//no cookie
		if(_csid_test==null)
		{
			System.err.println("NO COOKIE");

			//is there a session id given as param? (i.e. cookie origin issue, cross-domain)
			_csid_test=req.getParameter("_csid");
		}

		//no cookie. example: 797058A14161BEC49BF58E7C258BA37A1E9C52F4XXXX 
		//this means a user is not logged in and has no session
		if(!basicSessionIdCheck(_csid_test))
		{
			//trying to logout without a valid session?
			if(req.getParameter("logout")!=null)
			{
				return -1;
			}

			//does user want to login?
			String username=req.getParameter("username");
			String password=req.getParameter("password");
			if(username==null && password==null)
			{
				//unauthorized: no csid and no user & pw given
				return 0;
			}
			else
			{
				//does a user with that password exist?
				System.out.println("user: " + username + " password_hash: "
					+" "+toHashString(new StringBuffer(static_random+password)));

				///late init
				connectDb();
				prepareStatements();

				user=ps_get_user_by_login_(username
					,toHashString(new StringBuffer(static_random+password))
				);
				if(user==null)
				{
					//unauthorized: no csid and invalid user and/or pw given
					close();
					System.err.println("DELAYING REPLY "+delay_unauthorized_ms);
					try{Thread.sleep(delay_unauthorized_ms);}catch(Exception e){}
					return 0;
				}
				else
				{
					//user exists
					System.err.println(user);

					//create session id as hash
					long nowMillis=DTime.nowMillis();
					requestHash.append(nowMillis);
					requestHash.append(user.password_hash);
					encodedHash=toHashString(requestHash);

					String session_id=toHashString(new StringBuffer(static_random+encodedHash)).substring(0,4)
						+encodedHash;

					System.err.println("new _csid:    "+session_id);
					_csid_test=session_id;

					//creating cookie containing hash as sessionid
					//https://tomcat.apache.org/tomcat-5.5-doc/servletapi/javax/servlet/http/Cookie.html
					sessionCookie = new Cookie("_csid",session_id);
					sessionCookie.setPath("/");
					sessionCookie.setSecure(true);

					//store session in db
					ps_insert_session_(user.id,session_id,nowMillis,nowMillis);

					//cookie will be handed over later
				}//end user not null
			}//end username and pw no null
		}//end have no cookie

		//cookie /session id now (if not returned with login form already)

		System.err.println("cookie _csid: "+_csid_test);

		///late init
		if(db_connection==null)
		{
			connectDb();
			prepareStatements();
		}

		//test if cookie valid (ev. length)
		session=ps_get_session_by_hash_(_csid_test);

		if(session==null)
		{
			//unauthorized: csid invalid and no user & pw given
			sessionCookie = new Cookie("_csid","__invalid");
			sessionCookie.setPath("/");
			sessionCookie.setMaxAge(0);
			res.addCookie(sessionCookie);
			close();
			return 0;
		}
		else
		{
			System.err.println(session);

			//get user from session
			user=ps_get_user_(session.id_user);

			//test if rehash fits session id
			requestHash=new StringBuffer(requestHashPart1);
			requestHash.append(session.created);

			requestHash.append(user.password_hash);
			encodedHash=toHashString(requestHash);

			String session_id=toHashString(new StringBuffer(static_random+encodedHash)).substring(0,4)
				+encodedHash;

			System.err.println("rehash:       "+session_id);

			if(!session_id.equals(session.hash))
			{
				sessionCookie = new Cookie("_csid","__invalid");
				sessionCookie.setPath("/");
				sessionCookie.setMaxAge(0);
				res.addCookie(sessionCookie);
				close();
				return 0;
			}

			boolean allow_access=false;
			int next_request_id=ps_next_request_id_();
			ps_insert_request_(
				next_request_id
				,user.id
				,req.getProtocol()
				,req.getScheme()
				,req.getRemoteAddr()
				,req.getRemoteHost()
				,req.isSecure()
				,req.getHeader("User-Agent")
				,req.getServerName()
				,req.getServerPort()
				,req.getMethod()
				,req.getContentLength()
				,req.getContentType()
				,req.getRequestURI()  // /a/bc
				,req.getContextPath() // /a
				,req.getPathInfo()    // /bc
				,req.getQueryString()
			);

			allow_access=ps_check_perms_(user.condition,next_request_id);//request.id);

			System.err.println("after check_perms: allow_access="+allow_access);

			//maybe an immediate logout of this user / that session is requested
			boolean do_logout=false;
			String logout_param=req.getParameter("logout");
			if(logout_param!=null || !allow_access)
			{
				do_logout=true;
			}

			//update 'last_access' in session
			int count=ps_update_session_(session.id,do_logout);

			if(do_logout)
			{
				sessionCookie = new Cookie("_csid","__invalid");
				sessionCookie.setPath("/");
				sessionCookie.setMaxAge(0); ///
				res.addCookie(sessionCookie);
				close();
				return -1;
			}
			else
			{
				sessionCookie = new Cookie("_csid",session.hash);
				sessionCookie.setPath("/");
				sessionCookie.setMaxAge(cookie_lifetime_s); ///
				res.addCookie(sessionCookie);
				System.err.println("cookie lifetime set to "+cookie_lifetime_s+" sec");
				close();
				return 1;
			}
		}//end session not null
	}//end auth()

//========================================================================
	public v_user_condition_item getUser()
	{
		return user;
	}

//========================================================================
	public v_session_item getSession()
	{
		return session;
	}

//========================================================================
	public String toHashString(StringBuffer sb)
	{
		byte[] encodedHashB=md.digest( (sb.toString()).getBytes() );
		return String.format("%032X", new java.math.BigInteger(1, encodedHashB));
	}

//========================================================================
	public void sendFile(HttpServletResponse res, String file_uri) throws Exception
	{
		System.err.println("sending file "+file_uri);
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
	public void sendLoginForm(HttpServletResponse res) throws Exception
	{
		sendFile(res,login_form_file_uri);
	}

//========================================================================
	public void sendLogoutRedirect(HttpServletResponse res) throws Exception
	{
		sendFile(res,logout_redirect_file_uri);
	}
}//end class CustomSessionManager
//EOF
