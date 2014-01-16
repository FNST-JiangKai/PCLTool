package org.pcltool.util;

import java.util.*;
import java.io.*;

import org.apache.sshd.*;
import org.apache.sshd.client.channel.*;
import org.apache.sshd.client.future.*;
import org.apache.sshd.common.util.*;
import org.pcltool.log.LogCollector;

public class SshConsole
{
	public static final int DEFAULT_PORT = 22;

	private static final int CONSOLE_SIZE = 1024;
	private static final int BUFF_SIZE = 1024;
	private static final int WAIT = 1000;

	private static final int FLAG_ON = 1;
	private static final int FLAG_OFF = 0;

	private String remoteIP = null;
	private int remotePort = 0;
	private String username = null;
	private String password = null;

	private PipedInputStream readCommand = null;
	private PipedOutputStream writeCommand = null;
	private PipedInputStream readData = null;
	private PipedOutputStream writeData = null;

	private ChannelShell sshShell = null;

	volatile private int execThreadSwitch = FLAG_OFF;
	volatile private int dataThreadSwitch = FLAG_OFF;
	volatile private int dataFinishFlag = FLAG_ON;

	private LogCollector sshLog = LogCollector.createLogCollector( "sshLog" );
	private StringBuffer result = new StringBuffer();

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
		try
		{
			Thread exec = null;
			Thread data = null;

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
			synchronized ( sshLog )
			{
				sshLog.logToConsole( "Create pipes for command.",
						LogCollector.INFO );
			}
			readCommand = new PipedInputStream( BUFF_SIZE );
			writeCommand = new PipedOutputStream();
			readCommand.connect( writeCommand );
			synchronized ( sshLog )
			{
				sshLog.logToConsole( "Create pipes for data.",
						LogCollector.INFO );
			}
			readData = new PipedInputStream( BUFF_SIZE );
			writeData = new PipedOutputStream();
			readData.connect( writeData );

			execThreadSwitch = FLAG_ON;
			dataThreadSwitch = FLAG_ON;
			synchronized ( sshLog )
			{
				sshLog.logToConsole( "Create exec thread.", LogCollector.INFO );
				exec = new Thread( new ExecuteThread() );
				sshLog.logToConsole( "Create data thread.", LogCollector.INFO );
				data = new Thread( new GetDataThread() );
			}
			exec.start();
			data.start();
		}
		catch ( IllegalArgumentException e )
		{
			synchronized ( sshLog )
			{
				sshLog.logToConsole(
						"Initial failed becasue of illegal argument!",
						LogCollector.ERROR );
				sshLog.logToConsole( e.getMessage(), LogCollector.ERROR );
			}
		}
		catch ( IOException e )
		{
			// TODO Auto-generated catch block
			synchronized ( sshLog )
			{
				sshLog.logToConsole(
						"Initial failed because pipe can not be created!",
						LogCollector.ERROR );
				sshLog.logToConsole( e.getMessage(), LogCollector.ERROR );
			}
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
		try
		{
			if ( sshShell != null )
			{
				synchronized ( sshLog )
				{
					sshLog.logToConsole( "Closing SSH channel...",
							LogCollector.INFO );
				}
				sshShell.close( false );
				sshShell = null;
			}

			readCommand.close();
			writeCommand.close();
			readData.close();
			writeData.close();

			execThreadSwitch = FLAG_OFF;
			dataThreadSwitch = FLAG_OFF;
		}
		catch ( IOException e )
		{
			// TODO Auto-generated catch block
			synchronized ( sshLog )
			{
				sshLog.logToConsole( "Pipe closed.", LogCollector.WARN );
				sshLog.logToConsole( e.getMessage(), LogCollector.WARN );
			}
		}
	}

