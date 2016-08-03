package util;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

//tb/1607

//========================================================================
//========================================================================
public class v_user_condition_item
{
	public int id;
	public String username;
	public String password_hash;
	public int id_condition;
	public String condition;

//========================================================================
	public v_user_condition_item(ResultSet rs) throws Exception
	{
		ResultSetMetaData rsmd = rs.getMetaData();
		int columnCount = rsmd.getColumnCount();
		if(columnCount!=5)
		{
			throw new Exception("column count doesn't match");
		}

		id=rs.getInt(1);
		username=rs.getString(2);
		password_hash=rs.getString(3);
		id_condition=rs.getInt(4);
		condition=rs.getString(5);
	}
//========================================================================
	public String toString()
	{
		return "USER: "+username;
	}
}//end v_user_condition_item
//EOF
