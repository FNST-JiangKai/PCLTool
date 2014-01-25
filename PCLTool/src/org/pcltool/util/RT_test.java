package org.pcltool.util;

import java.io.*;

import org.pcltool.log.LogCollector;

import java.util.*;

/*
 *      1. 写全注释 -->完成
 * 		2. 完善异常抛出 -->完成
 * 		3. ssh调试
 * 			a. 修改命令执行方式 -->完成
 * 			b. 修改switch设置方式-->完成
 * 		4. 修改log输出--完成
 * */

public class RT_test
{
	private static final byte COMMANDNODE = 0;
	private static final byte IFNODE = 1;
	private static final byte WHILENODE = 2;

	/*
	 * 支持的命令集
	 */

	private LogCollector RTTestLog = LogCollector
			.createLogCollector( "rttestlog" );
	// 全局命令树
	private Queue< CommandNode > CMDTree = null;

	private Boolean isTestReady = false;
	private MakeCommandTree cmdt = null;
	private Node hostNode = null;
	private File sshLog = null;
	private File TestCaseFile = null; // 测试脚本
	private FileOutputStream fos = null;
	private String logDir = "D:/"; // 测试使用

	/**
	 * 根据RTTestcaseFilePath路径指示文件，构造命令树 如果没有异常，则设置isTestReady为True
	 * 否则设置isTestReady为False log文件初始化。
	 * 
	 * 待测试脚本路径。
	 * 
	 * @param RTTestCaseFilePath
	 */

	public RT_test( String RTTestCaseFilePath )
	{
		CMDTree = new LinkedList< CommandNode >();
		TestCaseFile = new File( RTTestCaseFilePath );
		if ( !TestCaseFile.isFile() )
		{
			RTTestLog.logToConsole( "RTTestCaseFile not exist.",
					LogCollector.ERROR );
			isTestReady = false;
			return;
		}
		try
		{
			cmdt = new MakeCommandTree();
			isTestReady = true;
		}
		catch ( CreateCMDTreeErrorException e )
		{
			// TODO Auto-generated catch block
			isTestReady = false;
			e.printStackTrace();
		}
		String TestCaseFileName = TestCaseFile.getName();
		String TestCaseLogFileName = TestCaseFileName.split( "\\." )[ 0 ]
				+ ".log";
		sshLog = new File( logDir + "/" + TestCaseLogFileName );
	}

	/**
	 * 开始执行测试，遍历命令树，并且执行命令树节点命令
	 * 
	 */
	public void startTest()
	{
		ExecuteCommandTree();
		if ( this.hostNode != null )
			hostNode.SSHUnInit();
	}

	/**
	 * 遍历执行CMDTree，并且生成log文件。
	 */
	private void ExecuteCommandTree()
	{
		CommandNode node = CMDTree.poll();
		while ( node != null )
		{
			node.execute();
			node = CMDTree.poll();
		}
	}

