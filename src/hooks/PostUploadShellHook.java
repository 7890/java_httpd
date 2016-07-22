package hooks;
import util.*;
import interfaces.*;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import java.util.Enumeration;
import java.util.Properties;
import java.util.Arrays;

//========================================================================
//========================================================================
public class PostUploadShellHook implements PostUpload
{
	//this file is loaded if found in current directory
	private static String propertiesFileUri="PostUploadShellHook.properties";

	//===configurable parameters (here: default values)
	public String script_uri="resources/testhook.sh";
	//===end configurable parameters

//========================================================================
	public PostUploadShellHook()
	{
		if(!LProps.load(propertiesFileUri,this))
		{
			System.err.println("/!\\ could not load properties");
		}
	}

//http://www.xyzws.com/javafaq/how-to-run-external-programs-by-using-java-processbuilder-class/189
//========================================================================
	public String postUploadProcess(File file, String originalFilename) throws Exception
	{
		String file_uri=file.getAbsolutePath();
		String[] command = {script_uri, file_uri, originalFilename} ;

		ProcessBuilder probuilder = new ProcessBuilder( command );
///		probuilder.directory(new File("/tmp"));
		Process process = probuilder.start();
	
		//read process output
		InputStream is = process.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String line=null;
		String last_line=null;
		System.out.printf("Output of running %s is:\n",Arrays.toString(command));
		while ((line = br.readLine()) != null)
		{
			System.out.println(line);
			last_line=line;
		}
		
		//wait to get exit value
		try
		{
			int exitValue = process.waitFor();
			System.out.println("post upload hook exit value is " + exitValue);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		return last_line;
	}//end postUploadProcess
}//end class PostUploadShellHook
//EOF
