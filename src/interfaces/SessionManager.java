package interfaces;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//=============================================================================
public interface SessionManager
{
	public int auth(HttpServletRequest req,HttpServletResponse res) throws Exception;
}
//EOF
