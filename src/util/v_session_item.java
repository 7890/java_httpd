package util;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

//tb/1607

//========================================================================
//========================================================================
public class v_session_item
{
	public int id;
	public int id_user;
	public String hash;
	public long created;
	public long last_access;
	public long expires_in;
	public long logout;

//========================================================================
	public v_session_item(ResultSet rs) throws Exception
	{
		ResultSetMetaData rsmd = rs.getMetaData();
		int columnCount = rsmd.getColumnCount();
		if(columnCount!=7)
		{
			throw new Exception("column count doesn't match");
		}

		id=rs.getInt(1);
		id_user=rs.getInt(2);
		hash=rs.getString(3);
		created=rs.getLong(4);
		last_access=rs.getLong(5);
		logout=rs.getLong(6);
		expires_in=rs.getLong(7);
	}
//========================================================================
	public String toString()
	{
		return("SESSION:      "+hash+" "+expires_in);
	}
}//end v_session_item
//EOF
