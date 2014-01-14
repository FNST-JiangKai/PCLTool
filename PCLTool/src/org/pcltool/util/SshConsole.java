package org.pcltool.util;

import java.util.*;
import java.io.*;

import org.apache.sshd.*;
import org.apache.sshd.client.channel.*;
import org.apache.sshd.client.future.*;
import org.apache.sshd.common.util.NoCloseOutputStream;
import org.pcltool.log.LogCollector;

public class SshConsole
{
	public static final int DEFAULT_PORT = 22;

	private static final int CONSOLE_SIZE = 1024;
	private static final int WAIT = 1000;

	private static final int FLAG_ON = 1;
	private static final int FLAG_OFF = 0;

	private String remoteIP = null;
	private int remotePort = 0;
	private String username = null;
	private String password = null;

	private PipedInputStream input = null;
	private PipedOutputStream output = null;

	volatile private int execThreadSwitch = FLAG_OFF;
	volatile private int dataThreadSwitch = FLAG_OFF;
	volatile private int cmdFinishFlag = FLAG_ON;
	volatile private int dataFinishFlag = FLAG_ON;

	private StringBuilder commandBuff = new StringBuilder();
	private String commandLocal = null;

	private LogCollector sshLog = LogCollector.createLogCollector( "sshLog" );

	private ArrayList< String > result = new ArrayList< String >();

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

		execThreadSwitch = FLAG_ON;
		dataThreadSwitch = FLAG_ON;

		try
		{
			synchronized ( sshLog )
			{
				sshLog.logToConsole( "Create pipes for data.",
						LogCollector.INFO );
			}
			input = new PipedInputStream();
			output = new PipedOutputStream();
			input.connect( output );
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
		execThreadSwitch = FLAG_OFF;
		dataThreadSwitch = FLAG_OFF;

		try
		{
			input.close();
			output.close();
		}
		catch ( IOException e )
		{
			// TODO Auto-generated catch block
			synchronized ( sshLog )
			{
				sshLog.logToConsole(
						"SSHConsole uninitial failed because of pipe problem.",
						LogCollector.WARN );
				sshLog.logToConsole( e.getMessage(), LogCollector.WARN );
			}
		}
	}

	public ArrayList< String > executeCommand( ArrayList< String > command )
	{
		Thread exec = null;
		Thread data = null;
		try
		{
			synchronized ( result )
			{
				result.clear();
			}
			synchronized ( sshLog )
			{
				sshLog.logToConsole( "Create exec thread.", LogCollector.INFO );
				exec = new Thread( new ExecuteThread() );
				sshLog.logToConsole( "Create data thread.", LogCollector.INFO );
				data = new Thread( new GetDataThread() );
			}
			cmdFinishFlag = FLAG_OFF;
			dataFinishFlag = FLAG_ON;

			int index = 0;
			commandBuff.append( command.get( index ) );
			commandLocal = commandBuff.toString();

			exec.start();
			data.start();

			for ( index = 1; index < command.size(); index++ )
			{
				while ( commandLocal.length() != 0 )
				{
					synchronized ( commandBuff )
					{
						commandLocal = commandBuff.toString();
					}
					synchronized ( sshLog )
					{
						sshLog.logToConsole(
								"Last command has not finished. Wait...",
								LogCollector.INFO );
					}
					Thread.sleep( WAIT );
				}
				commandBuff.append( command.get( index ) );
			}

			synchronized ( commandBuff )
			{
				commandLocal = commandBuff.toString();
			}
			while ( commandLocal.length() > 0 || cmdFinishFlag != FLAG_ON
					|| dataFinishFlag != FLAG_ON )
			// 判断exec线程和data线程是否已执行完毕。
			{
				synchronized ( sshLog )
				{
					sshLog.logToConsole(
							"Child thread has not finished. Wait...",
							LogCollector.INFO );
				}
				Thread.sleep( WAIT );
			}
			synchronized ( result )
			{
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
			return result;
		}
	}

	private class ExecuteThread implements Runnable
	{
		private SshClient sshClient = null;
		private ConnectFuture sshConnection = null;
		private ClientSession sshSession = null;
		private ClientChannel sshChannel = null;
		private ChannelShell sshShell = null;

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
					throw new IOException(
							"Session has been closed with unknown reason!" );
				}
				synchronized ( sshLog )
				{
					sshLog.logToConsole(
							"Execution thread initial finished successfully!",
							LogCollector.INFO );
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
				sshSession.close( false );
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
					String cmd = null;
					ByteArrayInputStream inputCommand = null;
					synchronized ( commandBuff )
					{
						// 同步获取当前缓冲区中的命令，然后清空缓冲区。
						// 如果缓冲区为null或者没有内容，则等待WATI指定时间后继续下一次循环。
						if ( commandBuff != null && commandBuff.length() > 0 )
						{
							synchronized ( sshLog )
							{
								sshLog.logToConsole(
										"Get command successfully!",
										LogCollector.INFO );
							}
							cmd = commandBuff.toString();
							commandBuff.delete( 0, commandBuff.length() );
						}
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
										"Data of last command has not been finished. Wait...",
										LogCollector.INFO );
							}
							Thread.sleep( WAIT );
						}
						// 设定标志位，表示命令正在执行中。
						cmdFinishFlag = FLAG_OFF;
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
						inputCommand = new ByteArrayInputStream( cmd.getBytes() );

						sshShell.setIn( inputCommand );
						sshShell.setOut( new NoCloseOutputStream( output ) );
						sshShell.open().await();
						sshShell.waitFor( ClientChannel.CLOSED, 0 );
						// 设定标志位，表示命令执行完毕。
						cmdFinishFlag = FLAG_ON;
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
					while ( (length = input.available()) == 0 )
					// 当管道中数据为空时。
					{

						if ( cmdFinishFlag == FLAG_ON )
						// 如果命令已执行完毕，表明没有后续数据，数据收集已完成。
						// 将缓存中的数据添加到命令返回值列表中。
						{
							synchronized ( result )
							{
								if ( buffer.length() != 0 )
								{
									result.add( buffer.toString() );
								}
							}
							buffer.delete( 0, buffer.length() );
							dataFinishFlag = FLAG_ON;
							break;
						}
						else
						// 如果命令未执行完毕，则等待后续的命令执行结果发送至管道。
						{
							synchronized ( sshLog )
							{
								sshLog.logToConsole( "Pipe is empty. Wait...",
										LogCollector.INFO );
							}
							Thread.sleep( WAIT );
						}
					}
					ret = new byte[ length ];
					input.read( ret );
					if ( ret != null )
					{
						buffer.append( ret );
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
			catch ( InterruptedException e )
			{
				// TODO Auto-generated catch block
				synchronized ( sshLog )
				{
					sshLog.logToConsole(
							"Data collection thread terminated because of interruption.",
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
