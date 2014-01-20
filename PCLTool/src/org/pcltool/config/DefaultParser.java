package org.pcltool.config;

import java.util.*;
import java.io.*;

import org.pcltool.log.LogCollector;

public class DefaultParser implements ParserInterface
{
	public static final String DEFAULT_PCL_CONFIG = "./config/pcl.cfg";
	public static final String DEFAULT_CLUSTER_CONFIG = "./config/cluster.cfg";

	private LogCollector log = LogCollector.createLogCollector( "parserLog" );

	@Override
	public ArrayList< DefaultConfig > loadConfig( String configPath )
	{
		ArrayList< DefaultConfig > result = new ArrayList< DefaultConfig >();

		File config = null;
		FileInputStream fis = null;
		InputStreamReader isr = null;
		BufferedReader reader = null;

		String line = null;
		String name = null;
		DefaultConfig item = null;
		try
		{
			config = new File( configPath );
			fis = new FileInputStream( config );
			isr = new InputStreamReader( fis, "UTF-8" );
			reader = new BufferedReader( isr );
			while ( (line = reader.readLine()) != null )
			{
				line = line.trim().substring( 0, line.indexOf( "#" ) );
				if ( line.length() == 0 )
				{
					continue;
				}
				if ( line.charAt( 0 ) == '[' )
				{
					if ( item != null )
					{
						result.add( item );
					}
					name = line.substring( 1, line.lastIndexOf( "]" ) ).trim();
					item = new DefaultConfig();
					item.setName( name );
					continue;
				}
				String[] split = line.split( "=" );
				if ( split.length >= 2 )
				{
					if ( item != null )
					{
						item.setParamater( split[ 0 ].trim(), split[ 1 ].trim() );
					}
					else
					{
						reader.close();
						isr.close();
						fis.close();
						throw new IllegalArgumentException(
								"Invalid config file!" );
					}
				}
			}
			result.add( item );

			reader.close();
			isr.close();
			fis.close();
			return result;
		}
		catch ( IllegalArgumentException e )
		{
			log.logToConsole( "Invalid config file!", LogCollector.ERROR );
			log.logToConsole( e.getMessage(), LogCollector.ERROR );
			return result;
		}
		catch ( IOException e )
		{
			// TODO Auto-generated catch block
			log.logToConsole( "Failed to open config file!", LogCollector.ERROR );
			log.logToConsole( e.getMessage(), LogCollector.ERROR );
			return result;
		}
	}

	@Override
	public void saveConfig( ArrayList< ? extends ConfigInterface > config,
			String configPath )
	{
		File output = null;
		FileOutputStream fos = null;
		OutputStreamWriter osw = null;
		BufferedWriter writer = null;

		try
		{
			output = new File(configPath);
			fos = new FileOutputStream(output);
			osw = new OutputStreamWriter(fos, "UTF-8");
			writer = new BufferedWriter(osw);
			
			for(ConfigInterface i : config)
			{
				StringBuilder item = new StringBuilder();
				item.append( "[" );
				item.append( i.getName() );
				item.append( "]" );
				
				writer.write( item.toString() );
				writer.newLine();
				
			}
			
			writer.close();
			osw.close();
			fos.close();
		}
		catch ( IOException e )
		{
			// TODO Auto-generated catch block
			log.logToConsole( "Failed to save config file!", LogCollector.ERROR );
			log.logToConsole( e.getMessage(), LogCollector.ERROR );
		}
	}
}
