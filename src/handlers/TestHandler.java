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
		//from snoop servlet
		System.out.println("Protocol: " + req.getProtocol());
		System.out.println("Scheme: " + req.getScheme());
		System.out.println("Server Name: " + HTMLfilter(req.getServerName()));
		System.out.println("Server Port: " + req.getServerPort());
		System.out.println("Remote Addr: " + req.getRemoteAddr());
		System.out.println("Remote Host: " + req.getRemoteHost());
		System.out.println("Character Encoding: " + HTMLfilter(req.getCharacterEncoding()));
		System.out.println("Content Length: " + req.getContentLength());
		System.out.println("Content Type: "+ HTMLfilter(req.getContentType()));
		System.out.println("Locale: "+ HTMLfilter(req.getLocale().toString()));

		System.out.println("HTTP Method: " + req.getMethod());
		System.out.println("Request URI: " + req.getRequestURI());
		System.out.println("Context Path: " + req.getContextPath());
		System.out.println("Path Info: " + HTMLfilter(req.getPathInfo()));
		System.out.println("Query String: " + HTMLfilter(req.getQueryString()));
		System.out.println("Request Is Secure: " + req.isSecure());

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
	}//end printRequest()

//========================================================================
	public void handle(String target,
			Request baseRequest,
			HttpServletRequest request,
			HttpServletResponse response ) //throws IOException, ServletException
	{
		printRequest(request);
		baseRequest.setHandled(true);
	}

/**
* Filter the specified message string for characters that are sensitive
* in HTML.  This avoids potential attacks caused by including JavaScript
* codes in the request URL that is often reported in error messages.
*
* @param message The message string to be filtered
*/
//========================================================================
	public static String HTMLfilter(String message)
	{
		if (message == null)
		{
			return (null);
		}

		char content[] = new char[message.length()];
		message.getChars(0, message.length(), content, 0);
		StringBuffer result = new StringBuffer(content.length + 50);
		for (int i = 0; i < content.length; i++)
		{
			switch (content[i])
			{
				case '<':
					result.append("<");
					break;
				case '>':
					result.append(">");
					break;
				case '&':
					result.append("&");
					break;
				case '"':
					result.append("\"");
					break;
				default:
					result.append(content[i]);
			}
		}
		return (result.toString());
	}
}//end class TestHandler
//EOF
