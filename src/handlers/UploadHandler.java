package handlers;
import util.*;
import interfaces.*;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.File;

import java.io.InputStream;
import java.io.OutputStream;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import java.security.SecureRandom;

import java.util.Enumeration;
import java.util.Properties;

import java.lang.reflect.Constructor;

//tb/1606
//all-in-one upload html form is included in this handler
//http://localhost:8081/upload_form

//========================================================================
//========================================================================
public class UploadHandler extends AbstractHandler
{
	//this file is loaded if found in current directory
	private String propertiesFileUri="UploadHandler.properties";

	//===configurable parameters (here: default values)
	public String upload_html_form			="resources/upload_form.html";
	public String download_dir			="/tmp";
	public String Access_Control_Allow_Origin	="*";
	//public String Access-Control-Allow-Origin	="null"; //html loaded from anywhere, server running on localhost
	public String Access_Control_Allow_Headers	="content-type,x_filename";

	public boolean post_upload_hook_enabled		=false;
	public String post_upload_hook			="hooks.PostUploadShellHook";
	//===end configurable parameters

	private static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	private static SecureRandom rnd = new SecureRandom();

	private PostUpload post;

//========================================================================
	public UploadHandler() throws Exception
	{
		if(!LProps.load(propertiesFileUri,this))
		{
			System.err.println("/!\\ could not load properties");
		}

		if(post_upload_hook_enabled)
		{
			try
			{
				System.err.println("loading PostUpload class");
				Class<?> c = Class.forName(post_upload_hook);
				Constructor<?> cons = c.getConstructor();
				post=(PostUpload)cons.newInstance();
			}
			catch(Exception e)
			{
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

//http://stackoverflow.com/questions/41107/how-to-generate-a-random-alpha-numeric-string
//========================================================================
	String randomString(int len)
	{
		StringBuilder sb = new StringBuilder( len );
		for(int i = 0; i < len; i++ )
		{
			sb.append(AB.charAt(rnd.nextInt(AB.length())));
		}
		return sb.toString();
	}

//========================================================================
	private void sendXMLResponse(HttpServletResponse response, String content) throws IOException
	{
		//https://bugzilla.mozilla.org/show_bug.cgi?id=884693
		//write dummy xml to response to prevent error "no element found" in firefox
		response.setHeader("Content-Type", "text/xml");
		PrintWriter pw=new PrintWriter(response.getOutputStream());
		pw.println("<response>"+content+"</response>");
		pw.close();
	}

//========================================================================
	public void sendFile(HttpServletResponse res, String file_uri) throws IOException
	{
		System.err.println("sending file "+file_uri);
		//deliver all-in-one html login form
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
	private void sendUploadForm(HttpServletResponse res) throws IOException
	{
		sendFile(res,upload_html_form);
	}

//========================================================================
	public void handle(String target,
			Request baseRequest,
			HttpServletRequest request,
			HttpServletResponse response ) throws IOException
	{
		if(request.getRequestURI().equals("/")
			|| request.getRequestURI().equals("/upload_form")
		)
		{
			sendUploadForm(response);
			baseRequest.setHandled(true);
			return;
		}

		if(request.getMethod()=="OPTIONS")
		{
			System.err.println("got OPTIONS request");
			response.setHeader("Access-Control-Allow-Origin", Access_Control_Allow_Origin);
			response.setHeader("Access-Control-Allow-Headers", Access_Control_Allow_Headers);
			response.setHeader("Access-Control-Allow-Methods", "POST,OPTIONS");
		}
		else if(request.getMethod()=="POST")
		{
			System.err.println("got POST request");

			response.setHeader("Access-Control-Allow-Origin", Access_Control_Allow_Origin);

			String filename=request.getHeader("X_FILENAME");
			if(filename==null)
			{
				System.err.println("/!\\ could not parse file upload request: x_filename header not found.");
				baseRequest.setHandled(true);
				return;
			}

			String random_id=randomString(20);
			String filename_store=random_id+".file";

			System.err.println("upload START: "+filename);
			System.err.println("storing '"+filename+"' to '"+download_dir+File.separator+filename_store +"'");

			InputStream is=request.getInputStream();
			OutputStream os=new FileOutputStream(new File(download_dir+File.separator+filename_store));

			///needs limit for total size

			byte[] buf = new byte[4096];
			for (int nChunk = is.read(buf); nChunk!=-1; nChunk = is.read(buf))
			{
				os.write(buf, 0, nChunk);
			}
			os.close();
			is.close();

			PrintWriter pw=new PrintWriter(new File(download_dir+File.separator+filename_store+".name"));
			pw.println(filename);
			pw.close();

			if(post_upload_hook_enabled)
			{
				try
				{
					//post upload hook
					String result=post.postUploadProcess(new File(download_dir+File.separator+filename_store), filename);
					System.out.println("hook return: "+result);
				}
				catch(Exception e)
				{
					///
					e.printStackTrace();
				}
			}

			sendXMLResponse(response,random_id);

			System.err.println("upload DONE:  "+filename);
		}
		else
		{///
			System.err.println("/!\\ request not understood.");
		}

		baseRequest.setHandled(true);
	}//end handle()
}//end class UploadHandler
//EOF
