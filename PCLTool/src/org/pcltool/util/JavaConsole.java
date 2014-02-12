package org.pcltool.util;

import java.io.*;
import java.util.*;
import org.pcltool.util.ScpConsole;

public class JavaConsole
{
	private HashMap< String, Integer > commandList = null;
	private final int SLEEP = 1;
	private final int UPLOAD = 2;
	private final int DOWNLOAD = 3;
	private final int REMOTEPORT = 4;
	private String hostIP = null;
	private String userName = null;
	private String passWord = null;
	private int remotePort = 22;

	public JavaConsole(String hostIP,int remotePort,String userName,String passWord)
	{
		commandList = new HashMap< String, Integer >();
		this.hostIP = hostIP;
		this.userName = userName;
		this.passWord = passWord;
		this.remotePort = remotePort;
		commandList.put( "sleep", SLEEP );
		commandList.put( "upload", UPLOAD );
		commandList.put( "download", DOWNLOAD );
	}

	//command -> @java:sleep:1
	//command -> @java:upload:src:dest
	//command -> @java:download:src:dest
	public void executeCommand( String command )
	{
		String cmdlst[] = command.split( ":" );
		if ( cmdlst.length < 2 )
		{
			//不符合命令格式
			return;
		}
		if ( !commandList.containsKey( cmdlst[ 1 ] ) )
		{
			//不支持的命令
			return;
		}
		switch ( commandList.get( cmdlst[ 1 ] ) )
		{
		case SLEEP:
		{
			if(cmdlst.length!= 3)
			{
				//命令格式错误
				return;
			}
			String strSleepTime = cmdlst[2];
			long intSleepTime = Integer.valueOf( strSleepTime ).intValue()*1000;
			try
			{
				Thread.sleep( intSleepTime );
			}
			catch ( InterruptedException e )
			{
				// TODO Auto-generated catch block
				//等待过程中被其他线程中断
				e.printStackTrace();
			}
			break;
		}
		case UPLOAD:
		{
			if(cmdlst.length != 4)
			{
				//命令格式错误
				return;
			}
			String src = cmdlst[2];
			String dest = cmdlst[3];
			if (this.hostIP == null )
			{
				//ip错误。
				return;
			}
			ScpConsole scp = new ScpConsole(this.hostIP,this.remotePort,this.userName,this.passWord);
			scp.init();
			scp.upload( src, dest );
			scp.uninit();
			break;
		}
		case DOWNLOAD:
			if(cmdlst.length != 4)
			{
				//命令格式错误
				return;
			}
			String src = cmdlst[2];
			String dest = cmdlst[3];
			if (this.hostIP == null )
			{
				//ip错误。
				return;
			}
			ScpConsole scp = new ScpConsole(this.hostIP,this.remotePort,this.userName,this.passWord);
			scp.init();
			scp.download( src, dest );
			scp.uninit();
			break;
		default:
			break;
		}
	}
}