	/**
	 * 将logStr追加到sshlog中。
	 * 
	 * @param logStr
	 */
	private void sshLogToFile( String logStr )
	{
		if ( !sshLog.exists() )
		{
			// 创建文件
			try
			{
				if ( !sshLog.createNewFile() )
				{
					RTTestLog.logToConsole( "log文件创建失败", LogCollector.ERROR );
					isTestReady = false;
					return;
				}
				RTTestLog.logToConsole( "log文件创建成功。\n", LogCollector.INFO );
			}
			catch ( IOException e )
			{
				RTTestLog.logToConsole( "文件IO异常。\n", LogCollector.ERROR );
				isTestReady = false;
				e.printStackTrace();
			}
		}
		try
		{
			if ( fos == null )
				fos = new FileOutputStream( sshLog );
			else
				fos = new FileOutputStream( sshLog, true );
			RTTestLog
					.logToConsole( "log写入：" + logStr + "\n", LogCollector.INFO );
			byte[] stb = logStr.getBytes();
			fos.write( stb );
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
		finally
		{
			try
			{
				fos.close();
			}
			catch ( IOException e )
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// 将logStr追加到sshLog中
	}

	/**
	 * 节点类，存储节点相关信息：ip，username，password，ssh
	 * 
	 * @author gaot.fnst
	 * 
	 */

	private class Node
	{
		public String hostIP = null;
		public String userName = null;
		public String passWord = null;
		private int sshPort = 22;
		private SshConsole ssh = null;

		public Node( String hostIP, String userName, String passWord )
		{
			this.hostIP = hostIP;
			this.userName = userName;
			this.passWord = passWord;
			ssh = new SshConsole();
			// ssh.init( this.hostIP, sshPort , this.userName, this.passWord );
		}

		/**
		 * 获取ssh输出。不写log。 ssh命令
		 * 
		 * @param command
		 * 命令输出
		 * @return
		 */
		public String getSSHOutput( String command )
		{
			return ssh.executeCommand( command );
		}

		/**
		 * 节点ssh连接初始化。
		 */
		public void SSHInit()
		{
			if ( ssh != null )
			{
				ssh.init( this.hostIP, sshPort, this.userName, this.passWord );
				ssh.executeCommand( "\n" ); // 执行空命令，消除首次登陆字符串对判断条件的影响。
			}
			else
			{
				RTTestLog.logToConsole( "节点没有初始化。", LogCollector.ERROR );
				return;
			}
		}

		/**
		 * 关闭ssh连接。
		 */
		public void SSHUnInit()
		{
			if ( ssh != null )
			{
				ssh.uninit();
			}
			else
			{
				RTTestLog.logToConsole( "节点没有初始化。", LogCollector.ERROR );
				return;
			}
		}

		/**
		 * 执行命令，并且将ssh输出写入log文件中。
		 * 
		 * @param Command
		 */
		public void executeCommand( String Command )
		{
			if ( Command.startsWith( "#@" ) ) // java命令
			{
				System.out.print( Command );
			}
			else
			{
				// shell命令
				// 缺少输出处理
				String sshOutput = ssh.executeCommand( Command );
				sshLogToFile( sshOutput );
			}
		}
	}

	/**
	 * 命令节点，语句块的抽象，普通节点只包含待执行命令数据 判断节点包含判断条件、TrueCmdTree命令树和FalseCmdTree命令树
	 * 
	 * @author gaot.fnst
	 * 
	 */
	private class CommandNode
	{
		/*
		 * 1.每行命令作为一个节点 2.每个语句块作为一个节点 3.节点中可以嵌套节点
		 */

		public byte NodeStyle = COMMANDNODE;// 默认节点类型为command

		private String NodeData = null;// 命令数据
		// 当判断条件为真时，要执行的命令树
		public Queue< CommandNode > TrueCmdTree = null;
		public Queue< CommandNode > FalseCmdTree = null;
		// 构建命令树时用来区分条件真语句块和条件假语句块
		// 执行命令树使用来标识条件的真假
		public Boolean Switch = true; // 默认只有TrueCmdTree

		// 命令类型
		private final int SSH_INIT_ERROR = 0;
		private final int SSH_INIT = 1;
		private final int LOCAL_COMMAND = 2;
		private final int JAVA_COMMAND = 3;
		private final int SSH_COMMAND = 4;

		public CommandNode( byte NodeStyle, String NodeData )
		{
			this.NodeStyle = NodeStyle;
			this.NodeData = NodeData;
			TrueCmdTree = new LinkedList< CommandNode >();
			FalseCmdTree = new LinkedList< CommandNode >();
			Switch = true;
		}

		/**
		 * 判断命令类型
		 * 
		 * @param Command
		 * 
		 * SSH_INIT_ERROR ->命令格式错误 SSH_INIT ->ssh初始化 LOCAL_COMMAND ->本地命令
		 * JAVA_COMMAND ->java命令 SSH_COMMAND ->ssh命令
		 * @return
		 */
		private int getCommandStyle( String Command )
		{
			if ( Command.startsWith( "@ssh" ) )
			{
				String hostInfoList[] = Command.split( ":" );
				if ( hostInfoList.length == 4 )
					return SSH_INIT;
				else
					return SSH_INIT_ERROR;
			}
			if ( Command.startsWith( "@local" ) )
			{
				return LOCAL_COMMAND;
			}
			if ( Command.startsWith( "@java" ) )
			{
				return JAVA_COMMAND;
			}
			return SSH_COMMAND;
		}

		/**
		 * 执行当前节点，如果节点是判断节点，则递归执行节点中所有命令树 
		 * 1. 如果节点是普通节点，则执行命令 
		 * 		a.ssh命令调用hostNode.executeCommand() 
		 * 		b. cmd命令调用localNode.executeCommand()
		 * 		c. java命令调用javaNode.executeCommand() 
		 * 2.如果节点是IF节点，根据判断条件选择递归执行TrueCMDTree或者FalseCMDTree 
		 * 		a. 如果判断条件是ssh命令，添加 &>/dev/null;echo $?发送，获取结果 
		 * 		b. 如果判断条件是java命令，调用java程序设置switch 
		 * 3.如果节点是WHILE节点，根据判断条件选择递归执行TrueCMDTree或者完成WHILE节点
		 */
		public void execute()
		{
			// System.out.print( SimpleUtil.generateTimestamp(
			// SimpleUtil.TIMESTAMP_ALL )+"\n");
			if ( !isTestReady ) // 如果测试没有准备好，不进行任何操作
				return;
			switch ( NodeStyle )
			{
			case COMMANDNODE:
			{
				switch ( this.getCommandStyle( this.NodeData ) )
				{
				case SSH_INIT: // ssh连接初始化
				{
					String nodeInfoList[] = this.NodeData.split( ":" );
					// 获取命令类型过程中已经检查过语法合法性了
					String hostIp = nodeInfoList[ 1 ];
					String userName = nodeInfoList[ 2 ];
					String passWord = nodeInfoList[ 3 ];
					if ( hostNode != null )
						hostNode.SSHUnInit();
					hostNode = new Node( hostIp, userName, passWord );
					hostNode.SSHInit();
					break;
				}
				case LOCAL_COMMAND: // 执行本地命令
				{
					System.out.print( "@local\n" );
					System.out.print( this.NodeData );
					break;
				}
				case JAVA_COMMAND: // 执行java命令（java函数）
				{
					System.out.print( "@java\n" );
					System.out.print( this.NodeData );
					break;
				}
				case SSH_COMMAND: // 执行普通ssh命令
				{
					if ( hostNode == null )
					{
						RTTestLog.logToConsole( "ssh connect not initial.\n",
								LogCollector.ERROR );
						return;
					}
					hostNode.executeCommand( this.NodeData ); // ssh命令
					break;
				}
				case SSH_INIT_ERROR: // ssh初始化语法错误
				{
					RTTestLog.logToConsole( "ssh语法错误：" + this.NodeData,
							LogCollector.ERROR );
					isTestReady = false;
					return;
				}
				default: // 未支持的语法
				{
					RTTestLog.logToConsole( "未支持的语法类型。" + this.NodeData,
							LogCollector.ERROR );
					isTestReady = false;
					return;
				}
				}
				break;
			}
			case IFNODE:
			{
				if ( !setSwitch() )// 设置Switch的值，仅仅是设置Switch的值
				{
					RTTestLog.logToConsole( "Set Switch value failed. Command:"
							+ NodeData, LogCollector.ERROR );
					return;
				}
				if ( Switch )
				{
					// 遍历TrueCmdTree， 依次调用node.execute()
					CommandNode node = this.TrueCmdTree.poll();
					while ( node != null )
					{
						node.execute();
						node = this.TrueCmdTree.poll();
					}
				}
				else
				{
					// 遍历FalseCmdTree， 依次调用node.execute()
					CommandNode node = this.FalseCmdTree.poll();
					while ( node != null )
					{
						node.execute();
						node = this.FalseCmdTree.poll();
					}
				}
				break;
			}
			case WHILENODE:
			{
				if ( !setSwitch() )// 设置Switch的值，仅仅是设置Switch的值
				{
					RTTestLog.logToConsole( "Set Switch value failed. Command:"
							+ NodeData, LogCollector.ERROR );
					return;
				}
				while ( Switch )
				{
					Queue< CommandNode > WhileTree = new LinkedList< CommandNode >(
							this.TrueCmdTree );
					CommandNode node = WhileTree.poll();
					while ( node != null )
					{
						node.execute();
						node = WhileTree.poll();
					}
					if ( !setSwitch() )// 设置Switch的值，仅仅是设置Switch的值
					{
						RTTestLog.logToConsole(
								"Set Switch value failed. Command:" + NodeData,
								LogCollector.ERROR );
						return;
					}
				}
				break;
			}
			default:
				RTTestLog.logToConsole( "Unsupported NodeType",
						LogCollector.ERROR );
				return;
			}
		}

		/**
		 * 根据条件设置类中Switch的值，用来判别需要执行的语句块 设置成功返回true，设置失败返回false
		 * 
		 * @return
		 */
		private Boolean setSwitch()
		{
			/**
			 * 如果是判断节点，则根据条件设置Switch的值
			 */

			// 测试版本
			if ( this.NodeStyle == IFNODE || this.NodeStyle == WHILENODE )
			{
				if ( this.getCommandStyle( this.NodeData ) == this.SSH_COMMAND )
				{
					String conditionCommand = this.NodeData
							+ " &>/dev/null;echo $?";
					String sshOutput = null;
					if ( hostNode != null )
					{
						sshOutput = hostNode.getSSHOutput( conditionCommand );
					}
					else
					{
						isTestReady = false;
						RTTestLog.logToConsole( "host ssh channel not open.\n",
								LogCollector.ERROR );
						return false;
					}
					String outputList[] = sshOutput.split( "\n" );
					System.out.print( sshOutput );
					if ( outputList.length != 2 )
					{
						RTTestLog.logToConsole( "set Switch failed.",
								LogCollector.ERROR );
						isTestReady = false;
						return false;
					}
					else
					{
						if ( outputList[ 1 ].startsWith( "0" ) ) // 判断返回值是不是0
						{
							this.Switch = true;
							return true;
						}
						else
						{
							this.Switch = false;
							return true;
						}
					}
				}
				if ( this.getCommandStyle( this.NodeData ) == this.JAVA_COMMAND )
				{
					// TODO:
					return true;
				}

				return false;// 不支持其他命令格式
			}
			else
			{
				RTTestLog.logToConsole(
						"Only IFNODE and WHILENODE has this interface.",
						LogCollector.ERROR );
				return false;
			}
		}
	}

	/**
	 * 创建命令树类，将脚本中内容按行做成命令节点， 填充进CMDTree中，只进行命令树的制作，相当于编译过程。
	 * 
	 * @author gaot.fnst
	 * 
	 */
	private class MakeCommandTree
	{

		private Stack< CommandNode > IfStack = null;
		private Stack< CommandNode > WhileStack = null;

		final Byte SWITCHIFSTACK = 1;
		final Byte SWITCHWHILESTACK = 2;

		private Stack< Byte > SwitchStack = null;

		private HashMap< String, Integer > CMDLIST = null;

		public MakeCommandTree() throws CreateCMDTreeErrorException
		{
			CMDLIST = new HashMap< String, Integer >();
			IfStack = new Stack< CommandNode >();
			WhileStack = new Stack< CommandNode >();
			SwitchStack = new Stack< Byte >();

			CMDLIST.put( "if", 1 );
			CMDLIST.put( "else", 2 );
			CMDLIST.put( "endif", 3 );
			CMDLIST.put( "while", 4 );
			CMDLIST.put( "endwhile", 5 );

			try
			{
				FileInputStream fis = new FileInputStream( TestCaseFile );
				InputStreamReader isr = new InputStreamReader( fis, "UTF-8" );
				BufferedReader br = new BufferedReader( isr );
				String strCommand = "";
				while ( (strCommand = br.readLine()) != null )
				{
					strCommand = strCommand.split( "\n" )[ 0 ];
					AddCommandToCMDTree( strCommand );
				}
				fis.close();
			}
			catch ( FileNotFoundException e )
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
				CreateCMDTreeErrorException exception = new CreateCMDTreeErrorException(
						"文件不存在。\n" );
				throw exception;
			}
			catch ( UnsupportedEncodingException e )
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
				CreateCMDTreeErrorException exception = new CreateCMDTreeErrorException(
						"不支持的编码格式\n" );
				throw exception;
			}
			catch ( IOException e )
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
				CreateCMDTreeErrorException exception = new CreateCMDTreeErrorException(
						"IO 错误。\n" );
				throw exception;
			}
			if ( !this.IfStack.empty() )
			{
				RTTestLog
						.logToConsole( "If 语法错误，缺少endif\n", LogCollector.ERROR );
				CreateCMDTreeErrorException exception = new CreateCMDTreeErrorException(
						"If 语法错误，缺少endif\n" );
				throw exception;
			}
			if ( !this.WhileStack.empty() )
			{
				RTTestLog.logToConsole( "While 语法错误，缺少endwhile\n",
						LogCollector.ERROR );
				CreateCMDTreeErrorException exception = new CreateCMDTreeErrorException(
						"While 语法错误，缺少endwhile\n" );
				throw exception;
			}
		}

