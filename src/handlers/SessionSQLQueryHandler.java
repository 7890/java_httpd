package handlers;
import util.*;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

//tb/1608

//========================================================================
//========================================================================
public class SessionSQLQueryHandler extends SQLQueryHandler
{
//========================================================================
	public SessionSQLQueryHandler()
	{
		super();
	}

//========================================================================
	public void handle(String target,
			Request baseRequest,
			HttpServletRequest request,
			HttpServletResponse response ) throws IOException
	{
		int authorized=0;
		CustomSessionManager sm=null;
		try{sm=new CustomSessionManager();}
		catch(Exception e)
		{
			e.printStackTrace();
			baseRequest.setHandled(true);
			return;	
		}

		try{authorized=sm.auth(request,response);}
		catch(Exception e)
		{
			e.printStackTrace();
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED");
			try{sm.close();}catch(Exception e1){e1.printStackTrace();}
			baseRequest.setHandled(true);
			return;	
		}

		if(authorized!=1)
		{
			//possibly XHR request. don't send "human" login form
			if(authorized<=0 && request.getMethod()=="POST" && request.getPathInfo().equals("/query"))
			{
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED");
			}
			else if(authorized==0)
			{
				try{sm.sendLoginForm(response);}catch(Exception e){e.printStackTrace();}
			}
			else if(authorized==-1)
			{
				try{sm.sendLogoutRedirect(response);}catch(Exception e){e.printStackTrace();}
			}

			try{sm.close();}catch(Exception e1){e1.printStackTrace();}
			baseRequest.setHandled(true);
			return;
		}

		super.handle(target,baseRequest,request,response);
	}//end handle()
}//end class SessionSQLQueryHandler
//EOF
