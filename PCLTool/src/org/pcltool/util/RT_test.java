package org.pcltool.util;

import java.io.*;

import org.pcltool.log.LogCollector;

import java.util.*;

/*
 * 
 * */

public class RT_test
{
	final byte COMMANDNODE = 0;
	final byte IFNODE = 1;
	final byte WHILENODE = 2;
	final byte EMPTYNODE = 3;
	final byte ENDWHILENODE = 4;

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
		private String NodeData = null;
		public Queue< CommandNode > TrueCmdTree = null;
		public Queue< CommandNode > FalseCmdTree = null;
		public Boolean Switch = true; // 默认只有TrueCmdTree

		public CommandNode( byte NodeStyle, String NodeData )
		{
			this.NodeStyle = NodeStyle;
			this.NodeData = NodeData;
			TrueCmdTree = new LinkedList< CommandNode >();
			FalseCmdTree = new LinkedList< CommandNode >();
			Switch = true;
		}
		
		/**
		 * 执行当前节点
		 */
		public void execute()
		{
			switch(NodeStyle)
			{
			case COMMANDNODE:
			{
				;//如果是普通节点，则调用执行函数进行执行
				//以打印代替
				System.out.print( NodeData );
				break;
			}
			case IFNODE:
			{
				;//设置Switch的值，仅仅是设置Switch的值
				if(Switch)
				{
					;//遍历TrueCmdTree， 依次调用node.execute()
				}
				else
				{
					//遍历FalseCmdTree， 依次调用node.execute()
				}
				break;
			}
			case WHILENODE:
				;//设置Switch的值，仅仅是设置Switch的值
				//类提供方法设置Switch的值
				break;
			}
		}
	}

	private class MakeCommandTree
	{

		private File TestCaseFile = null;

		private Stack< CommandNode > IfStack = null;
		private Stack< CommandNode > WhileStack = null;

		final Byte SWITCHIFSTACK = 1;
		final Byte SWITCHWHILESTACK = 2;

		private Stack< Byte > SwitchStack = new Stack< Byte >();

		private HashMap< String, Integer > CMDLIST = null;

		public MakeCommandTree( String TestCaseFilePath )
		{
			CMDLIST = new HashMap< String, Integer >();
			IfStack = new Stack< CommandNode >();
			WhileStack = new Stack< CommandNode >();

			CMDLIST.put( "if", 1 );
			CMDLIST.put( "else", 2 );
			CMDLIST.put( "endif", 3 );
			CMDLIST.put( "while", 4 );
			CMDLIST.put( "endwhile", 5 );

			TestCaseFile = new File(TestCaseFilePath);
			if(!TestCaseFile.exists())		//如果文件不存在
			{
				;
			}
			else
			{
				
			}
		}

		private void AddCommandToCMDTree( String Command )
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
						{
							IfStack.pop(); // 弹出最近一个IF节点。
							if ( SwitchStack.peek() == SWITCHIFSTACK )
							{
								SwitchStack.pop();
							}
							else
							{
								RTTestLog.logToConsole( "IfStack Error",
										LogCollector.ERROR );
								return;
							}
						}
					}
					break;
				}
				case 4: // while
				{
					if ( CommandList.length != 2 )
					{
						RTTestLog.logToConsole( "Synax Error: " + Command,
								LogCollector.ERROR );
						return;
					}
					else
					{
						String Condition = CommandList[ 1 ];
						CommandNode node = new CommandNode( WHILENODE,
								Condition );
						node.Switch = true;
						AddNodeToQueue( node );
					}
					break;
				}
				case 5: // endwhile
				{
					if ( CommandList.length != 1 )
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
						{
							WhileStack.pop(); // 弹出最近一个While节点。
							if ( SwitchStack.peek() == SWITCHWHILESTACK )
							{
								SwitchStack.pop();
							}
							else
							{
								RTTestLog.logToConsole( "WhileStack Error",
										LogCollector.ERROR );
								return;
							}
						}
					}
				}
				default:
					RTTestLog.logToConsole( "Unknown Error.AddCommandToTree.",
							LogCollector.ERROR );
					break;
				}
			}
			else
			{
				CommandNode node = new CommandNode( COMMANDNODE, Command );
				AddNodeToQueue( node );
			}

		}
		
		/**
		 * 将节点node插入队列q中，并且根据节点类型设置IfStack和WhileStack
		 * @param q	队列
		 * @param node 待插入节点
		 */

		private void __AddNodeToQueue__( Queue< CommandNode > q,
				CommandNode node )
		{
			try
			{
				if ( !q.offer( node ) )
				{
					RTTestLog.logToConsole( "Add node to queue Failed.",
							LogCollector.ERROR );
				}
				else
				{
					// 成功将节点插入队列中判断当前节点类型
					if ( node.NodeStyle == IFNODE )
					{
						node.Switch = true;
						IfStack.push( node );
						SwitchStack.push( SWITCHIFSTACK );
					}
					if ( node.NodeStyle == WHILENODE )
					{
						node.Switch = true;
						WhileStack.push( node );
						SwitchStack.push( SWITCHWHILESTACK );
					}
				}
			}
			catch ( ClassCastException e )
			{
				e.printStackTrace();
				RTTestLog.logToConsole( "Unsupported node type.",
						LogCollector.ERROR );
			}
			catch ( NullPointerException e )
			{
				e.printStackTrace();
				RTTestLog.logToConsole( "Unsupported null node.",
						LogCollector.ERROR );
			}
			catch ( IllegalArgumentException e )
			{
				e.printStackTrace();
				RTTestLog.logToConsole( "Unsupported argument.",
						LogCollector.ERROR );
			}

		}

		private void AddNodeToQueue( CommandNode node )
		{
			if ( SwitchStack.empty() )
			{
				if ( IfStack.empty() && WhileStack.empty() )
				{
					__AddNodeToQueue__( CMDTree, node );
				}
				else
				{
					RTTestLog.logToConsole( "Stack Error.", LogCollector.ERROR );
				}
			}
			else
			{
				if ( SwitchStack.peek() == SWITCHIFSTACK
						&& IfStack.empty() != true ) // 当前栈为IF栈，并且栈非空
				{
					CommandNode LastIfNode = IfStack.pop();
					if ( LastIfNode.Switch == true ) // if条件成立语句块
					{
						__AddNodeToQueue__( LastIfNode.TrueCmdTree, node );
					}
					else
					// else后if条件语句块
					{
						__AddNodeToQueue__( LastIfNode.FalseCmdTree, node );
					}
					IfStack.push( LastIfNode );
				}
				if ( SwitchStack.peek() == SWITCHWHILESTACK
						&& WhileStack.empty() != true ) // 当前栈为WHILE栈，并且栈非空
				{
					CommandNode LastWhileNode = WhileStack.pop();
					__AddNodeToQueue__( LastWhileNode.TrueCmdTree, node );
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

		public void ExecuteCommandTree( Queue< CommandNode > que )
		{
			while ( que.size() > 0 )
			{
				CommandNode node = que.element(); // 取出队列头
				if ( node.NodeStyle == IFNODE ) // if节点，根据条件决定取出哪个队列进行执行
				{
					;// 根据条件设置Switch的值
						// 设置Switch值还没写。
					if ( node.Switch == true
							&& node.TrueCmdTree.isEmpty() == false )
					{
						ExecuteCommandTree( node.TrueCmdTree );
					}
					if ( node.Switch == false
							&& node.FalseCmdTree.isEmpty() == false )
					{
						ExecuteCommandTree( node.FalseCmdTree );
					}
					else
					{
						RTTestLog.logToConsole( "IFNODE Switch Error.",
								LogCollector.ERROR );
						return;
					}
					que.remove(); // if语句块执行结束后，将if语句块在当前队列中删除。
				}
				if ( node.NodeStyle == WHILENODE )
				{
					;// 根据条件设置Switch的值
						// 设置Switch值还没写。
					while ( node.Switch == true
							&& node.TrueCmdTree.isEmpty() != true )
					{
						Queue< CommandNode > WhileQueue = new LinkedList< CommandNode >(
								node.TrueCmdTree );
						ExecuteCommandTree( WhileQueue );
						;// 根据条件，设置Switch的值。
					}
				}
				if ( node.NodeStyle == COMMANDNODE )
				{
					;// 如果节点只是普通节点，取出节点内数据进行操作
						// 操作函数还没有写。
					que.remove(); // 执行结束后，将节点在队列头删除。、
				}
				else
				{
					;
				}
			}
		}

	}
}
