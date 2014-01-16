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
		commands.add( "echo 'command 1'");
		commands.add( "echo 'command 2'");
		commands.add( "echo 'command 3'");
		commands.add( "echo 'command 4'");
		commands.add( "echo 'command 5'");
		commands.add( "echo 'command 6'");
		commands.add( "echo 'command 7'");
		commands.add( "echo 'command 8'");
		returns = ssh.executeCommand( commands );
		
		returns.add( ssh.executeCommand( "echo 'command a'" ));
		returns.add( ssh.executeCommand( "echo 'command b'" ));
		returns.add( ssh.executeCommand( "echo 'command c'" ));
		returns.add( ssh.executeCommand( "echo 'command d'" ));
		returns.add( ssh.executeCommand( "echo 'command e'" ));
		returns.add( ssh.executeCommand( "echo 'command f'" ));
		returns.add( ssh.executeCommand( "echo 'command g'" ));
		returns.add( ssh.executeCommand( "echo 'command h'" ));
		
		ssh.uninit();
		
		if(returns != null)
		{
			for(String i : returns)
			{
				if(i != null && i.length() > 0)
				{
					System.out.println("======DEBUG======" + i);
				}
				else
				{
					System.out.println("======ERROR======" + i);
				}
			}
		}
	}

}
