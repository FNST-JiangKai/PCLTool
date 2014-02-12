package org.pcltool.util;

import java.io.*;

public class LocalConsole
{
	private ProcessBuilder pb = null;
	private Process p = null;
	private String output = "";
	private int returncode = 0;
	private String command = "";
	private String args = "";

	public LocalConsole( String command )
	{
		this.command = command;
	}

	public LocalConsole( String command, String args )
	{
		this.command = command;
		this.args = args;
	}

	public void executeCommand()
	{
		pb = new ProcessBuilder();
		pb.command( command, args );
		pb.redirectErrorStream( true );
		try
		{
			p = pb.start();
			InputStream is = p.getInputStream();
			BufferedReader br = new BufferedReader( new InputStreamReader( is,
					System.getProperty("sun.jnu.encoding") ) );
			String tmp = br.readLine();
			while ( tmp != null )
			{
				output = output + tmp + "\n";
				tmp = br.readLine();
			}
		}
		catch ( IOException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public int getReturnValue()
	{
		return returncode;
	}

	public String getOutput()
	{
		return output;
	}
}
