import util.*;

import java.util.Vector;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.AbstractHandler;

//for https / ssl =========
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.http.HttpVersion;
//=========================

import java.lang.reflect.Constructor;

//http://download.eclipse.org/jetty/9.3.7.v20160115/apidocs/org/eclipse/jetty/websocket/api/package-summary.html
//https://www.eclipse.org/jetty/documentation/9.3.x/embedding-jetty.html

//tb/160403

//========================================================================
//========================================================================
public class WebServer
{
	//this file is loaded if found in current directory
	private String propertiesFileUri="WebServer.properties";

	//===configurable parameters (here: default values)
	public Vector handlers		=new Vector(); ///handlers.add("handlers.TileInfoHandler");
	public Vector instance_count	=new Vector();
	public Vector context		=new Vector();
	public int port_range_start	=8081;
	public int use_ssl		=0;
	public String keystore_uri	="resources/keystore";
	public String keystore_pass	="123456";
	//===end configurable parameters

//========================================================================
	public static void main(String[] args) throws Exception
	{
		WebServer ws=new WebServer(args);
	}

//========================================================================
	public WebServer(String[] args)
	{
		if(args.length>0 && (args[0].equals("-h") || args[0].equals("--help")))
		{
			System.out.println("WebServer Help");
			System.out.println("Arguments: (properties file to use)");
			System.out.println("If no argument given, default file '"+propertiesFileUri+"' will be used.");
			System.exit(0);
		}
		else if(args.length>0)
		{
			propertiesFileUri=args[0];
		}

		if(!loadProps(propertiesFileUri))
		{
			System.err.println("could not load properties");
		}
/*
		// HTTP Configuration
		HttpConfiguration http_config = new HttpConfiguration();
		http_config.setOutputBufferSize(32768);
		http_config.setRequestHeaderSize(8192);
		http_config.setResponseHeaderSize(8192);
		http_config.setSendServerVersion(true);
		http_config.setSendDateHeader(false);
*/

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
				for(int i=0;i<Integer.parseInt((String)instance_count.get(h));i++)
				{
					Server server=null;
					if(use_ssl==0) //http://
					{
						System.out.println("\n\nstarting instance # "+i+" of "+handlers.get(h)+" on port HTTP "+port+", attaching to context "+context.get(h)+"\n");
						server = new Server(port);
					}
					else //https://
					{
						System.out.println("\n\nstarting instance # "+i+" of "+handlers.get(h)+" on port HTTPS "+port+", attaching to context "+context.get(h)+"\n");
						server = new Server();

						// SSL Context Factory
						SslContextFactory sslContextFactory = new SslContextFactory();

						sslContextFactory.setKeyStorePath(keystore_uri);
						sslContextFactory.setKeyStorePassword(keystore_pass);

						// SSL HTTP Configuration
						HttpConfiguration https_config = new HttpConfiguration();
						https_config.addCustomizer(new SecureRequestCustomizer());

						ServerConnector sslConnector = new ServerConnector(
							server
							,new SslConnectionFactory(sslContextFactory,HttpVersion.HTTP_1_1.asString())
							,new HttpConnectionFactory(https_config)
						);
						sslConnector.setPort(port);

						server.addConnector(sslConnector);
					}

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
			}//end for every handler
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
