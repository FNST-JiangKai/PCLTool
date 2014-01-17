package org.pcltool.log;

import java.util.*;
import java.io.*;

import org.pcltool.util.SimpleUtil;

public class LogCollector
{
	public static final int INFO = 0;
	public static final int WARN = 1;
	public static final int ERROR = 2;
	public static final int OK = 3;
	public static final int ACT = 4;
	
	private static String[] head = {" INFO"," WARN","ERROR","   OK","  ACT",};

	private static HashMap< String, LogCollector > allLog = new HashMap< String, LogCollector >();
	private static ArrayList< String > logList = new ArrayList< String >();

	private String log = null;
	private GuiCanLog gui = null;

	private LogCollector()
	{

	}

	public static LogCollector createLogCollector( String name )
	{
		if ( logList.contains( name ) )
		{
			return allLog.get( name );
		}
		else
		{
			LogCollector lc = new LogCollector();
			allLog.put( name, lc );
			return lc;
		}
	}

	public void registerLogFile( String log )
	{
		this.log = log;
	}

	public void registerLogGui( GuiCanLog gui )
	{
		this.gui = gui;
	}

	public void logToFile( String content, int level )
	{
		// TODO Not implemented.
		File logFile = new File( log );
		System.out.println( "Not implemented yet!" );
	}

	public void logToGui( String content, int level )
	{
		gui.addLog( content, level );
	}

	public void logToConsole( String content, int level )
	{
		StringBuilder logShown = new StringBuilder();

		logShown.append( "[" + head[level] + "]" );
		logShown.append( SimpleUtil
				.generateTimestamp( SimpleUtil.TIMESTAMP_ALL ) );
		logShown.append( "\n" );
		logShown.append( content );

		System.out.println( logShown.toString() );
	}
}
