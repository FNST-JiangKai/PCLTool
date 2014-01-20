package org.pcltool.config;

import java.util.*;

public interface ConfigInterface
{
	public String getName();

	public void setName( String name );

	public String getParamater( String key );

	public void setParamater( String key, String value );

	public ArrayList< String > getAllParamaters();
}
