package loea.sched.scheduler;

import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Vm;

import loea.sched.task.SubTask;

public interface TaskScheduler {
	
	// map the incoming subtasks to existing VMs
	Map<SubTask, Vm> schedule(List<SubTask> stList);

	// migrate existing subtasks among existing VMs
	Map<SubTask, Vm> schedule();
}
