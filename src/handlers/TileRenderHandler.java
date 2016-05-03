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

import com.jetdrone.map.render.backend.Renderer;
import com.jetdrone.map.rules.RuleSet;
import com.jetdrone.map.source.MapSource;

import java.util.Properties;

//tb/160415

/*
TileRenderHandler:
-takes pathinfo of request (/z/x/y | /z/x/y.png*) without parameters
-renders tiles using .idx database file
*/

//========================================================================
//========================================================================
public class TileRenderHandler extends AbstractHandler
{
	private static final Renderer renderer;
	private static String propertiesFileUri="TileRenderHandler.properties";

//========================================================================
	public TileRenderHandler(){}

//========================================================================
	static
	{
		System.err.println("static initialization of TileRenderHandler");

		String ruleset_uri=null;
		String mapsource_uri=null;
		Properties props=LProps.checkLoadFile(propertiesFileUri);
		if(props!=null)
		{
			try{ruleset_uri=props.getProperty("ruleset_uri");}
			catch(Exception e){System.err.println("/!\\ no ruleset configured. can not start.");}

			try{mapsource_uri=props.getProperty("mapsource_uri");}
			catch(Exception e){System.err.println("/!\\ no mapsource configured. can not start.");}
		}
		RuleSet ruleset = null;
		MapSource map = null;
		Renderer instance = null;
		try {
			ruleset = new RuleSet(new FileInputStream(ruleset_uri));//.xml file
			System.err.println("loading map (this may take a while)");
			map = new MapSource(new FileInputStream(mapsource_uri));//.idx file
			instance = new Renderer(ruleset, map);

		} catch (Exception e) {
			e.printStackTrace();
		}
		renderer = instance;
	}//end static

//========================================================================
	public void handle(String target,
			Request baseRequest,
			HttpServletRequest request,
			HttpServletResponse response ) throws IOException,ServletException
	{
		if(renderer==null)
		{
			System.err.println("renderer not ready");
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			baseRequest.setHandled(true);
			return;
		}
/*
		try
		{
			renderer.setRuleSet(new RuleSet(new FileInputStream("new_rules.xml")));
		}catch(Exception e){}
*/
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

		int zoom=0;
		int x=0;
		int y=0;
		//test if numbers
		try
		{
			zoom=Integer.parseInt(tokens[1]);
			x=Integer.parseInt(tokens[2]);
			if(tokens[3].contains(".png"))
			{
				tokens[3]=tokens[3].substring(0,tokens[3].indexOf(".png"));
			}
			y=Integer.parseInt(tokens[3]);
		}
		catch(Exception e)
		{
			System.err.println(e);
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			baseRequest.setHandled(true);
			return;
		}

		response.setContentType("image/png");
		response.setHeader("Access-Control-Allow-Origin", "*");

		//======
		renderer.drawTile(response.getOutputStream(), x, y, zoom);

		baseRequest.setHandled(true);
	}//end handle()
}//end class TileRenderHandler
//EOF
