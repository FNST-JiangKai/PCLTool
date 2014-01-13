package org.pcltool.util;

import java.text.*;
import java.util.*;
import java.io.*;

/**
 * 工具类,提供各种零碎的简单工具方法.
 * <p>
 * 以静态方法的形式提供各种工具方法,用户不需要创建该类的对象
 * 
 * @author jiangkai
 * @version 1.0
 */
public class SimpleUtil
{
	public static final String TIMESTAMP_ALL = "yyyy-MM-dd HH:mm:ss.SSS z";
	public static final String TIMESTAMP_YEAR_MONTH_DAY = "yyyy-MM-dd";
	public static final String TIMESTAMP_HOUR_MINUTE_SECOND = "HH:mm:ss";
	
	/**
	 * 校验IP
	 * <p>
	 * 检查指定的字符串是否表示合法的IP地址
	 * 
	 * @param ip
	 * 代表IP地址的字符串
	 * @return 如果合法返回true,不合法返回false
	 */
	public static boolean checkIP( String ip )
	{
		boolean result = true;

		if ( ip == null || ip.equals( "" ) )
		{
			result = result && false;
		}
		else
		{
			String[] ipSplit = ip.split( "\\." );
			if ( ipSplit.length != 4 )
			{
				result = result && false;
			}
			else
			{
				for ( int i = 0; i < 4; i++ )
				{
					try
					{
						int num = Integer.parseInt( ipSplit[ i ] );
						if ( num < 0 || num > 255 )
						{
							result = result && false;
							break;
						}
					}
					catch ( NumberFormatException e )
					{
						result = result && false;
					}
				}
			}
		}
		return result;
	}
	
	public static String generateTimestamp(String format)
	{
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format( cal.getTime() );
	}
}
