package util;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.IOException;

import java.util.List;
import java.util.ArrayList;

import java.util.zip.ZipOutputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;

//tb/1608

//=============================================================================
//=============================================================================
public class Zipper
{
/*
//=============================================================================
	public static void main(String[] args) throws Exception
	{
		List<File> files=new ArrayList<File>();
		files.add(new File("/tmp/a.b"));
		files.add(new File("/tmp/c.b"));

//		streamFilesToZip(files, new FileOutputStream("/tmp/testarchive.zip"));
		streamFilesToZip(files, System.out);
	}
*/

//=============================================================================
	public static void streamFilesToZip(List<File> files, OutputStream os) throws IOException
	{
		ZipOutputStream zip = new ZipOutputStream(os);

		for(File f : files)
		{
			InputStream is = new FileInputStream(f);
			zip.putNextEntry(new ZipEntry(f.getName()));
			int length;

			byte[] b = new byte[4096];
			while((length = is.read(b)) > 0)
			{
				zip.write(b,0,length);
			}
			zip.closeEntry();
			is.close();
		}
		zip.close();
	}

//=============================================================================
	public static void streamFileAsGzip(File f, OutputStream os) throws IOException
	{
		GZIPOutputStream gzip = new GZIPOutputStream(os);

		InputStream is = new FileInputStream(f);
		int length;

		byte[] b = new byte[4096];
		while((length = is.read(b)) > 0)
		{
			gzip.write(b,0,length);
		}
		is.close();
		gzip.close();
	}
}//end class Zipper
//EOF
