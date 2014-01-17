package org.pcltool.util;

import java.io.*;

import org.apache.sshd.*;
import org.apache.sshd.client.*;
import org.apache.sshd.client.ScpClient.Option;
import org.apache.sshd.client.future.*;

import org.pcltool.log.*;

/**
 * 用来实现SCP传输文件的类.
 * <p>
 * 通过调用Apache提供的SSHD库,实现SCP传输文件.
 * 
 * @author jiangkai
 * @version 1.0
 */
public class ScpConsole
{
	private String remoteIP = null;
	private int remotePort = 0;
	private String username = null;
	private String password = null;

	private SshClient sshClient = null;
	private ConnectFuture sshConnection = null;
	private ClientSession sshSession = null;
	private ScpClient scpSession = null;

	private LogCollector scpLog = LogCollector.createLogCollector( "scpLog" );

	/**
	 * 默认的构造函数.
	 */
	public ScpConsole()
	{

	}

	/**
	 * 有参数的构造函数.
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
	public ScpConsole( String remoteIP, int remotePort, String username,
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
	 * 此方法通常与带参数的构造函数配合使用.
	 * <p>
	 * 初始化时,会对参数进行校验,校验失败则抛出异常.校验成功则会启动SCP连接等待文件传输.
	 */
	public void init()
	{
		try
		{
			// 参数校验.
			if ( remoteIP == null || username == null || password == null )
			{
				scpLog.logToConsole( "Initialize failed! Missing arguments!",
						LogCollector.ERROR );
				throw new IllegalArgumentException( "Missing arguments" );
			}
			if ( !SimpleUtil.checkIP( remoteIP ) )
			{
				scpLog.logToConsole( "Initialize failed! Invalid IP address!",
						LogCollector.ERROR );
				throw new IllegalArgumentException( "Invalid IP address!" );
			}
			if ( remotePort == 0 )
			{
				remotePort = SshConsole.DEFAULT_PORT;
			}

			// 初始化SCP连接.
			sshClient = SshClient.setUpDefaultClient();
			sshClient.start();
			sshConnection = sshClient.connect( remoteIP, remotePort ).await();
			if ( sshConnection != null )
			{
				scpLog.logToConsole( "Connect SSH successfully!",
						LogCollector.INFO );
			}
			sshSession = sshConnection.getSession();
			if ( sshSession != null )
			{
				scpLog.logToConsole( "Get SSH session successfully!",
						LogCollector.INFO );
			}
			int ret = ClientSession.WAIT_AUTH;
			while ( (ret & ClientSession.WAIT_AUTH) != 0 )
			{
				sshSession.authPassword( "root", "cscenter" );
				ret = sshSession.waitFor( ClientSession.AUTHED
						| ClientSession.CLOSED | ClientSession.TIMEOUT, 0 );
			}
			if ( (ret & ClientSession.CLOSED) != 0 )
			{
				throw new IllegalStateException(
						"Session has been closed with unknown reason!" );
			}
			scpSession = sshSession.createScpClient();
			if ( scpSession != null )
			{
				scpLog.logToConsole( "Create SCP session successfully!",
						LogCollector.INFO );
			}
			scpLog.logToConsole( "Initialize finished successfully!",
					LogCollector.OK );
		}
		catch ( IllegalArgumentException e )
		{
			// TODO Auto-generated catch block
			scpLog.logToConsole(
					"Initialize failed because of illegal argument.",
					LogCollector.ERROR );
			scpLog.logToConsole( e.getMessage(), LogCollector.ERROR );
		}
		catch ( IllegalStateException e )
		{
			// TODO Auto-generated catch block
			scpLog.logToConsole(
					"Initialize failed because session closed by unknown reason.",
					LogCollector.ERROR );
			scpLog.logToConsole( e.getMessage(), LogCollector.ERROR );
		}
		catch ( InterruptedException e )
		{
			// TODO Auto-generated catch block
			scpLog.logToConsole( "Initialize failed because of interruption.",
					LogCollector.ERROR );
			scpLog.logToConsole( e.getMessage(), LogCollector.ERROR );
		}
		catch ( IOException e )
		{
			// TODO Auto-generated catch block
			scpLog.logToConsole(
					"Initialize failed because of ssh connection failed.",
					LogCollector.ERROR );
			scpLog.logToConsole( e.getMessage(), LogCollector.ERROR );
		}
	}

	/**
	 * 初始化方法.
	 * <p>
	 * 此方法通常与无参构造函数组合使用.
	 * 
	 * @param remoteIP
	 * @param remotePort
	 * @param username
	 * @param password
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
	 * 此方法用于对SCP连接等进行清理.ScpConsole对象使用完毕后必须调用此方法进行扫尾处理.
	 */
	public void uninit()
	{
		if ( sshSession != null )
		{
			scpLog.logToConsole( "Closing SSH session...", LogCollector.INFO );
			sshSession.close( false );
			sshSession = null;
		}
		if ( sshConnection != null )
		{
			scpLog.logToConsole( "Closing SSH connection...", LogCollector.INFO );
			sshConnection.cancel();
			sshConnection = null;
		}
		if ( sshClient != null )
		{
			scpLog.logToConsole( "Closing SSH client...", LogCollector.INFO );
			sshClient.stop();
		}
	}