		/**
		 * 将字符串做成节点添加到命令树中 字符串命令
		 * 
		 * @param Command
		 * 制作命令树时，发生错误，抛出制作命令树错误异常。
		 * @throws CreateCMDTreeErrorException
		 */
		private void AddCommandToCMDTree( String Command )
				throws CreateCMDTreeErrorException
		{
			// Command = Command.split( "\n" )[ 0 ]; // 删除结尾换行符
			Command = Command.trim();// 删除命令前后空格
			String CommandList[] = Command.split( " " ); // 将命令按空格区分
			if ( CommandList.length == 0 ) // 如果命令只有一个空格
			{
				return;
			}
			if ( CMDLIST.containsKey( CommandList[ 0 ] ) )
			{
				switch ( CMDLIST.get( CommandList[ 0 ] ) )
				{
				case 1: // if
				{
					// 检查if语句是否合法
					String Condition = Command.replaceFirst( "if ", "" );
					CommandNode node = new CommandNode( IFNODE, Condition );
					node.Switch = true; // if 语句块初始化
					AddNodeToQueue( node );
					break;
				}
				case 2: // else
				{
					if ( CommandList.length != 1 )
					{
						RTTestLog.logToConsole( "Synax Error: " + Command,
								LogCollector.ERROR );
						CreateCMDTreeErrorException exception = new CreateCMDTreeErrorException(
								"语法错误：" + Command );
						throw exception;
					}
					else
					{
						CommandNode LastIfNode = IfStack.peek();
						LastIfNode.Switch = false; // 将if语句块中开关指向False
					}
					break;
				}
				case 3: // endif
				{
					if ( CommandList.length != 1 )
					{
						RTTestLog.logToConsole( "Synax Error: " + Command,
								LogCollector.ERROR );
						CreateCMDTreeErrorException exception = new CreateCMDTreeErrorException(
								"语法错误:" + Command );
						throw exception;
					}
					else
					{
						if ( IfStack.empty() )
						{
							RTTestLog.logToConsole( "IfStack Error",
									LogCollector.ERROR );
							CreateCMDTreeErrorException exception = new CreateCMDTreeErrorException(
									"IF栈错误，多余endif" );
							throw exception;
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
								CreateCMDTreeErrorException exception = new CreateCMDTreeErrorException(
										"switchstack错误。\n" );
								throw exception;
							}
						}
					}
					break;
				}
				case 4: // while
				{
					String Condition = Command.replaceFirst( "while ", "" );
					CommandNode node = new CommandNode( WHILENODE, Condition );
					node.Switch = true;
					AddNodeToQueue( node );
					break;
				}
				case 5: // endwhile
				{
					if ( CommandList.length != 1 )
					{
						RTTestLog.logToConsole( "Synax Error: " + Command,
								LogCollector.ERROR );
						CreateCMDTreeErrorException exception = new CreateCMDTreeErrorException(
								"endwhile语法错误：" + Command );
						throw exception;
					}
					else
					{
						if ( WhileStack.empty() )
						{
							RTTestLog.logToConsole( "WhileStack Error",
									LogCollector.ERROR );
							CreateCMDTreeErrorException exception = new CreateCMDTreeErrorException(
									"While栈错误，endwhile多余" );
							throw exception;
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
								CreateCMDTreeErrorException exception = new CreateCMDTreeErrorException(
										"switchstack栈错误。\n" );
								throw exception;
							}
						}
					}
					break;
				}
				default:
					RTTestLog.logToConsole( "Unknown Error.AddCommandToTree.",
							LogCollector.ERROR );
					CreateCMDTreeErrorException exception = new CreateCMDTreeErrorException(
							"未知错误，不支持的命令类型。" );
					throw exception;
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
		 * 
		 * @param q
		 * 队列
		 * @param node
		 * 待插入节点
		 * @throws CreateCMDTreeErrorException
		 * 抛出创建命令树失败异常
		 */

		private void __AddNodeToQueue__( Queue< CommandNode > q,
				CommandNode node ) throws CreateCMDTreeErrorException
		{
			try
			{
				if ( !q.offer( node ) )
				{
					RTTestLog.logToConsole( "Add node to queue Failed.",
							LogCollector.ERROR );
					CreateCMDTreeErrorException exception = new CreateCMDTreeErrorException(
							"节点插入失败。\n" );
					throw exception;
				}
				else
				{
					// 成功将节点插入队列中判断当前节点类型
					if ( node.NodeStyle == IFNODE )
					{
						// node.Switch = true;
						IfStack.push( node );
						SwitchStack.push( SWITCHIFSTACK );
					}
					if ( node.NodeStyle == WHILENODE )
					{
						// node.Switch = true;
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
				CreateCMDTreeErrorException exception = new CreateCMDTreeErrorException(
						"强制类型转换错误。\n" );
				throw exception;
			}
			catch ( NullPointerException e )
			{
				e.printStackTrace();
				RTTestLog.logToConsole( "Unsupported null node.",
						LogCollector.ERROR );
				CreateCMDTreeErrorException exception = new CreateCMDTreeErrorException(
						"空指针错误。\n" );
				throw exception;
			}
			catch ( IllegalArgumentException e )
			{
				e.printStackTrace();
				RTTestLog.logToConsole( "Unsupported argument.",
						LogCollector.ERROR );
				CreateCMDTreeErrorException exception = new CreateCMDTreeErrorException(
						"参数违法\n" );
				throw exception;
			}

		}

		/**
		 * 将节点加入命令树中 节点
		 * 
		 * @param node
		 * 添加过程中发生错误，抛出异常。
		 * @throws CreateCMDTreeErrorException
		 */
		private void AddNodeToQueue( CommandNode node )
				throws CreateCMDTreeErrorException
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
					CreateCMDTreeErrorException exception = new CreateCMDTreeErrorException(
							"栈错误，缺少endif或者endwhile.\n" );
					throw exception;
				}
			}
			else
			{
				if ( SwitchStack.peek() == SWITCHIFSTACK
						&& IfStack.empty() != true ) // 当前栈为IF栈，并且栈非空
				{
					CommandNode LastIfNode = IfStack.peek();
					if ( LastIfNode.Switch == true ) // if条件成立语句块
					{
						__AddNodeToQueue__( LastIfNode.TrueCmdTree, node );
					}
					else
					// else后if条件语句块
					{
						__AddNodeToQueue__( LastIfNode.FalseCmdTree, node );
					}
				}
				else
				{
					if ( SwitchStack.peek() == SWITCHWHILESTACK
							&& WhileStack.empty() != true ) // 当前栈为WHILE栈，并且栈非空
					{
						CommandNode LastWhileNode = WhileStack.pop();
						__AddNodeToQueue__( LastWhileNode.TrueCmdTree, node );
						WhileStack.push( LastWhileNode );
					}
					else
					{
						RTTestLog
								.logToConsole(
										"ERROR in Making CommandTree. Wrong StackSwitch.",
										LogCollector.ERROR );
						CreateCMDTreeErrorException exception = new CreateCMDTreeErrorException(
								"while栈错误，缺少endwhile\n" );
						throw exception;
					}
				}
			}
		}
	}
}
