package phantom.task;

import java.util.List;

import net.sf.l2j.commons.concurrent.ThreadPool;

import phantom.FakePlayerTaskManager;

public class AITaskRunner implements Runnable
{	
	@Override
	public void run()
	{		
		FakePlayerTaskManager.INSTANCE.adjustTaskSize();
		List<AITask> aiTasks = FakePlayerTaskManager.INSTANCE.getAITasks();		
		aiTasks.forEach(aiTask -> ThreadPool.execute(aiTask));
	}	
}