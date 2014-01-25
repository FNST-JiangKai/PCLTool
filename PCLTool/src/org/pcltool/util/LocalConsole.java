package org.pcltool.util;

//import java.io.*;

class LocalConsole
{
	public LocalConsole()
	{
		System.out.print( "" );
	}
	public String executeCommand(String command)
	{
		System.out.print( command );
		return "Hello World.";
	}
	
}
