package util;

import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.DecimalFormat;

/*
Date(long date)
Allocates a Date object and initializes it to represent the specified number of milliseconds 
since the standard base time known as "the epoch", namely January 1, 1970, 00:00:00 GMT.
*/

//tb/1603

//=============================================================================
public class DTime
{
	private static String timezone_id="UTC";

	private static String date_format_string="yyyy-MM-dd";
	private static String time_format_string="HH:mm:ss.SSS";
	private static String time_format_string_short="HH:mm:ss";
	private static String date_time_format_string=date_format_string+"_"+time_format_string;
	private static String date_time_format_string_short=date_format_string+"_"+time_format_string_short;

	private static SimpleDateFormat date_time_format=new SimpleDateFormat(date_time_format_string);
	private static SimpleDateFormat date_time_format_short=new SimpleDateFormat(date_time_format_string_short);
	private static SimpleDateFormat date_format=new SimpleDateFormat(date_format_string);
	private static SimpleDateFormat time_format=new SimpleDateFormat(time_format_string);

	private static String time2_format_string="HHmmss";
	private static String time3_format_string="HHmmss.SSS";
	private static String time4_format_string="HH:mm:ss";

	private static String date2_format_string="yyyyMMdd";
	private static String date3_format_string="ddMMyy";

	private static SimpleDateFormat time2_format=new SimpleDateFormat(time2_format_string);
	private static SimpleDateFormat time3_format=new SimpleDateFormat(time3_format_string);
	private static SimpleDateFormat time4_format=new SimpleDateFormat(time4_format_string);

	private static SimpleDateFormat date2_format=new SimpleDateFormat(date2_format_string);
	private static SimpleDateFormat date3_format=new SimpleDateFormat(date3_format_string);

	private static DecimalFormat df_time_zero = new java.text.DecimalFormat("000000.000");

	public DTime(){}

	public static void setTimeZoneUTC()
	{
		date_time_format.setTimeZone(TimeZone.getTimeZone(timezone_id));
		date_time_format_short.setTimeZone(TimeZone.getTimeZone(timezone_id));
		date_format.setTimeZone(TimeZone.getTimeZone(timezone_id));
		time_format.setTimeZone(TimeZone.getTimeZone(timezone_id));
		time2_format.setTimeZone(TimeZone.getTimeZone(timezone_id));
		time3_format.setTimeZone(TimeZone.getTimeZone(timezone_id));
		time4_format.setTimeZone(TimeZone.getTimeZone(timezone_id));
		date2_format.setTimeZone(TimeZone.getTimeZone(timezone_id));
		date3_format.setTimeZone(TimeZone.getTimeZone(timezone_id));
	}

	public static long millisFrom_yyyymmdd(String yyyymmdd) throws Exception {return date2_format.parse(yyyymmdd).getTime();}
	public static long millisFrom_ddmmyy(String ddmmyy) throws Exception {return date3_format.parse(ddmmyy).getTime();}

	public static long millisFrom_HHMMSS(String HHMMSS) throws Exception {return time2_format.parse(HHMMSS).getTime();}
	public static long millisFrom_HHMMSSpSSS(String HHMMSSpSSS) throws Exception {return time3_format.parse(HHMMSSpSSS).getTime();}
	public static long millisFrom_HHcMMcSS(String HHcMMcSS) throws Exception {return time4_format.parse(HHcMMcSS).getTime();}

	public static String yyyymmdd_from_ddmmyy(String ddmmyy) throws Exception {return date2_format.format(date3_format.parse(ddmmyy));}

	public static String now() {return date_time_format.format(new Date());}
	public static long nowMillis() {return new Date().getTime();}

	public static long millisFromDate(String date) throws Exception {return (date_format.parse(date).getTime());}
	public static long millisFromTime(String time) throws Exception {return (time_format.parse(time).getTime());}
	public static long millisFromDateTime(String date_time) throws Exception {return (date_time_format.parse(date_time)).getTime();}

	public static String dateFromMillis(long millis) {return date_format.format(new Date(millis));}
	public static String timeFromMillis(long millis) {return time_format.format(new Date(millis));}
	public static String timeCFromMillis(long millis) {return time4_format.format(new Date(millis));}

	public static String dateTimeFromMillis(long millis) {return date_time_format.format(new Date(millis));}
	public static String dateTimeFromMillisShort(long millis) {return date_time_format_short.format(new Date(millis));}

	public static String dateFromDateTime(String date_time) throws Exception {return date_format.format(date_time_format.parse(date_time));}
	public static String timeFromDateTime(String date_time) throws Exception {return time_format.format(date_time_format.parse(date_time));}
	public static long millisSinceStartOfDayFromMillis(long millis) throws Exception {return millis-millisFromDate(dateFromMillis(millis));}

	public static String formatTimeLeadingZeros(float time) {return df_time_zero.format(time);}

	public static void main(String[] args) throws Exception
	{
		setTimeZoneUTC(); ///important to call
		System.err.println("1 "+now());
		System.err.println("2 "+nowMillis());
		System.err.println("3 "+dateTimeFromMillis(nowMillis()));
		System.err.println("4 "+dateTimeFromMillis(1000l));
		System.err.println("5 "+millisFromDateTime("2016-03-06_07:53:13.411"));
		System.err.println("6 "+dateTimeFromMillis(millisFromDateTime("2016-03-06_07:53:13.411")));
		System.err.println("7 "+millisFromDate("1970-01-01"));
		System.err.println("8 "+millisFromTime("00:01:01.234"));
		System.err.println("9 "+dateFromMillis(1457250793411l));
		System.err.println("10 "+timeFromMillis(1457250793411l));
		System.err.println("11 "+timeFromMillis(3600000l));
		System.err.println("12 "+timeCFromMillis(10921000l));
		System.err.println("13 "+dateFromDateTime("2016-03-06_07:53:13.411"));
		System.err.println("14 "+timeFromDateTime("2016-03-06_07:53:13.411"));
		System.err.println("15 "+millisSinceStartOfDayFromMillis(1457250793411l)/1000d);
		System.err.println("16 "+millisFrom_ddmmyy("270316"));
		System.err.println("17 "+millisFrom_yyyymmdd("20160327"));
		System.err.println("18 "+yyyymmdd_from_ddmmyy("270316"));
		System.err.println("19 "+dateFromMillis(millisFrom_ddmmyy("270316")));
		System.err.println("20 "+millisFrom_HHMMSS("030201"));
		System.err.println("21 "+millisFrom_HHMMSSpSSS("030201.001"));
		System.err.println("22 "+millisFrom_HHcMMcSS("03:02:01"));
		System.err.println("23 "+dateTimeFromMillis(
			millisFrom_ddmmyy("270316")+millisFrom_HHMMSSpSSS("030201.78934")
		));
		System.err.println("24 "+formatTimeLeadingZeros(0.001f));
		System.err.println("25 "+formatTimeLeadingZeros(314.5f));
		System.err.println("26 "+formatTimeLeadingZeros(131415f));
		System.err.println("27 "+dateTimeFromMillisShort(nowMillis()));
	}
}//end class DTime
//EOF
