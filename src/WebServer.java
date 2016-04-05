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
	public String handler_classname="TileInfoHandler"; ///////////
	public int port_range_start=8081;
	public int instance_count=4;
	public String context="/ti";
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
		try
		{
			for(int i=0;i<instance_count;i++)
			{
				System.out.println("\n\nstarting incance # "+i+" on port "+(port_range_start+i)+"\n");

				Server server = new Server(port_range_start+i);
				ContextHandler ch = new ContextHandler();
				ch.setContextPath(context);

				//ch.setHandler(new TileInfoHandler());
				Class<?> c = Class.forName(handler_classname);
				Constructor<?> cons = c.getConstructor();
				ch.setHandler((AbstractHandler)cons.newInstance());

				HandlerCollection hc = new HandlerCollection();
				hc.addHandler(ch);
				server.setHandler(hc);
				server.start();
				///server.join();
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
