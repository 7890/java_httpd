package handlers;
import util.*;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
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

//tb/1606
//all-in-one upload html form is included in this handler
//http://localhost:8081/?upload_form

//========================================================================
//========================================================================
public class UploadHandler extends AbstractHandler
{
	//this file is loaded if found in current directory
	private static String propertiesFileUri="UploadHandler.properties";

	//===configurable parameters (here: default values)
	public String upload_html_form			="resources/upload_form.html";
	public String download_dir			="/tmp";
	public String Access_Control_Allow_Origin	="*";
	//public String Access-Control-Allow-Origin	="null"; //html loaded from anywhere, server running on localhost
	public String Access_Control_Allow_Headers	="content-type,x_filename";
	//===end configurable parameters

	private static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	private static SecureRandom rnd = new SecureRandom();

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
	public UploadHandler()
	{
		if(!LProps.load(propertiesFileUri,this))
		{
			System.err.println("/!\\ could not load properties");
		}
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
	public void handle(String target,
			Request baseRequest,
			HttpServletRequest request,
			HttpServletResponse response ) throws IOException
	{

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

			boolean found_filename_header=false;
			String filename="";

			//check headers
			Enumeration headerNames = request.getHeaderNames();
			while(headerNames.hasMoreElements())
			{
				String headerName = (String)headerNames.nextElement();
//				System.out.println("Header: " + headerName + " = " + request.getHeader(headerName));
				if(headerName.toLowerCase().equals("x_filename"))
				{
					found_filename_header=true;
					filename=request.getHeader(headerName);
					break;
				}
			}

			if(!found_filename_header)
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

			sendXMLResponse(response,random_id);

			System.err.println("upload DONE:  "+filename);
		}
		else if(request.getMethod()=="GET")
		{
			System.err.println("got GET request");
			boolean found_param=false;
			//get params
			Enumeration params = request.getParameterNames();
			while(params.hasMoreElements())
			{
				String paramName = (String)params.nextElement();
//				System.out.println("Parameter: " + paramName + " = " + request.getParameter(paramName));
				if(paramName.equals("upload_form"))
				{
					found_param=true;
					break;
				}
			}
			if(found_param)
			{
				//deliver all-in-one html upload form
				response.setHeader("Content-Type", "text/html");
				OutputStream os=response.getOutputStream();
				InputStream is=new FileInputStream(upload_html_form);

				byte[] buf = new byte[4096];
				for (int nChunk = is.read(buf); nChunk!=-1; nChunk = is.read(buf))
				{
					os.write(buf, 0, nChunk);
				}
				os.close();
				is.close();
			}
			else
			{
				System.err.println("param 'upload_form' not found.");
			}
		}
		else
		{///
			System.err.println("/!\\ request not understood.");
		}

		baseRequest.setHandled(true);
		return;

	}//end handle()
}//end class UploadHandler
//EOF
