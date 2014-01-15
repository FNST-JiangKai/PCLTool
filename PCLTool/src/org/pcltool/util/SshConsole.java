package org.pcltool.util;

import java.util.*;
import java.io.*;

import org.apache.sshd.*;
import org.apache.sshd.client.channel.*;
import org.apache.sshd.client.future.*;
import org.pcltool.log.LogCollector;

public class SshConsole
{
	public static final int DEFAULT_PORT = 22;

	private static final int CONSOLE_SIZE = 1024;
	private static final int WAIT = 1000;
	private static final int BUFF_SIZE = 1024;

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
	volatile private int cmdFinishFlag = FLAG_ON;
	volatile private int dataFinishFlag = FLAG_ON;
	volatile private int count = 0;

	private LogCollector sshLog = LogCollector.createLogCollector( "sshLog" );
	private ArrayList< String > result = new ArrayList< String >();

	public SshConsole()
	{

	}

	public SshConsole( String remoteIP, int remotePort,
			String username, String password )
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
		execThreadSwitch = FLAG_OFF;
		dataThreadSwitch = FLAG_OFF;

		try
		{
			readCommand.close();
			writeCommand.close();
			readData.close();
			writeData.close();
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

	public ArrayList< String > executeCommand( ArrayList< String > commands )
	{
		try
		{
			int size = commands.size();
			cmdFinishFlag = FLAG_OFF;
			count = size;

			writeCommand.write( commands.get( 0 ).getBytes() );
			for ( int i = 1; i < size; i++ )
			{
				while ( cmdFinishFlag == FLAG_OFF )
				// 检测上一条命令是否已执行完毕
				{
					synchronized ( sshLog )
					{
						sshLog.logToConsole(
								"Last command has not finished. Wait",
								LogCollector.INFO );
						Thread.sleep( WAIT );
					}
				}
				synchronized ( sshLog )
				{
					sshLog.logToConsole( "Write commands...", LogCollector.INFO );
				}
				writeCommand.write( commands.get( i ).getBytes() );
			}
			synchronized ( result )
			{
				while ( count > 0 )
				{
					synchronized ( sshLog )
					{
						sshLog.logToConsole(
								"Excution has not finished. Wait.\nProgress "
										+ count + "/" + commands.size(),
								LogCollector.INFO );
						Thread.sleep( WAIT );
					}
				}

				return result;
			}
		}
		catch ( InterruptedException e )
		{
			// TODO Auto-generated catch block
			synchronized ( sshLog )
			{
				sshLog.logToConsole(
						"Command execution terminated because of interruption.",
						LogCollector.WARN );
				sshLog.logToConsole( e.getMessage(), LogCollector.WARN );
			}
			synchronized ( result )
			{
				return result;
			}
		}
		catch ( IOException e )
		{
			synchronized ( sshLog )
			{
				sshLog.logToConsole( "Failed to write commands to pipes.",
						LogCollector.WARN );
				sshLog.logToConsole( e.getMessage(), LogCollector.WARN );
			}
			synchronized ( result )
			{
				return result;
			}
		}
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
				System.out.println("[DEBUG]Session alive");
				sshSession.close( false );
				sshSession = null;
			}
			if ( sshConnection != null )
			{
				System.out.println("[DEBUG]Connection alive");
				sshConnection.cancel();
				sshConnection = null;
			}
			if ( sshClient != null )
			{
				System.out.println("[DEBUG]Client alive");
				sshClient.stop();
			}
		}

		public void run()
		{
			try
			{
				init();
				while ( execThreadSwitch == FLAG_ON )
				// 当exec线程开关打开时，线程循环执行。
				{
					StringBuilder cmd = new StringBuilder();
					byte[] buff = null;
					int length = 0;

					ByteArrayInputStream inputCommand = null;

					while ( (length = readCommand.available()) > 0 )
					// 当管道不为空时，读取管道数据
					{
						buff = new byte[ length ];
						readCommand.read( buff );
						cmd.append( new String( buff ) );
					}
					if ( cmd != null && cmd.length() > 0 )
					// 当取得的命令不为null且不为空字符串时执行命令
					{
						while ( dataFinishFlag == FLAG_OFF )
						// 上一次命令执行的结果尚未被全部保存，等待data线程收集数据。
						{
							synchronized ( sshLog )
							{
								sshLog.logToConsole(
										"Data of last command has not been saved. Wait...",
										LogCollector.INFO );
							}
							Thread.sleep( WAIT );
						}
						sshChannel = sshSession.createChannel( "shell" );
						if ( sshChannel != null )
						{
							synchronized ( sshLog )
							{
								sshLog.logToConsole(
										"Get SSH channel successfully!",
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
						inputCommand = new ByteArrayInputStream( cmd.toString()
								.getBytes() );
						cmd.delete( 0, cmd.length() );

						sshShell.setIn( inputCommand );
						sshShell.setOut( writeData );
						dataFinishFlag = FLAG_OFF;
						sshShell.open().await();
						sshShell.waitFor( ClientChannel.CLOSED, 0 );
						// 设定标志位，表示命令执行完毕。
						cmdFinishFlag = FLAG_ON;
						count--;
					}
					else
					{
						synchronized ( sshLog )
						{
							sshLog.logToConsole( "Empty command. Wait...",
									LogCollector.INFO );
						}
						Thread.sleep( WAIT );
						continue;
					}
				}
			}
			catch ( InterruptedException e )
			{
				// TODO Auto-generated catch block
				synchronized ( sshLog )
				{
					sshLog.logToConsole(
							"Command execution thread terminated because of interruption.",
							LogCollector.WARN );
					sshLog.logToConsole( e.getMessage(), LogCollector.WARN );
				}
			}
			catch ( IOException e )
			{
				// TODO Auto-generated catch block
				synchronized ( sshLog )
				{
					sshLog.logToConsole(
							"Command execution thread terminated because of pipe problem.",
							LogCollector.WARN );
					sshLog.logToConsole( e.getMessage(), LogCollector.WARN );
				}
			}
			finally
			{
				cmdFinishFlag = FLAG_ON;
				execThreadSwitch = FLAG_OFF;
				uninit();
			}
		}
	}

	private class GetDataThread implements Runnable
	{
		private StringBuilder buffer = new StringBuilder();

		public void run()
		{
			int length = 0;
			byte[] ret = null;
			try
			{
				while ( dataThreadSwitch == FLAG_ON )
				// 当data线程开关打开时，线程循环执行。
				{
					if ( (length = readData.available()) == 0
							&& buffer.length() > 0
							&& dataFinishFlag == FLAG_OFF )
					// 当管道中数据长度为0.且缓冲区不为空时，表示数据传输完毕。
					{
						// 将缓存中的数据添加到命令返回值列表中。
						synchronized ( result )
						{
							result.add( buffer.toString() );
						}
						buffer.delete( 0, buffer.length() );
						ret = null;
						dataFinishFlag = FLAG_ON;
						sshShell.close( false );
					}
					else
					{
						ret = new byte[ length ];
						readData.read( ret );
						buffer.append( new String( ret ) );
					}
					ret = null;
				}
			}
			catch ( IOException e )
			{
				// TODO Auto-generated catch block
				synchronized ( sshLog )
				{
					sshLog.logToConsole(
							"Data collection thread terminated because of pipe problem.",
							LogCollector.WARN );
					sshLog.logToConsole( e.getMessage(), LogCollector.WARN );
				}
			}
			finally
			{
				dataThreadSwitch = FLAG_OFF;
			}
		}
	}
}
