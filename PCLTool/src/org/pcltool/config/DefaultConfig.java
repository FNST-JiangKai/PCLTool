package org.pcltool.config;

import java.util.*;

public class DefaultConfig implements ConfigInterface
{
	private String name = null;
	private HashMap< String, String > properties = new HashMap< String, String >();

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public void setName( String name )
	{
		this.name = name;
	}

	@Override
	public String getParamater( String key )
	{
		return properties.get( key );
	}

	@Override
	public void setParamater( String key, String value )
	{
		properties.put( key, value );
	}

	@Override
	public ArrayList< String > getAllParamaters()
	{
		ArrayList< String > result = new ArrayList< String >();
		Set< String > keys = properties.keySet();
		for ( String i : keys )
		{
			result.add( properties.get( i ) );
		}
		return result;
	}

	@Override
	public String toString()
	{
		StringBuilder result = new StringBuilder();

		result.append( "Name:" );
		result.append( this.name );
		result.append( "\nProperties:\n" );

		Set< String > keys = properties.keySet();
		for ( String i : keys )
		{
			result.append( i );
			result.append( " = " );
			result.append( properties.get( i ) );
			result.append( "\n" );
		}

		return result.toString();
	}
}
