package org.pcltool.debug;

import org.pcltool.*;
import org.pcltool.log.*;
import org.pcltool.util.*;

import java.io.*;
import java.util.*;

public class DebugFrame
{

	public static void main( String[] args )
	{
		// TODO Auto-generated method stub
		//SshConsoleWithFile ssh = new SshConsoleWithFile();
		SshConsole ssh = new SshConsole();
		ssh.init( "10.124.115.213", SshConsole.DEFAULT_PORT, "root", "cscenter" );
		ArrayList<String> commands = new ArrayList<String>();
		ArrayList<String> returns = null;
		commands.add( "echo 'This is a complex test!'\n");
		commands.add( "who -r\n" );
		returns = ssh.executeCommand( commands );
		ssh.uninit();
		
		if(returns != null)
		{
			for(String i : returns)
			{
				System.out.println("[DEBUG]" + i);
			}
		}
		else
		{
			System.out.println("Test failed.");
		}
	}

}
