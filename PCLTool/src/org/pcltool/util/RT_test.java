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

	static String TRUE = "true";
	static String FALSE = "false";

	/*
	 * 支持的命令集
	 */

	static String IF = "if";
	static String ELSE = "else";

	private CommandTree cmdTree = null;
	private LogCollector RTTestLog = LogCollector
			.createLogCollector( "rttestlog" );

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
		File TestCaseFile = null;


		public MakeCommandTree( String TestCaseFilePath )
		{
			TestCaseFile = new File( TestCaseFilePath );
			if ( TestCaseFile.exists() == false )
			{
				TestCaseFile = null;
				RTTestLog.logToConsole( "File not exist. File"
						+ TestCaseFilePath + ".\n", LogCollector.ERROR );
				return;
			}
		}

		public void ParaseTestCaseFile()
		{
			try
			{
				FileInputStream in = new FileInputStream( TestCaseFile );
				InputStreamReader inReader = new InputStreamReader( in );
				BufferedReader bufReader = new BufferedReader( inReader );

				String linestr = null;
				while ( (linestr = bufReader.readLine()) != null )
				{
					/*
					 * 从文件中按行读取，将读取的内容按照格式放入cmdTree中 
					 * 每个文件一棵树
					 */
					AddCommandToCommandTree(linestr);
				}

			}
			catch ( FileNotFoundException e )
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			catch ( IOException e )
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		
		
		private void AddCommandToCommandTree( String Command )
		{
			/*
			 * 按空格区分，然后进行匹配
			 */
			HashMap< String, String > CMDLST = null;
			CMDLST.put( "if", "if" );
			// CMDLST.put( "else", "else" ); 本次不支持else
			CMDLST.put( "endif", "endif" );

			CMDLST.put( "while", "while" );
			CMDLST.put( "endwhile", "endwhile" );

			Command = Command.split( "\n" )[ 0 ];
			String CmdListSplitBySpace[] = Command.split( " " );

			if ( CMDLST.containsValue( CmdListSplitBySpace[ 0 ] ) )
			{
				switch ( CmdListSplitBySpace.length )
				{
				case 2:
				{
					// 处理if,while
					CommandNode node = null;
					if ( CmdListSplitBySpace[ 0 ] == CMDLST.get( "if" ) )
					{
						node = new CommandNode( CmdListSplitBySpace[ 1 ],
								IFNODE );// 按照判断节点处理当前操作
					}
					if ( CmdListSplitBySpace[ 0 ] == CMDLST.get( "while" ) )
					{
						node = new CommandNode( CmdListSplitBySpace[ 1 ],
								WHILENODE );
					}
					if ( node == null )
					{
						RTTestLog.logToConsole( "ERROR Command: " + Command
								+ ".\n", LogCollector.ERROR );
						return;
					}
					else
					{
						cmdTree.appendNode( node );
					}
					break;
				}
				case 1:
				{
					// 处理endif，endwhile
					if ( CmdListSplitBySpace[ 0 ] == CMDLST.get( "endif" ) )
					{
						CommandNode node = new CommandNode( null, EMPTYNODE );
						cmdTree.appendNode( node );
						CommandNode LastIfNode = cmdTree.IfNodeStack.pop();
					}
					if ( CmdListSplitBySpace[ 0 ] == CMDLST.get( "endwhile" ) )
					{
						CommandNode node = new CommandNode( null, ENDWHILENODE );
						cmdTree.appendNode( node );
						CommandNode LastWhileNode = cmdTree.WhileNodeStack
								.pop();
						node.Next.put( FALSE, LastWhileNode );
						LastWhileNode.Next.put( FALSE, node );
					}
					break;
				}
				default:
				{
					RTTestLog.logToConsole(
							"ERROR Command: " + Command + ".\n",
							LogCollector.ERROR );
					return;
				}
				}
			}
			else
			{
				CommandNode node = new CommandNode( Command, COMMANDNODE );
				cmdTree.appendNode( node );
			}

			return;
		}

	}

	private class CommandNode
	{
		public String Command = null;
		public byte NodeStyle = COMMANDNODE;
		public HashMap< String, CommandNode > Next = null;

		public CommandNode( String Command, byte NodeStyle )
		{
			this.Command = Command;
			this.NodeStyle = NodeStyle;
		}

	}

	private class CommandTree
	{
		CommandNode Root = null;
		Stack< CommandNode > IfNodeStack = null;
		Stack< CommandNode > WhileNodeStack = null;

		public CommandTree()
		{
			Root = null;
			IfNodeStack = null;
			WhileNodeStack = null;
		}

		public void appendNode( CommandNode a )
		{
			CommandNode pNode = null; // 当前插入节点指针
			if ( Root == null )
			{
				Root = a;
				pNode = a;
			}
			else
			{
				pNode = Root;
				for ( ; pNode.Next.get( TRUE ) != null; pNode = pNode.Next
						.get( TRUE ) )
					;
				pNode.Next.put( TRUE, a );
				pNode = pNode.Next.get( TRUE );
			}
			if ( a.NodeStyle == IFNODE )
			{
				IfNodeStack.push( pNode ); // 如果是判断节点，则把当前节点压栈
			}
			if ( a.NodeStyle == WHILENODE )
			{
				WhileNodeStack.push( pNode );
			}
			RTTestLog.logToConsole( "Add command to commandtree. Command: "
					+ a.Command + ".\n", LogCollector.INFO );
		}
	}
}
