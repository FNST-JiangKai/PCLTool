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
	
	static String TRUE = "true";
	static String FALSE = "false";
	
	/*
	 * 支持的命令集
	 */
	
	static String IF = "if";
	static String ELSE = "else";
	
	
	CommandTree cmdTree = new CommandTree();
	
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
	
	private class MakeCommandTree
	{
		public MakeCommandTree(String command)
		{
			switch(command)
			{
			case "":
				break;
			}
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
		Stack <CommandNode> JudgeNodeStack = null;
		
		public CommandTree()
		{
			Root = null;
			JudgeNodeStack = null;
		}
		
		public void appendNode(CommandNode a)
		{
			CommandNode pNode = null;		//当前插入节点指针
			if(Root == null)
			{
				Root = a;
				pNode = a;
			}
			else
			{
				pNode = Root;
				for (;pNode.Next.get(TRUE) != null;pNode = pNode.Next.get(TRUE))
					;
				pNode.Next.put(TRUE,a);
				pNode = pNode.Next.get(TRUE);
			}
			if(a.NodeStyle == JUDGENODE)
			{
				pNode.Next.put(TRUE,null);
				pNode.Next.put(FALSE,null);
				JudgeNodeStack.push(pNode);		//如果是判断节点，则把当前节点压栈
			}
			else
			{
				pNode.Next.put(TRUE, null);
			}
		}
	}
}
