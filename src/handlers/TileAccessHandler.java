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
import java.util.Vector;

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

	public Vector layers=new Vector(); //subdirectories of tiles_root_uri

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

		for(int i=0;i<layers.size();i++)
		{
			System.out.println("TileAccessHandler layer: "+layers.get(i));
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
//		String tokens[]=pathInfo.split("/"); // "/z/x/y"
		//first token is layer
		String tokens[]=pathInfo.split("/"); // "[layername]/z/x/y"

//		if(tokens.length!=4)
		if(tokens.length!=5)
		{
			System.err.println("invalid request");
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			baseRequest.setHandled(true);
			return;
		}

		//test if numbers
		try
		{
			Integer.parseInt(tokens[2]);
			Integer.parseInt(tokens[3]);
			if(tokens[4].contains(".png"))
			{
				tokens[4]=tokens[4].substring(0,tokens[4].indexOf(".png"));
			}
			Integer.parseInt(tokens[4]);
		}
		catch(Exception e)
		{
			System.err.println(e);
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			baseRequest.setHandled(true);
			return;
		}

		int layer_index=getLayerIndex(tokens[1]);
		//test if layer exists in config
		if(layer_index<0)
		{
			System.err.println("layer not found: "+tokens[1]);
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			baseRequest.setHandled(true);
			return;
		}

		String image_uri
			=tiles_root_uri
				+File.separator
			+layers.get(layer_index)
				+File.separator
			+tokens[2]
				+File.separator
			+tokens[3]
				+File.separator
			+tokens[3]+"_"+tokens[4]+".png";

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

//========================================================================
	private int getLayerIndex(String layer)
	{
		//using a hashmap would be better
		for(int i=0;i<layers.size();i++)
		{
			if( ( (String)layers.get(i) ).equals(layer) )
			{
				return i;
			}
		}
		return -1;
	}

}//end class TileAccessHandler
//EOF
