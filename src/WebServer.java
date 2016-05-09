import util.*;

import java.util.Vector;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.AbstractHandler;

import java.lang.reflect.Constructor;

//http://download.eclipse.org/jetty/9.3.7.v20160115/apidocs/org/eclipse/jetty/websocket/api/package-summary.html

//tb/160403

//========================================================================
//========================================================================
public class WebServer
{
	//this file is loaded if found in current directory
	private String propertiesFileUri="WebServer.properties";

	//===configurable parameters (here: default values)
	public Vector handlers=new Vector(); ///handlers.add("handlers.TileInfoHandler");
	public Vector instance_count=new Vector();
	public Vector context=new Vector();
	public int port_range_start=8081;
	//===end configurable parameters

//========================================================================
	public static void main(String[] args) throws Exception
	{
		WebServer ws=new WebServer(args);
	}

//========================================================================
	public WebServer(String[] args)
	{
		if(!loadProps(propertiesFileUri))
		{
			System.err.println("could not load properties");
		}

		if( ! (handlers.size()==instance_count.size() && handlers.size()==context.size() ) )
		{
			System.err.println("properties invalid, token count doesn't match");
			System.exit(1);
		}

		for(int k=0;k<handlers.size();k++)
		{
			System.err.println("handler: '"+handlers.get(k)+"' "+instance_count.get(k)+" "+context.get(k));
		}

		try
		{
			int port=port_range_start;

			//for every handler
			for(int h=0;h<handlers.size();h++)
			{
				//n instances
				for(int i=0;i<  Integer.parseInt((String)instance_count.get(h))  ;i++)
				{
					System.out.println("\n\nstarting instance # "+i+" of "+handlers.get(h)+" on port "+port+", attaching to context "+context.get(h)+"\n");

					Server server = new Server(port);
					ContextHandler ch = new ContextHandler();
					ch.setContextPath((String)context.get(h));

					//ch.setHandler(new TileInfoHandler());
					Class<?> c = Class.forName((String)handlers.get(h));
					Constructor<?> cons = c.getConstructor();
					ch.setHandler((AbstractHandler)cons.newInstance());

					HandlerCollection hc = new HandlerCollection();
					hc.addHandler(ch);
					server.setHandler(hc);
					server.start();
					///server.join();
					port++;
				}
			}
		}
		catch(Exception e)
		{
			System.err.println("/!\\ "+e);
			System.err.println("/!\\ could not start up. terminating");
			System.exit(1);
		}
	}//end WebServer constructor

//========================================================================
	public boolean loadProps(String configfile_uri)
	{
		propertiesFileUri=configfile_uri;
		return LProps.load(propertiesFileUri,this);
	}
}//end class WebServer
//EOF
