package org.pcltool.util;

import java.io.*;
import java.util.*;

import org.pcltool.log.LogCollector;

public class ScriptParser
{
	private SshConsole sshConsole = null;
	private CommandNode head = null;
	private LogCollector parseLog = LogCollector
			.createLogCollector( "parseLog" );

	public ScriptParser()
	{

	}

	public CommandNode parseScript( String script )
	{
		File f = null;
		FileInputStream fis = null;
		InputStreamReader isr = null;
		BufferedReader reader = null;
		String line = null;
		
		int flag = 0;
		Stack< CommandNode > stack = new Stack< CommandNode >();
		CommandNode current = null;
		CommandNode back = null;

		try
		{
			f = new File( script );
			fis = new FileInputStream( f );
			isr = new InputStreamReader( fis, "UTF-8" );
			reader = new BufferedReader( isr );

			while ( (line = reader.readLine()) != null )
			{
				line = line.trim();
				String start = line.substring( 0, line.indexOf( " " ) );
				if ( start.compareToIgnoreCase( "IF" ) == 0 )
				{
					line = line.substring( line.indexOf( " " ) + 1 );
					flag = 1;
					back = generateNode(line);
					stack.push( back );
				}
				if ( start.compareToIgnoreCase( "ELSE" ) == 0 )
				{
					line = line.substring( line.indexOf( " " ) + 1 );
					flag = 2;
				}
				if ( start.compareToIgnoreCase( "WHILE" ) == 0 )
				{
					line = line.substring( line.indexOf( " " ) + 1 );
					flag = 3;
				}
				if ( start.compareToIgnoreCase( "END" ) == 0 )
				{
					line = line.substring( line.indexOf( " " ) + 1 );
					flag = 4;
				}
				if ( current == null )
				{
					current = generateNode( line );
				}
				else
				{
					current.setNext( "default", generateNode( line ) );
					current = current.getNext( "default" );
				}
			}

			reader.close();
		}
		catch ( FileNotFoundException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch ( UnsupportedEncodingException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch ( IOException e )
		{
			parseLog.logToConsole( "", LogCollector.ERROR );
			parseLog.logToConsole( e.getMessage(), LogCollector.ERROR );
		}

		return head;
	}

	private CommandNode generateNode( String line )
	{
		CommandNode result = new CommandNode();

		return result;
	}

	private class CommandNode
	{
		private String command = null;
		private HashMap< String, CommandNode > nexts = new HashMap< String, CommandNode >();

		public CommandNode()
		{

		}

		public void setCommand( String command )
		{
			this.command = command;
		}

		public String getCommand()
		{
			return this.command;
		}

		public void setNext( String type, CommandNode next )
		{
			nexts.put( type, next );
		}

		public CommandNode getNext( String type )
		{
			CommandNode next = nexts.get( type );
			if ( next == null )
			{
				return nexts.get( "default" );
			}
			return next;
		}

		public String execute()
		{
			return returnHandle( sshConsole.executeCommand( command ) );
		}

		public String returnHandle( String ret )
		{
			StringBuilder result = new StringBuilder( ret );

			return result.toString();
		}
	}
}
