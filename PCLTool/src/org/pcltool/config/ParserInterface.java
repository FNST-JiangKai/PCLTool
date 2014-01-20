package org.pcltool.config;

import java.util.ArrayList;

public interface ParserInterface
{
	public ArrayList< ? extends ConfigInterface > loadConfig( String configPath );

	public void saveConfig( ArrayList< ? extends ConfigInterface > config, String configPath );
}
