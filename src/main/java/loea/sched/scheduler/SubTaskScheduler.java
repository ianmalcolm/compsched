package loea.sched.scheduler;

import java.util.Map;

import loea.sched.task.SubTask;

public interface SubTaskScheduler {
	
	Map<SubTask,Long> getRemainingWorkload();

}
