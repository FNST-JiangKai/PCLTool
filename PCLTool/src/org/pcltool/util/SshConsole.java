package org.pcltool.util;

import java.io.*;

import org.apache.sshd.*;
import org.apache.sshd.client.channel.*;
import org.apache.sshd.client.future.*;
import org.pcltool.log.LogCollector;

public class SshConsole
{
	public static final int DEFAULT_PORT = 22;
	public static final int CONSOLE_SIZE = 1024;
	public static final int TIMEOUT = 5;
	public static final int WAIT = 1000;

	private String remoteIP = null;
	private int remotePort = 0;
	private String username = null;
	private String password = null;

	private LogCollector sshLog = LogCollector.createLogCollector( "sshLog" );

	private SshClient sshClient = null;
	private ConnectFuture sshConnection = null;
	private ClientSession sshSession = null;

	private PipedInputStream input = null;
	private PipedOutputStream output = null;
	private StringBuilder result = new StringBuilder();
	private Thread exec = null;
	private Thread data = null;

	public SshConsole()
	{

	}

	public SshConsole( String remoteIP, int remotePort, String username,
			String password )
	{
		this.remoteIP = remoteIP;
		this.remotePort = remotePort;
		this.username = username;
		this.password = password;
	}

	public void init()
	{
		uninit();
		if ( remoteIP == null || username == null || password == null )
		{
			sshLog.logToConsole( "Initialize failed! Missing arguments!",
					LogCollector.ERROR );
			throw new IllegalArgumentException( "Missing arguments" );
		}
		if ( !SimpleUtil.checkIP( remoteIP ) )
		{
			sshLog.logToConsole( "Initialize failed! Invalid IP address!",
					LogCollector.ERROR );
			throw new IllegalArgumentException( "Invalid IP address!" );
		}
		if ( remotePort == 0 )
		{
			remotePort = SshConsole.DEFAULT_PORT;
		}

		try
		{
			sshClient = SshClient.setUpDefaultClient();
			sshClient.start();
			sshConnection = sshClient.connect( remoteIP, remotePort ).await();
			if ( sshConnection != null )
			{
				sshLog.logToConsole( "Connect SSH successfully!",
						LogCollector.OK );
			}
			sshSession = sshConnection.getSession();
			if ( sshSession != null )
			{
				sshLog.logToConsole( "Get SSH session successfully!",
						LogCollector.OK );
			}
			int ret = ClientSession.WAIT_AUTH;
			while ( (ret & ClientSession.WAIT_AUTH) != 0 )
			{
				sshSession.authPassword( username, password );
				ret = sshSession.waitFor( ClientSession.AUTHED
						| ClientSession.CLOSED | ClientSession.TIMEOUT, 0 );
			}
			if ( (ret & ClientSession.CLOSED) != 0 )
			{
				throw new IOException(
						"Session has been closed with unknown reason!" );
			}
		}
		catch ( InterruptedException e )
		{
			// TODO Auto-generated catch block
			sshLog.logToConsole( "Initialize failed! Thread is interrupted!",
					LogCollector.ERROR );
			sshLog.logToConsole( e.getMessage(), LogCollector.ERROR );
		}
		catch ( IOException e )
		{
			// TODO Auto-generated catch block
			sshLog.logToConsole( "Initialize failed! Failed to connect to "
					+ remoteIP, LogCollector.ERROR );
			sshLog.logToConsole( e.getMessage(), LogCollector.ERROR );
		}
	}

	public void init( String remoteIP, int remotePort, String username,
			String password )
	{
		this.remoteIP = remoteIP;
		this.remotePort = remotePort;
		this.username = username;
		this.password = password;
		init();
	}

	public void uninit()
	{
		if ( sshSession != null )
		{
			sshSession.close( true );
			sshSession = null;
		}
		if ( sshConnection != null )
		{
			sshConnection.cancel();
			sshConnection = null;
		}
		if ( sshClient != null )
		{
			sshClient.stop();
			sshClient = null;
		}
	}

