import util.*;

import java.util.Vector;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.net.InetSocketAddress;

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

//for built-in error handler
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.Request;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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
	public int start_on_same_server	=0;
	public int port_range_start	=8081;
	public String bind_to_interface="127.0.0.1"; //only localhost. all: 0.0.0.0
	public int use_ssl		=0;
	public String keystore_uri	="resources/keystore";
	public String keystore_pass	="123456";
	//===end configurable parameters

	private ErrorHandler errorHandler;

//========================================================================
	public static void main(String[] args) throws Exception
	{
		WebServer ws=new WebServer(args);
	}

//========================================================================
	private Server getSslServer(int port)
	{
		Server server = new Server();

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
		sslConnector.setHost(bind_to_interface);
		server.addConnector(sslConnector);
		return server;
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
			System.err.println("/!\\ could not load properties");
		}

		//check if properties make sense
		if( ! (handlers.size()==instance_count.size() && handlers.size()==context.size()) )
		{
			System.err.println("/!\\ properties invalid: token count doesn't match");
			System.exit(1);
		}

		//make sure contexts start with '/'
		for(int h=0;h<handlers.size();h++)
		{
			if( ! ((String)context.get(h)).startsWith("/") )
			{
				System.err.println("/!\\ properties invalid: contexts must start with '/'");
				System.exit(1);
			}
		}

		//when all handlers should be started on the same server (port):
		if(start_on_same_server==1)
		{
			//instance_count can't be > 1
			System.out.println("/!\\ ignoring instance_count, fixed to 1 (start_on_same_server=1)");
			for(int k=0;k<handlers.size();k++)
			{
				instance_count.set(k,"1");
			}

			//can't use the same context, contexts should not overlap
			for(int h=0;h<handlers.size();h++)
			{
				String context1=(String)context.get(h);
				for(int k=(h+1);k<handlers.size();k++)
				{
					String context2=(String)context.get(k);
					if(context1.equals(context2))
					{
						System.err.println("/!\\ properties invalid: contexts must be unique (start_on_same_server=1)");
						System.exit(1);
					}
					String tokens1[]=context1.split("/");
					String tokens2[]=context2.split("/");
					if(tokens1.length<2 || tokens2.length<2 || tokens1[1].equals(tokens2[1]))
					{
						System.err.println("/!\\ properties invalid: contexts can't overlap (start_on_same_server=1)");
						System.err.println(context1+" "+context2);
						System.exit(1);
					}
				}
			}
		}//end if(start_on_same_server==1)

		for(int k=0;k<handlers.size();k++)
		{
			System.err.println("handler: '"+handlers.get(k)+"' "+instance_count.get(k)+" "+context.get(k));
		}

		try
		{
			Server server=null;
			HandlerCollection handler_collection=null;

			//common for all
			errorHandler = new CustomErrorHandler();

			int port=port_range_start;

			if(start_on_same_server==1)
			{
				if(use_ssl==0)
				{
					System.out.println("creating server on HTTP port "+port);
					server = new Server(InetSocketAddress.createUnresolved(bind_to_interface,port));
				}
				else
				{
					System.out.println("creating server on HTTPS port "+port);
					server = getSslServer(port);
				}
				handler_collection = new HandlerCollection();
			}

			//for every handler
			for(int h=0;h<handlers.size();h++)
			{
				//n instances
				for(int i=0;i<Integer.parseInt((String)instance_count.get(h));i++)
				{
					System.out.println("creating handler instance # "+(i+1)+" of "+handlers.get(h)+", context "+context.get(h));

					ContextHandler context_handler = new ContextHandler();
					context_handler.setContextPath((String)context.get(h));

					//context_handler.setHandler(new TileInfoHandler());
					Class<?> c = Class.forName((String)handlers.get(h));
					Constructor<?> cons = c.getConstructor();
					context_handler.setHandler((AbstractHandler)cons.newInstance());

					///
					/*
					String[] vhosts=new String[1];
					vhosts[0]="foo.bar.foo";
					context_handler.setVirtualHosts(vhosts);
					*/

					if(start_on_same_server==1)
					{
						handler_collection.addHandler(context_handler);
					}
					else
					{
						if(use_ssl==0) //http://
						{
							System.out.println("creating server on HTTP port "+port+" for handler instance # "+(i+1)+" of "+handlers.get(h)+", context "+context.get(h));
							server = new Server(InetSocketAddress.createUnresolved(bind_to_interface,port));
						}
						else //https://
						{
							System.out.println("creating server on HTTPS port "+port+" for handler instance # "+(i+1)+" of "+handlers.get(h)+", context "+context.get(h));
							server = getSslServer(port);
						}
						server.setHandler(context_handler);
						///errorHandler.setServer(server);
						server.addBean(errorHandler);
						server.start();
						///server.join();
						port++;
					}
				}//end for n instances
			}//end for every handler

			if(start_on_same_server==1)
			{
				server.setHandler(handler_collection);
				errorHandler.setServer(server);
				server.addBean(errorHandler);
				server.start();
				server.join();
			}
		}
		catch(Exception e)
		{
			System.err.println("/!\\ "+e);
			e.printStackTrace();
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

//========================================================================
	class CustomErrorHandler extends ErrorHandler
	{
		//public ByteBuffer badMessageError(int status,String reason,HttpFields fields)
		public void handle(String target,Request baseRequest,HttpServletRequest request,HttpServletResponse response) throws IOException
		{
			PrintWriter pw=new PrintWriter(response.getOutputStream());
			pw.println("oops.");
			pw.close();
		}
	}//end inner class CustomErrorHandler
}//end class WebServer
//EOF