	public String executeCommand( String command )
	{
		try
		{
			String temp = null;
			if ( command.charAt( command.length() - 1 ) != '\n' )
			{
				command = command + "\n";
			}
			synchronized ( sshLog )
			{
				sshLog.logToConsole( "Write commands...", LogCollector.INFO );
			}
			writeCommand.write( command.getBytes() );

			dataFinishFlag = FLAG_OFF;
			while ( dataFinishFlag == FLAG_OFF )
			{
				synchronized ( sshLog )
				{
					sshLog.logToConsole( "Waiting results...",
							LogCollector.INFO );
				}
				Thread.sleep( WAIT );
			}
			synchronized ( result )
			{
				temp = result.toString();
				result.delete( 0, result.length() );
			}
			return temp;
		}
		catch ( InterruptedException e )
		{
			synchronized ( sshLog )
			{
				sshLog.logToConsole(
						"Execution failed because of interruption.",
						LogCollector.ERROR );
				sshLog.logToConsole( e.getMessage(), LogCollector.ERROR );
			}
			return null;
		}
		catch ( IOException e )
		{
			synchronized ( sshLog )
			{
				sshLog.logToConsole( "Failed to write commands.",
						LogCollector.ERROR );
				sshLog.logToConsole( e.getMessage(), LogCollector.ERROR );
			}
			return null;
		}
	}

	public ArrayList< String > executeCommand( ArrayList< String > commands )
	{
		int all = commands.size();
		int count = 0;
		ArrayList< String > results = new ArrayList< String >();
		for ( String i : commands )
		{
			count++;
			synchronized ( sshLog )
			{
				sshLog.logToConsole( "Progress:" + count + "/" + all,
						LogCollector.INFO );
			}
			results.add( executeCommand( i ) );
		}
		return results;
	}

	private class ExecuteThread implements Runnable
	{
		private SshClient sshClient = null;
		private ConnectFuture sshConnection = null;
		private ClientSession sshSession = null;
		private ClientChannel sshChannel = null;

		private void init()
		{
			try
			{
				synchronized ( sshLog )
				{
					sshLog.logToConsole( "Execution thread initial starts!",
							LogCollector.INFO );
				}
				// 初始化SSH客户端
				sshClient = SshClient.setUpDefaultClient();
				sshClient.start();
				sshConnection = sshClient.connect( remoteIP, remotePort )
						.await();
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
					throw new IllegalStateException(
							"Session has been closed with unknown reason!" );
				}
				synchronized ( sshLog )
				{
					sshLog.logToConsole(
							"Execution thread initial finished successfully!",
							LogCollector.INFO );
				}

				sshChannel = sshSession.createChannel( "shell" );
				if ( sshChannel != null )
				{
					synchronized ( sshLog )
					{
						sshLog.logToConsole( "Get SSH channel successfully!",
								LogCollector.OK );
					}
				}
				if ( sshChannel instanceof ChannelShell )
				{

					sshShell = (ChannelShell)sshChannel;
					synchronized ( sshLog )
					{
						sshLog.logToConsole(
								"Convert ClientChannel to ChannelShell.",
								LogCollector.INFO );
					}
				}
				sshShell.setPtyColumns( CONSOLE_SIZE );
				sshShell.setIn( new NoCloseInputStream( readCommand ) );
				sshShell.setOut( new NoCloseOutputStream( writeData ) );
				sshShell.open().await();
				dataFinishFlag = FLAG_OFF;
			}
			catch ( IllegalStateException e )
			{
				synchronized ( sshLog )
				{
					sshLog.logToConsole( "Failed to create ssh session!",
							LogCollector.ERROR );
					sshLog.logToConsole( e.getMessage(), LogCollector.ERROR );
				}
			}
			catch ( InterruptedException e )
			{
				// TODO Auto-generated catch block
				synchronized ( sshLog )
				{
					sshLog.logToConsole(
							"Command execution thread initial failed because of interruption.",
							LogCollector.ERROR );
					sshLog.logToConsole( e.getMessage(), LogCollector.ERROR );
				}
			}
			catch ( IOException e )
			{
				// TODO Auto-generated catch block
				synchronized ( sshLog )
				{
					sshLog.logToConsole(
							"Command execution thread initial failed because of ssh problem.",
							LogCollector.ERROR );
					sshLog.logToConsole( e.getMessage(), LogCollector.ERROR );
				}
			}
		}

