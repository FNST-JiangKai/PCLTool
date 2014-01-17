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
		testPathHandler();
	}

	public static void testPathHandler()
	{
		ScpConsole scpConsole = new ScpConsole();
		scpConsole.init( "10.124.115.213", SshConsole.DEFAULT_PORT, "root", "cscenter" );

		String[] locals = {
				"W:\\scpTest\\upload\\test.txt",
				"W:\\scpTest\\upload\\中文文件.txt",
				"W:\\scpTest\\upload\\中文测试\\",
				"W:\\scpTest\\download\\download.txt",
				"W:\\scpTest\\download\\日文.txt",
				"W:\\scpTest\\download\\badpath\\dir\\",
		};
		
		String[] remotes = {
				"/sf9-fnst/scp/upload/fromwin.txt",
				"/sf9-fnst/scp/upload/badpath/fromwin.txt",
				"/sf9-fnst/scp/upload/test/",
				"/sf9-fnst/scp/download/testfile.txt",
				"/sf9-fnst/scp/download/ディレクトリ.txt",
				"/sf9-fnst/scp/download/そのよう/",
		};
		
		for(int i = 0 ; i < 3 ; i++)
		{
			scpConsole.upload( locals[i], remotes[i] );
		}
		
		for(int i = 3 ; i < 6 ; i++)
		{
			scpConsole.download( remotes[i], locals[i] );
		}
		
		scpConsole.uninit();
	}

	public static void testSshConsole()
	{
		SshConsole ssh = new SshConsole();
		ssh.init( "10.124.115.213", SshConsole.DEFAULT_PORT, "root", "cscenter" );
		ArrayList< String > commands = new ArrayList< String >();
		ArrayList< String > returns = null;
		commands.add( "echo 'command 1'" );
		commands.add( "echo 'command 2'" );
		commands.add( "echo 'command 3'" );
		commands.add( "echo 'command 4'" );
		commands.add( "echo 'command 5'" );
		commands.add( "echo 'command 6'" );
		commands.add( "echo 'command 7'" );
		commands.add( "echo 'command 8'" );
		returns = ssh.executeCommand( commands );

		returns.add( ssh.executeCommand( "echo 'command a'" ) );
		returns.add( ssh.executeCommand( "echo 'command b'" ) );
		returns.add( ssh.executeCommand( "echo 'command c'" ) );
		returns.add( ssh.executeCommand( "echo 'command d'" ) );
		returns.add( ssh.executeCommand( "echo 'command e'" ) );
		returns.add( ssh.executeCommand( "echo 'command f'" ) );
		returns.add( ssh.executeCommand( "echo 'command g'" ) );
		returns.add( ssh.executeCommand( "echo 'command h'" ) );

		ssh.uninit();

		if ( returns != null )
		{
			for ( String i : returns )
			{
				if ( i != null && i.length() > 0 )
				{
					System.out.println( "======DEBUG======" + i );
				}
				else
				{
					System.out.println( "======ERROR======" + i );
				}
			}
		}
	}
}
