package org.pcltool.config;

import java.util.*;
import java.io.*;

public class ConfigParser
{
	private static ConfigParser parser = null;
	private static ArrayList< ? extends ParserInterface > parserPool = new ArrayList< ParserInterface >();

	private ConfigParser()
	{

	}

	public static ConfigParser getInstance()
	{
		if ( parser == null )
		{
			return new ConfigParser();
		}
		return parser;
	}
}
