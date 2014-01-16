package org.pcltool.util;

import java.io.*;

import org.pcltool.log.LogCollector;

import java.util.*;

/*
 * 
 * */

public class RT_test
{
	static byte COMMANDNODE = 0;
	static byte IFNODE = 1;
	static byte WHILENODE = 2;
	static byte EMPTYNODE = 3;
	static byte ENDWHILENODE = 4;

	/*
	 * 支持的命令集
	 */

	private LogCollector RTTestLog = LogCollector
			.createLogCollector( "rttestlog" );

	public RT_test( String RTTestCaseFilePath )
	{
		;
	}

	public Queue< CommandNode > CMDTree = null;

	private class CommandNode
	{
		/*
		 * 1.每行命令作为一个节点 2.每个语句块作为一个节点 3.节点中可以嵌套节点
		 */

		public byte NodeStyle = COMMANDNODE;// 默认节点类型为command

		// public String JudgementConditions = null;
		public String NodeData = null;
		public Queue< CommandNode > TrueCmdTree = null;
		public Queue< CommandNode > FalseCmdTree = null;
		public Boolean Switch = true; // 默认只有TrueCmdTree

		public CommandNode( byte NodeStyle, String NodeData )
		{
			this.NodeStyle = NodeStyle;
			this.NodeData = NodeData;
		}
	}

	private class MakeCommandNode
	{

		private File TestCaseFile = null;

		private Stack< CommandNode > IfStack = null;
		private Stack< CommandNode > WhileStack = null;

		private Boolean StackSwitch = true; // 当前栈开关。
											// true->IfStack。false->WhileStack

		private HashMap< String, Integer > CMDLIST = null;

		public MakeCommandNode( String TestCaseFilePath )
		{
			CMDLIST = new HashMap< String, Integer >();
			IfStack = new Stack< CommandNode >();
			WhileStack = new Stack< CommandNode >();

			CMDLIST.put( "if", 1 );
			CMDLIST.put( "else", 2 );
			CMDLIST.put( "endif", 3 );
			CMDLIST.put( "while", 4 );
			CMDLIST.put( "endwhile", 5 );

		}

		public void AddCommandToCMDTree( String Command )
		{
			Command = Command.split( "\n" )[ 0 ]; // 删除结尾换行符
			String CommandList[] = Command.split( " " ); // 将命令按空格区分
			if ( CMDLIST.containsKey( CommandList[ 0 ] ) )
			{
				switch ( CMDLIST.get( CommandList[ 0 ] ) )
				{
				case 1: // if
				{
					// 检查if语句是否合法
					if ( CommandList.length != 2 )
					{
						RTTestLog.logToConsole( "Synax Error: " + Command,
								LogCollector.ERROR );
						return;
					}
					else
					{
						String Condition = CommandList[ 1 ];
						CommandNode node = new CommandNode( IFNODE, Condition );
						node.Switch = true; // if 语句块初始化
						AddNodeToQueue( node );
					}
					break;
				}
				case 2: // else
				{
					if ( CommandList.length != 1 )
					{
						RTTestLog.logToConsole( "Synax Error: " + Command,
								LogCollector.ERROR );
						return;
					}
					else
					{
						CommandNode LastIfNode = IfStack.pop();
						LastIfNode.Switch = false; // 将if语句块中开关指向False
						IfStack.push( LastIfNode );
					}
					break;
				}
				case 3: // endif
				{
					if ( CommandList.length != 1 )
					{
						RTTestLog.logToConsole( "Synax Error: " + Command,
								LogCollector.ERROR );
						return;
					}
					else
					{
						if ( IfStack.empty() )
						{
							RTTestLog.logToConsole( "IfStack Error",
									LogCollector.ERROR );
							return;
						}
						else
							IfStack.pop(); // 弹出最近一个IF节点。
					}
					break;
				}
				case 4: // while
				{
					if(CommandList.length != 2)
					{
						RTTestLog.logToConsole( "Synax Error: " + Command,
								LogCollector.ERROR );
						return;
					}
					else
					{
						String Condition = CommandList[ 1 ];
						CommandNode node = new CommandNode( WHILENODE, Condition );
						node.Switch = true;
						AddNodeToQueue( node );
					}
					break;
				}
				case 5: // endwhile
				{
					if (CommandList.length != 1)
					{
						RTTestLog.logToConsole( "Synax Error: " + Command,
								LogCollector.ERROR );
						return;
					}
					else
					{
						if ( WhileStack.empty() )
						{
							RTTestLog.logToConsole( "WhileStack Error",
									LogCollector.ERROR );
							return;
						}
						else
							WhileStack.pop(); // 弹出最近一个While节点。
					}
				}
				default:
					break;
				}
			}
			else
			{
				CommandNode node = new CommandNode( COMMANDNODE, Command );
				AddNodeToQueue( node );
			}

		}

		public void AddNodeToQueue( CommandNode node )
		{
			if ( IfStack.empty() && WhileStack.empty() )
			{
				CMDTree.add( node );
			}
			else
			{
				if ( StackSwitch = true && IfStack.empty() != true ) // 当前栈为IF栈，并且栈非空
				{
					CommandNode LastIfNode = IfStack.pop();
					if ( LastIfNode.Switch == true ) // if条件成立语句块
					{
						LastIfNode.TrueCmdTree.add( node );
					}
					else
					// else后if条件语句块
					{
						LastIfNode.FalseCmdTree.add( node );
					}
					IfStack.push( LastIfNode );
				}
				if ( StackSwitch = false && WhileStack.empty() != true ) // 当前栈为WHILE栈，并且栈非空
				{
					CommandNode LastWhileNode = WhileStack.pop();
					LastWhileNode.TrueCmdTree.add( node );
					WhileStack.push( LastWhileNode );
				}
				else
				{
					RTTestLog.logToConsole(
							"ERROR in Making CommandTree. Wrong StackSwitch.",
							LogCollector.ERROR );
					return;
				}
			}
		}
	}
}