	/**
	 * 上传文件.
	 * <p>
	 * 通过SCP连接上传文件.
	 * 
	 * @param source
	 * 本地源文件.
	 * <p>
	 * 源文件必须存在,否则会抛出异常.
	 * <p>
	 * 源文件可以是普通文件或者目录,但当源文件是目录时,目标文件也必须是目录,否则会抛出异常.
	 * @param target
	 * 远程目标文件.
	 * <p>
	 * 目标文件可以是普通文件或者目录.目标文件是目录时,此目录必须存在.目标文件是普通文件时,全部上级目录必须都存在.
	 */
	public void upload( String source, String target )
	{
		try
		{
			String local = convertPath( source );
			String remote = convertPath( target );

			if ( isDirectory( local ) && !isDirectory( remote ) )
			{
				throw new IllegalStateException(
						"Can not send a directory to a file!" );
			}
			File f = new File( local );
			if ( !f.exists() )
			{
				throw new IllegalArgumentException( "File dose not exist!" );
			}
			Option[] options = getOptions( remote );

			scpSession.upload( local, remote, options );
			scpLog.logToConsole( source + " upload successfully!",
					LogCollector.INFO );
		}
		catch ( IllegalStateException e )
		{
			scpLog.logToConsole( "Can not send directory to a file!",
					LogCollector.ERROR );
			scpLog.logToConsole( e.getMessage(), LogCollector.ERROR );
		}
		catch ( IllegalArgumentException e )
		{
			scpLog.logToConsole( "File doesn't exist!", LogCollector.ERROR );
			scpLog.logToConsole( e.getMessage(), LogCollector.ERROR );
		}
		catch ( IOException e )
		{
			// TODO Auto-generated catch block
			scpLog.logToConsole( "Failed to upload files!", LogCollector.ERROR );
			scpLog.logToConsole( e.getMessage(), LogCollector.ERROR );
		}
	}

	/**
	 * 下载文件.
	 * <p>
	 * 通过SCP连接下载文件.
	 * 
	 * @param source
	 * 远程源文件.
	 * <p>
	 * 源文件可以是普通文件或目录.
	 * <p>
	 * 源文件必须存在,否则会抛出异常.
	 * <p>
	 * 当源文件是目录时,目标文件也必须是目录,否则会抛出异常.
	 * @param target
	 * 本地目标文件.
	 * <p>
	 * 目标文件可以是普通文件或目录.
	 * <p>
	 * 当目标文件不存在时,会自动创建目标文件及所有必须的上级目录.
	 */
	public void download( String source, String target )
	{
		try
		{
			String remote = convertPath( source );
			String local = convertPath( target );
			if ( isDirectory( local ) && !isDirectory( remote ) )
			{
				throw new IllegalStateException(
						"Can not send a directory to a file!" );
			}

			File f = new File( target );
			if ( !f.exists() )
			{
				if ( isDirectory( target ) )
				{
					f.mkdirs();
				}
				else
				{
					f.getParentFile().mkdirs();
				}
			}

			Option[] options = getOptions( local );

			scpSession.download( remote, local, options );
			scpLog.logToConsole( source + " download successfully!",
					LogCollector.INFO );
		}
		catch ( IllegalStateException e )
		{
			scpLog.logToConsole( "Can not send directory to a file!",
					LogCollector.ERROR );
			scpLog.logToConsole( e.getMessage(), LogCollector.ERROR );
		}
		catch ( IOException e )
		{
			// TODO Auto-generated catch block
			scpLog.logToConsole( "Failed to download files!",
					LogCollector.ERROR );
			scpLog.logToConsole( e.getMessage(), LogCollector.ERROR );
		}
	}

	/**
	 * 路径转化.
	 * <p>
	 * 对文件路径进行标准化处理.由于Apache的SSHD库只接受形如"/x:/dir1/dir2/file"的Windows文件路径,
	 * 因此需要将JAVA获取的Windows文件路径进行转换.
	 * 
	 * @param path
	 * 需要转换的文件路径.
	 * @return 转换完成后的标准文件路径.
	 */
	private String convertPath( String path )
	{
		File f = new File( path );
		char flag = path.charAt( path.length() - 1 );
		if ( path.charAt( 0 ) != '/' )
		{
			String fullPath = f.getAbsolutePath();
			fullPath = "/" + fullPath;
			if ( flag == '\\' || flag == '/' )
			{
				fullPath = fullPath + '/';
			}
			fullPath = fullPath.replace( "\\", "/" );
			return fullPath;
		}
		return path;
	}

	/**
	 * 判断路径是否为目录.
	 * <p>
	 * 根据输入的路径格式,判断是文件还是目录.判断规则如下
	 * <p>
	 * 1.当路径在本地存在时,直接返回File对象的isDirectory方法执行的结果.
	 * <p>
	 * 2.当路径在本地不存在时,判断路径最后一个字符,如果最后一个字符为'/'或者'\',则判断为目录,否则判断为文件.
	 * 
	 * @param path
	 * 输入的路径.
	 * @return 如果为目录则返回true,否则返回false.
	 */
	private boolean isDirectory( String path )
	{
		File f = new File( path );
		if ( f.exists() )
		{
			return f.isDirectory();
		}
		else
		{
			char flag = path.charAt( path.length() - 1 );
			if ( flag == '/' || flag == '\\' )
			{
				return true;
			}
			else
			{
				return false;
			}
		}
	}

	/**
	 * 获取SCP的选项.
	 * <p>
	 * 根据上传下载路径是否为目录,设定必要的选项.
	 * 
	 * @param path
	 * 上传或者下载的标准化路径.
	 * @return 包含有必要选项的数组.
	 */
	private Option[] getOptions( String path )
	{
		Option[] options = null;
		if ( isDirectory( path ) )
		{
			options = new Option[ 2 ];
			options[ 1 ] = Option.TargetIsDirectory;
		}
		else
		{
			options = new Option[ 1 ];
		}
		options[ 0 ] = Option.Recursive;
		return options;
	}
}
