package handlers;
import util.*;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

//import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Properties;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.File;

import java.io.IOException;

//tb/1610

//========================================================================
//========================================================================
public class RedirectHandler extends AbstractHandler
{
	//this file is loaded if found in current directory
	private static String propertiesFileUri="RedirectHandler.properties";

	//===configurable parameters (here: default values)
	public String redirect_html_file="./resources/redirect.html";
	//===end configurable parameters

//========================================================================
	public RedirectHandler()
	{
		if(!LProps.load(propertiesFileUri,this))
		{
			System.err.println("/!\\ could not load properties");
		}
	}

//========================================================================
	public void handle(String target,
			Request baseRequest,
			HttpServletRequest request,
			HttpServletResponse response ) throws IOException//, ServletException
	{
		//send file
		OutputStream os=response.getOutputStream();
		InputStream is=new FileInputStream(new File(redirect_html_file));
		byte[] buf = new byte[4096];
		for (int nChunk = is.read(buf); nChunk!=-1; nChunk = is.read(buf))
		{
			os.write(buf, 0, nChunk);
		}
		is.close();
		os.close();

		baseRequest.setHandled(true);
	}
}//end class RedirectHandler
//EOF
