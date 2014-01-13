package org.pcltool.log;

public interface GuiCanLog
{
	public void addLog(String content, int level);
	public String getLog();
	public void saveLog(String logPath);
	public void clearLog();
}
