package org.pcltool.debug;

import org.pcltool.*;
import org.pcltool.log.*;
import org.pcltool.util.*;

public class DebugFrame
{

	public static void main( String[] args )
	{
		// TODO Auto-generated method stub
		SshConsole ssh = new SshConsole();
		ssh.init( "10.124.115.213", SshConsole.DEFAULT_PORT, "root", "cscenter" );
		String command = "echo 'This is a complex test!'";
		ssh.executeCommand( command );
		ssh.uninit();
	}

}
