package org.pcltool.util;

import java.io.*;
import java.util.*;

public class JavaConsole
{
	private HashMap< String, Integer > commandList = null;
	private final int SLEEP = 1;
	private final int UPLOAD = 2;
	private final int DOWNLOAD = 3;

	public JavaConsole()
	{
		commandList = new HashMap< String, Integer >();
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
			
			break;
		}
		case DOWNLOAD:
			break;
		default:
			break;
		}
	}
}