		private void uninit()
		{
			if ( sshSession != null )
			{
				synchronized ( sshLog )
				{
					sshLog.logToConsole( "Closing SSH session...",
							LogCollector.INFO );
				}
				sshSession.close( false );
				sshSession = null;
			}
			if ( sshConnection != null )
			{
				synchronized ( sshLog )
				{
					sshLog.logToConsole( "Closing SSH connection...",
							LogCollector.INFO );
				}
				sshConnection.cancel();
				sshConnection = null;
			}
			if ( sshClient != null )
			{
				synchronized ( sshLog )
				{
					sshLog.logToConsole( "Closing SSH client...",
							LogCollector.INFO );
				}
				sshClient.stop();
			}
		}

		public void run()
		{
			init();
			while ( execThreadSwitch == FLAG_ON )
			// 当exec线程开关打开时，线程循环执行。
			{
				sshShell.waitFor( ClientChannel.CLOSED, 0 );
			}
			uninit();
		}
	}

	private class GetDataThread implements Runnable
	{
		private StringBuilder buffer = new StringBuilder();
		private StringBuilder allData = new StringBuilder();
		private StringBuilder all = new StringBuilder();

		public void run()
		{
			int length = 0;
			byte[] ret = null;
			int waitFlag = FLAG_OFF;
			String content = null;
			try
			{
				while ( dataThreadSwitch == FLAG_ON )
				// 当data线程开关打开时，线程循环执行。
				{
					writeData.flush();
					if ( (length = readData.available()) > 0 )
					// 如果管道中数据不为空则读取数据并重置等待标志。
					{
						ret = new byte[ length ];
						readData.read( ret );
						buffer.append( new String( ret ) );
						allData.append( new String( ret ) );
						waitFlag = FLAG_OFF;
						dataFinishFlag = FLAG_OFF;
					}
					else
					{
						// 当管道中数据为空时，判断命令是否已执行结束。
						if ( isDataEnd() )
						{
							if ( waitFlag == FLAG_ON )
							{
								content = buffer.substring( 0,
										buffer.lastIndexOf( "[" ) );
								if ( content != null && content.length() > 0 )
								{
									synchronized ( result )
									{
										result.append( content );
									}
									all.append( content );
									buffer.delete( 0, buffer.lastIndexOf( "[" ) );
									dataFinishFlag = FLAG_ON;
								}
								Thread.sleep( WAIT );
							}
							else
							{
								waitFlag = FLAG_ON;
								Thread.sleep( WAIT );
							}
						}
					}
				}
			}
			catch ( InterruptedException e )
			{
				synchronized ( sshLog )
				{
					sshLog.logToConsole(
							"Data collection thread terminated because of interruption.",
							LogCollector.ERROR );
					sshLog.logToConsole( e.getMessage(), LogCollector.ERROR );
				}
			}
			catch ( IOException e )
			{
				// TODO Auto-generated catch block
				synchronized ( sshLog )
				{
					sshLog.logToConsole(
							"Data collection thread terminated because of pipe problem.",
							LogCollector.ERROR );
					sshLog.logToConsole( e.getMessage(), LogCollector.ERROR );
				}
			}
			finally
			{
				dataFinishFlag = FLAG_ON;
				dataThreadSwitch = FLAG_OFF;
			}
		}

		private boolean isDataEnd()
		{
			// 根据Linux命令行提示符判断命令是否执行结束。
			// 此判断基于以下事实
			// 当一个命令执行完毕后，最后显示的末尾必定是命令行提示符
			// BUG：此判断在输出恰好与提示符格式一致时会导致误判。
			String flag = buffer.substring( buffer.lastIndexOf( "\n" ) + 1,
					buffer.length() );
			int indexLeft = flag.lastIndexOf( "[" );
			int indexRight = flag.lastIndexOf( "@" );
			if ( indexLeft == -1 || indexRight == -1 || indexLeft > indexRight )
			{
				return false;
			}
			if ( flag.substring( indexLeft, indexRight )
					.equals( "[" + username ) )
			{
				return true;
			}
			return false;
		}
	}
}