	public String executeCommand( String command )
	{
		ClientChannel sshChannel = null;
		ChannelShell sshShell = null;

		StringBuilder result = new StringBuilder();

		try
		{
			sshChannel = sshSession.createChannel( "shell" );
			if ( sshChannel != null )
			{
				sshLog.logToConsole( "Get SSH channel successfully!",
						LogCollector.OK );
			}
			if ( sshChannel instanceof ChannelShell )
			{
				sshShell = (ChannelShell)sshChannel;
				sshLog.logToConsole( "Convert ClientChannel to ChannelShell.",
						LogCollector.INFO );
			}

			sshLog.logToConsole( "Create pipes for data.", LogCollector.INFO );
			input = new PipedInputStream();
			output = new PipedOutputStream();
			input.connect( output );

			sshLog.logToConsole( "Create execute thread.", LogCollector.INFO );
			exec = new Thread( new ExecuteThread( sshShell, command ) );
			sshLog.logToConsole( "Create getdata thread.", LogCollector.INFO );
			data = new Thread( new GetDataThread() );

			while ( exec.isAlive() || data.isAlive() )
			{
				sshLog.logToConsole( "Waiting for exec thread and data thread finish.", LogCollector.INFO );
			}
			return result.toString();
		}
		catch ( IOException e )
		{
			// TODO Auto-generated catch block
			sshLog.logToConsole(
					"Command execution failed! There is something wrong with SSH.",
					LogCollector.ERROR );
			sshLog.logToConsole( e.getMessage(), LogCollector.ERROR );
			return null;
		}
	}

	private class ExecuteThread implements Runnable
	{
		private ChannelShell sshShell = null;
		private String command = null;

		private LogCollector sshLog = null;
		private String threadId = SimpleUtil
				.generateTimestamp( SimpleUtil.TIMESTAMP_ALL );

		public ExecuteThread( ChannelShell sshShell, String command )
		{
			this.sshShell = sshShell;
			this.command = command;

			sshLog = LogCollector.createLogCollector( threadId );
		}

		public void run()
		{
			try
			{
				sshShell.setPtyColumns( CONSOLE_SIZE );
				ByteArrayInputStream inputCommand = new ByteArrayInputStream(
						command.getBytes() );

				sshShell.setIn( inputCommand );
				sshShell.setOut( output );
				sshShell.open().await();
			}
			catch ( InterruptedException e )
			{
				// TODO Auto-generated catch block
				sshLog.logToConsole(
						"Execution thread terminated because of signal.",
						LogCollector.INFO );
				sshLog.logToConsole( e.getMessage(), LogCollector.INFO );
			}
			catch ( IOException e )
			{
				// TODO Auto-generated catch block
				sshLog.logToConsole(
						"Execution thread terminated because pipe closed.",
						LogCollector.INFO );
				sshLog.logToConsole( e.getMessage(), LogCollector.INFO );
			}
		}
	}

	private class GetDataThread implements Runnable
	{
		private LogCollector sshLog = null;
		private String threadId = SimpleUtil
				.generateTimestamp( SimpleUtil.TIMESTAMP_ALL );

		public GetDataThread()
		{
			sshLog = LogCollector.createLogCollector( threadId );
		}

		public void run()
		{
			int length = 0;
			int timeout = 0;
			byte[] ret = null;
			try
			{
				while ( true )
				{
					while ( (length = input.available()) == 0 )
					{
						if ( timeout < SshConsole.TIMEOUT )
						{
							input.close();
							throw new IOException( "Pipe timeout!" );
						}
						Thread.sleep( SshConsole.WAIT );
						timeout++;
						sshLog.logToConsole( "Pipe is empty.",
								LogCollector.INFO );
						ret = new byte[ length ];
						input.read( ret );
						if ( ret != null )
						{
							result.append( new String( ret ) );
						}
						ret = null;
					}
				}
			}
			catch ( IOException e )
			{
				// TODO Auto-generated catch block
				sshLog.logToConsole(
						"Data collection thread terminated because pipe timeout.",
						LogCollector.INFO );
				sshLog.logToConsole( e.getMessage(), LogCollector.INFO );
			}
			catch ( InterruptedException e )
			{
				// TODO Auto-generated catch block
				sshLog.logToConsole(
						"Data collection thread terminated because pipe closed.",
						LogCollector.INFO );
				sshLog.logToConsole( e.getMessage(), LogCollector.INFO );
			}
			finally
			{
				sshLog.logToConsole( "Terminate the exec thread!",
						LogCollector.ACT );
				exec.interrupt();
			}
		}
	}
}
