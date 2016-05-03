package handlers;
import util.*;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

import java.util.Properties;

//tb/160404

/*
TileAccessHandler:
-takes pathinfo of request (/z/x/y | /z/x/y.png*) without parameters
*translates to image file path on filesystem
*/

//========================================================================
//========================================================================
public class TileAccessHandler extends AbstractHandler
{
	//this file is loaded if found in current directory
	private static String propertiesFileUri="TileAccessHandler.properties";

	//===configurable parameters (here: default values)
	public String tiles_root_uri="./tiles";
	public String dummy_image_uri=tiles_root_uri+"/dummy.png";
	public boolean send_dummy_on_missing=true;
	//===end configurable parameters

//========================================================================
	public TileAccessHandler()
	{
		if(!LProps.load(propertiesFileUri,this))
		{
			System.out.println("/!\\ could not load properties");
		}
	}

//========================================================================
	public void handle(String target,
			Request baseRequest,
			HttpServletRequest request,
			HttpServletResponse response ) throws IOException,ServletException
	{
		String pathInfo = request.getPathInfo();
		System.err.println("req "+pathInfo);
		String tokens[]=pathInfo.split("/"); // "/z/x/y"
		if(tokens.length!=4)
		{
			System.err.println("invalid request");
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			baseRequest.setHandled(true);
			return;
		}

		//test if numbers
		try
		{
			Integer.parseInt(tokens[1]);
			Integer.parseInt(tokens[2]);
			if(tokens[3].contains(".png"))
			{
				tokens[3]=tokens[3].substring(0,tokens[3].indexOf(".png"));
			}
			Integer.parseInt(tokens[3]);
		}
		catch(Exception e)
		{
			System.err.println(e);
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			baseRequest.setHandled(true);
			return;
		}

		String image_uri
			=tiles_root_uri
			+File.separator
			+tokens[1]
			+File.separator
			+tokens[2]
			+File.separator
			+tokens[2]+"_"+tokens[3]+".png";

		System.err.println("<- "+image_uri);

		File file=new File(image_uri);
		if(!file.canRead())
		{
			try
			{
				if(send_dummy_on_missing)
				{
					System.err.println("sending dummy image");
					file=new File(dummy_image_uri);
					if(!file.canRead()) {throw new Exception("dummy image not found.");}
				}
				else
				{
					{throw new Exception("image not found.");}
				}
			}
			catch(Exception e)
			{
				System.err.println(e);
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				baseRequest.setHandled(true);
				return;
			}
		}

		response.setContentLength((int)file.length());
		FileInputStream in = new FileInputStream(file);

		response.setContentType("image/png");
		//if this is NOT set, tiles won't be displayed in OSM slippy map!
		response.setHeader("Access-Control-Allow-Origin", "*");

		OutputStream out=response.getOutputStream();

		//copy the contents of the file to the output stream
		byte[] buf = new byte[4096];
		int count = 0;
		while ((count = in.read(buf)) >= 0)
		{
			out.write(buf, 0, count);
		}
		out.close();
		in.close();

		baseRequest.setHandled(true);
	}//end handle()
}//end class TileAccessHandler
//EOF
