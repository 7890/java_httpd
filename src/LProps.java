//http://docs.oracle.com/javase/tutorial/reflect/member/fieldTypes.html
//tb/160306
import java.util.Properties;
import java.util.Enumeration;
import java.util.TreeSet;
import java.util.Collections;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
//========================================================================
//========================================================================
class LProps
{
	public LProps(){}

//========================================================================
	public static boolean load(String configfile_uri, Object configurable_object)
	{
		try
		{
			Properties props=checkLoadFile(configfile_uri);
			if(props==null)
			{
				return false;
			}
			Class<?> c = configurable_object.getClass();
			Field[] fields = c.getFields();
			for(int i=0; i<fields.length;i++)
			{
//				System.err.println("field "+i+" : "+fields[i]);
				Class ctype=fields[i].getType();
				String fname=fields[i].getName();
				if(props.getProperty(fname)!=null)
				{
//					System.err.println("found matching member variable property in file");
					if(ctype==int.class || ctype==Integer.class)
					{
//						System.err.println("found int");
						try{fields[i].setInt(configurable_object, Integer.parseInt(props.getProperty(fname)));}
						catch(Exception e){System.err.println(""+e);}
					}
					else if(ctype==float.class || ctype==Float.class)
					{
//						System.err.println("found float");
						try{fields[i].setFloat(configurable_object, Float.parseFloat(props.getProperty(fname)));}
						catch(Exception e){System.err.println(""+e);}
					}
					else if(ctype==String.class)
					{
//						System.err.println("found string");
						try{fields[i].set(configurable_object, props.getProperty(fname));}
						catch(Exception e){System.err.println(""+e);}
					}
					else if(ctype==boolean.class || ctype==boolean.class)
					{
//						System.err.println("found boolean");
						try{fields[i].setBoolean(configurable_object, Boolean.parseBoolean(props.getProperty(fname)));}
						catch(Exception e){System.err.println(""+e);}
					}
					///else if byte,short,long,char,double
				}//end if found property
			}//end for all fields
			return true;
		}//end try
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return false;
	}//end load()

//========================================================================
	public static Properties checkLoadFile(String configfile_uri)
	{
		Properties props=new Properties();
		InputStream is=null;
		try
		{
			File f=new File(configfile_uri);
			if(!f.exists() || !f.canRead())
			{
				return null;
			}
			is=new FileInputStream(f);
			if(is==null)
			{
				return null;
			}
			props.load(is);
			if(props==null)
			{
				return null;
			}
			is.close(); ////
			return props;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

//========================================================================
	public static Properties getOrderedProperties(Properties props)
	{
		//http://stackoverflow.com/questions/17011108/how-can-i-write-java-properties-in-a-defined-order
		Properties tmp=new Properties()
		{
			public synchronized Enumeration<Object> keys()
			{
				return Collections.enumeration(new TreeSet<Object>(super.keySet()));
			}
		};
		tmp.putAll(props);
		return tmp;
	}

//========================================================================
	public static Object get(String configfile_uri, String key)
	{
		Properties props=checkLoadFile(configfile_uri);
		if(props==null){return null;}
		try
		{
			return props.getProperty(key);	
		}
		catch(Exception e){e.printStackTrace();}
		return null;
	}

//========================================================================
	public static boolean set(String configfile_uri, String key, Object val)
	{
		Properties props=checkLoadFile(configfile_uri);
		if(props==null){return false;}
		try
		{
			props.setProperty(key, ""+val);
//			getOrderedProperties(props).store(System.out, null);
			///OVERWRITE ORIGINAL FILE. LOOSING ALL COMMENTS. KEYS IN ALPHABETIC ORDER.
			getOrderedProperties(props).store(new FileOutputStream(new File(configfile_uri)), null);
//			System.out.println(key+"="+get(configfile_uri,key));
//			print(configfile_uri);
			return true;
		}
		catch(Exception e){e.printStackTrace();}
		return false;
	}

//========================================================================
	public static boolean print(String configfile_uri)
	{
		Properties props=checkLoadFile(configfile_uri);
		if(props==null){return false;}
		try
		{
			getOrderedProperties(props).store(System.out, null);
			return true;
		}
		catch(Exception e){e.printStackTrace();}
		return false;
	}

}//end class LProps
