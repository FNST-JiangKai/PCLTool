package org.pcltool.util;

import java.io.*;
import java.util.*;

/*
 * 
 * */

public class RT_test
{
	static byte COMMANDNODE = 0;
	static byte JUDGENODE = 1;
	
	public RT_test( String RTTestCaseFilePath )
	{
		;
	}

	/*
	 * 将文件内容解析为树状结构，RT_test对树状结构遍历执行
	 */
	private class ParaseTestCaseFile
	{
		public ParaseTestCaseFile( String ParaseTestCaseFile )
		{
			;
		}
	}
	
	private class CommandNode
	{
		public String Command = null;
		public byte NodeStyle = COMMANDNODE;
		public HashMap <String,CommandNode> Next = null;
		
		public CommandNode(String Command, byte NodeStyle)
		{
			this.Command = Command;
			this.NodeStyle = NodeStyle;
		}
		
	}
	
	private class CommandTree
	{
		CommandNode Root = null;
		Stack <CommandNode> JudgeNodeList = null;
		
		public void appendNode(CommandNode a)
		{
			if(a.NodeStyle == COMMANDNODE)
			{
				if(Root == null)
				{
					Root = a;
				}
			}
			else
			{
				;
			}
		}
	}
}
