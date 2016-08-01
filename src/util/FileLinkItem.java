package util;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

//tb/1607

//========================================================================
//========================================================================
public class FileLinkItem
{
	public int id;
	public String link;
	public int id_file;
	public String displayname;
	public String uri;
	public String mimetype;
	public int length;

//========================================================================
	public FileLinkItem(ResultSet rs) throws Exception
	{
		ResultSetMetaData rsmd = rs.getMetaData();
		int columnCount = rsmd.getColumnCount();
		if(columnCount!=7)
		{
			throw new Exception("column count doesn't match");
		}

		id=rs.getInt(1);
		link=rs.getString(2);
		id_file=rs.getInt(3);
		displayname=rs.getString(4);
		uri=rs.getString(5);
		mimetype=rs.getString(6);
		length=rs.getInt(7);
	}
//========================================================================
	public String toString()
	{
		return "FILE_LINK: "+link;
	}
}//end FileLinkItem
//EOF
