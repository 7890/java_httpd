package handlers;
import util.*;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

//import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Enumeration;
import java.util.Scanner;
import java.util.Properties;

import java.io.IOException;

//tb/1606
//test, ~template for starters
//http://localhost:8081/foo/bar?a=b&c=d

//========================================================================
//========================================================================
public class TestHandler extends AbstractHandler
{
	//this file is loaded if found in current directory
	private static String propertiesFileUri="TestHandler.properties";

	//===configurable parameters (here: default values)
	//===end configurable parameters

//========================================================================
	public TestHandler()
	{
		if(!LProps.load(propertiesFileUri,this))
		{
			System.err.println("/!\\ could not load properties");
		}
	}

//http://stackoverflow.com/questions/8100634/get-the-post-request-body-from-httpservletrequest
//========================================================================
	private void printRequest(HttpServletRequest req) //throws IOException
	{
		System.out.println("Method: " + req.getMethod());
		System.out.println("Path Info: " + req.getPathInfo());

		//get headers
		Enumeration headerNames = req.getHeaderNames();
		while(headerNames.hasMoreElements())
		{
			String headerName = (String)headerNames.nextElement();
			System.out.println("Header: " + headerName + " = " + req.getHeader(headerName));
		}

		//get params
		Enumeration params = req.getParameterNames();
		while(params.hasMoreElements())
		{
			String paramName = (String)params.nextElement();
			System.out.println("Parameter: " + paramName + " = " + req.getParameter(paramName));
		}
		//System.out.println(extractPostRequestBody(req));
	}

//========================================================================
	static String extractPostRequestBody(HttpServletRequest request) throws IOException
	{
		if ("POST".equalsIgnoreCase(request.getMethod()))
		{
			Scanner s = null;
			s = new Scanner(request.getInputStream(), "UTF-8").useDelimiter("\\A");
			return s.hasNext() ? s.next() : "";
		}
		return "";
	}

//========================================================================
	public void handle(String target,
			Request baseRequest,
			HttpServletRequest request,
			HttpServletResponse response ) //throws IOException, ServletException
	{
		printRequest(request);
		baseRequest.setHandled(true);
	}
}//end class TestHandler
//EOF
