package handlers;
import util.*;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Properties;

import java.io.IOException;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

//tb/1607

//========================================================================
//========================================================================
public class DownloadHandler extends AbstractHandler
{
	//this file is loaded if found in current directory
	private static String propertiesFileUri="DownloadHandler.properties";

	//===configurable parameters (here: default values)
	public boolean default_send_inline=true;
	public boolean allow_client_dispo_select=true;

	public String not_found_file_uri="./resources/not_found.html";
	//===end configurable parameters

//========================================================================
	public DownloadHandler()
	{
		if(!LProps.load(propertiesFileUri,this))
		{
			System.err.println("/!\\ could not load properties");
		}
	}

//========================================================================
	private void sendFile(HttpServletResponse response, File f, String displayName, String contentType, boolean content_dispo, boolean inline) throws IOException
	{
		System.err.println("sending file "+f.getPath());

		String dispo=(inline ? "inline" : "attachment");

		response.setHeader("Content-Type", contentType);
		if(content_dispo)
		{
			response.setHeader("Content-Disposition", dispo+";filename=\"" + displayName + "\"");
		}
		response.setHeader("Content-Length", ""+f.length());
		//response.setDateHeader("Last-Modified", lastModified);
		//response.setDateHeader("Expires", expires);

		OutputStream os=response.getOutputStream();
		InputStream is=new FileInputStream(f);

		byte[] buf = new byte[4096];
		for (int nChunk = is.read(buf); nChunk!=-1; nChunk = is.read(buf))
		{
			os.write(buf, 0, nChunk);
		}
		os.close();
		is.close();
	}//end sendFile()

//========================================================================
	public void handle(String target,
			Request baseRequest,
			HttpServletRequest request,
			HttpServletResponse response ) throws IOException
	{
		FileDownload fd=null;
		try
		{
			//get requested file from uri
			String ruri=request.getPathInfo();
			String file_link=ruri.substring(1,ruri.length());
			if(file_link==null)
			{
				sendFile(response, new File(not_found_file_uri), "", "text/html", false, false);
				baseRequest.setHandled(true);
				return;
			}

			//check if inline requested
			boolean inline=default_send_inline;

			if(allow_client_dispo_select)
			{
				String dispo_type_inline=request.getParameter("inline");
				if(dispo_type_inline!=null)
				{
					inline=true;
				}
				String dispo_type_attach=request.getParameter("attach");
				if(dispo_type_attach!=null)
				{
					inline=false;
				}
			}
			//create access object to file store backend
			fd=new FileDownload();

			FileLinkItem item=fd.ps_get_file_link_(file_link);
			if(item==null)
			{
				sendFile(response, new File(not_found_file_uri), "", "text/html", false, false);
				try{fd.close();}catch(Exception e){e.printStackTrace();}
				baseRequest.setHandled(true);
				return;
			}

			//send file to requester
			File f=new File(item.uri);
			if(f.exists() && !f.isDirectory())
			{
				sendFile(response,f,item.displayname,item.mimetype,true,inline);
			}
			else
			{
				sendFile(response, new File(not_found_file_uri), "", "text/html", false, false);
				try{fd.close();}catch(Exception e){e.printStackTrace();}
				baseRequest.setHandled(true);
				return;
			}
		}catch(Exception e){e.printStackTrace();}
		finally
		{
			try{fd.close();}catch(Exception e){e.printStackTrace();}
		}

		baseRequest.setHandled(true);
	}//end handle()
}//end class DownloadHandler
//EOF
