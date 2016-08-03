package handlers;
import util.*;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.FileReader;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STWriter;
import org.stringtemplate.v4.AutoIndentWriter;

//tb/1608

//========================================================================
//========================================================================
public class TemplateHandler extends AbstractHandler
{
	//this file is loaded if found in current directory
	private static String propertiesFileUri="TemplateHandler.properties";

	//===configurable parameters (here: default values)
	//===end configurable parameters

//========================================================================
	public TemplateHandler()
	{
		if(!LProps.load(propertiesFileUri,this))
		{
			System.err.println("/!\\ could not load properties");
		}
	}

//========================================================================
//========================================================================
	public void handle(String target,
			Request baseRequest,
			HttpServletRequest request,
			HttpServletResponse response ) throws IOException
	{
		ST test=tmpl("./resources/templates/test.html.tmpl");
		test.add("field","field_value");
		//System.out.println(test.render());
		//System.out.flush();

		response.setHeader("Content-Type", "text/html");
		OutputStream os=response.getOutputStream();
		OutputStreamWriter osWriter = new OutputStreamWriter(os);
		STWriter stWriter = new AutoIndentWriter(osWriter);
		test.write(stWriter);
		osWriter.flush();
		osWriter.close();
		os.close();

		baseRequest.setHandled(true);
	}

//=============================================================================
	private static ST tmpl(String template_file_uri) throws IOException
	{
		return new ST(getAsString(template_file_uri),'$','$');
	}

//=============================================================================
	private static String getAsString(String sFile) throws IOException
	{
		BufferedReader reader = new BufferedReader(new FileReader(sFile));
		StringBuffer content = new StringBuffer();
		String sLine="";

		while (sLine!=null)
		{
			sLine=reader.readLine();
			if (sLine!=null)
			{
				content.append(sLine+"\n");
			}
		}
		reader.close();
		return content.toString();
	}//end getAsString
}//end class TemplateHandler
//EOF
