package loea.sched.scheduler;

import java.util.Map;

import loea.sched.task.Subtask;

public interface SubtaskScheduler {
	
	Map<Subtask,Long> getRemainingWorkload();

}
