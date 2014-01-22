package org.pcltool.util;

import java.util.*;
import java.io.*;

import org.apache.sshd.*;
import org.apache.sshd.client.channel.*;
import org.apache.sshd.client.future.*;
import org.apache.sshd.common.util.*;

import org.pcltool.log.LogCollector;

/**
 * 用来实现SSH连接的类.
 * <p>
 * 通过调用Apache提供的SSHD库,创建SSH连接并执行命令.
 * 
 * @author jiangkai
 * @version 1.0
 */
public class SshConsole
{
	/**
	 * SSH连接的默认端口号.
	 */
	public static final int DEFAULT_PORT = 22;

	private static final int CONSOLE_SIZE = 1024;
	private static final int BUFF_SIZE = 1024;
	private static final int WAIT = 100;

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

	/**
	 * 默认构造函数.
	 */
	public SshConsole()
	{

	}

	/**
	 * 有参数的构造函数.
	 * 
	 * @param remoteIP
	 * 远程服务器IP地址.
	 * @param remotePort
	 * 远程服务器端口.
	 * @param username
	 * 用户名
	 * @param password
	 * 密码
	 */
	public SshConsole( String remoteIP, int remotePort, String username,
			String password )
	{
		this.remoteIP = remoteIP;
		this.remotePort = remotePort;
		this.username = username;
		this.password = password;
	}

	/**
	 * 初始化方法.
	 * <p>
	 * 此方法通常应该与带参数的构造函数组合使用.
	 * <p>
	 * 初始化时会对参数进行校验,如果校验失败则会抛出异常.校验成功则会启动SSH执行线程及数据记录线程.
	 */
	public void init()
	{
		try
		{
			Thread exec = null;
			Thread data = null;

			// 校验参数
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

			// 创建命令管道.
			synchronized ( sshLog )
			{
				sshLog.logToConsole( "Create pipes for command.",
						LogCollector.INFO );
			}
			readCommand = new PipedInputStream( BUFF_SIZE );
			writeCommand = new PipedOutputStream();
			readCommand.connect( writeCommand );

			// 创建数据管道
			synchronized ( sshLog )
			{
				sshLog.logToConsole( "Create pipes for data.",
						LogCollector.INFO );
			}
			readData = new PipedInputStream( BUFF_SIZE );
			writeData = new PipedOutputStream();
			readData.connect( writeData );

			// 初始化线程相关参数.
			execThreadSwitch = FLAG_ON;
			dataThreadSwitch = FLAG_ON;
			synchronized ( sshLog )
			{
				sshLog.logToConsole( "Create exec thread.", LogCollector.INFO );
				exec = new Thread( new ExecuteThread() );
				sshLog.logToConsole( "Create data thread.", LogCollector.INFO );
				data = new Thread( new GetDataThread() );
			}
			// 启动线程.
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

	/**
	 * 初始化方法.
	 * <p>
	 * 此方法通常与无参构造函数组合使用.
	 * 
	 * @param remoteIP
	 * 远程服务器IP地址
	 * @param remotePort
	 * 远程服务器端口
	 * @param username
	 * 用户名
	 * @param password
	 * 密码
	 */
	public void init( String remoteIP, int remotePort, String username,
			String password )
	{
		this.remoteIP = remoteIP;
		this.remotePort = remotePort;
		this.username = username;
		this.password = password;
		init();
	}

	/**
	 * 清理方法.
	 * <p>
	 * 此方法用于对SSH连接及相关线程进行清理.SshConsole对象使用完毕后必须调用此方法进行扫尾处理.
	 */
	public void uninit()
	{
		try
		{
			// 关闭SSH连接.
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

			// 关闭管道.
			readCommand.close();
			writeCommand.close();
			readData.close();
			writeData.close();

			// 关闭线程开关标志位.
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

	/**
	 * 通过SSH连接远程执行命令.
	 * <p>
	 * 此方法对底层进行了封装.
	 * 
	 * @param command
	 * 需要执行的命令.
	 * @return 命令的控制台输出.
	 */
	public String executeCommand( String command )
	{
		try
		{
			String temp = null;
			// 在命令末尾添加回车.
			if ( command.charAt( command.length() - 1 ) != '\n' )
			{
				command = command + "\n";
			}
			synchronized ( sshLog )
			{
				sshLog.logToConsole( "Write commands...", LogCollector.INFO );
			}
			writeCommand.write( command.getBytes() );

			// 等待命令的控制台输出.
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

	/**
	 * 通过SSH连接远程执行一组命令.
	 * <p>
	 * 此方法对{@link=org.pcltool.util.SshConsole.executeCommand(String)}
	 * 进行了包装以执行一组命令.
	 * 
	 * @param commands
	 * ArrayList形式的命令序列.
	 * @return ArrayList形式的控制台输出序列,和命令序列一一对应.
	 */
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

	/**
	 * 执行线程类.
	 * <p>
	 * 实现对SSH连接的初始化,命令的读取,执行.
	 * 
	 * @author jiangkai
	 * @version 1.0
	 */
	private class ExecuteThread implements Runnable
	{
		private SshClient sshClient = null;
		private ConnectFuture sshConnection = null;
		private ClientSession sshSession = null;
		private ClientChannel sshChannel = null;

		/**
		 * 初始化方法.
		 * <p>
		 * 对SSH连接进行初始化.
		 */
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

		/**
		 * 收尾方法.
		 * <p>
		 * 线程结束前会调用此方法进行收尾处理.
		 */
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

		/**
		 * 线程的主方法.
		 */
		@Override
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

	/**
	 * 数据收集线程类.
	 * <p>
	 * 收集SSH连接的控制台输出.
	 * 
	 * @author jiangkai
	 * @version 1.0
	 */
	private class GetDataThread implements Runnable
	{
		private StringBuilder buffer = new StringBuilder();

		/**
		 * 线程主方法.
		 */
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
						waitFlag = FLAG_OFF;
						dataFinishFlag = FLAG_OFF;
					}
					else
					{
						// 当管道中数据为空时，判断命令是否已执行结束。
						if ( isDataEnd() )
						{
							if ( waitFlag == FLAG_ON )
							// 如果已经是第2次检测到命令行提示符,则说明命令已执行完毕.
							{
								content = buffer.substring( 0,
										buffer.lastIndexOf( "[" ) );
								if ( content != null && content.length() > 0 )
								{
									synchronized ( result )
									{
										result.append( content );
									}
									buffer.delete( 0, buffer.lastIndexOf( "[" ) );
									dataFinishFlag = FLAG_ON;
								}
								Thread.sleep( WAIT );
							}
							else
							// 如果第1次检测到命令行提示符,可能是命令尚未发送,进行等待.
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

		/**
		 * 判断命令的控制台输出已经完成.
		 * <p>
		 * 根据命令行提示符判断控制台输出是否已完成.
		 * <p>
		 * 此判断基于以下事实:
		 * <p>
		 * 当一条命令执行完毕后,最后显示的一行文字的末尾必定是命令行提示符.
		 * 
		 * @return 输出是否已完成.
		 * @bug 当控制台输出恰好与命令行提示符格式一致时会导致误判.
		 */
		private boolean isDataEnd()
		{
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
