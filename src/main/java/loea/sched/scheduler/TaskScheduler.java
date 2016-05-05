package loea.sched.scheduler;

import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Vm;

import loea.sched.task.Subtask;
import loea.sched.task.Task;

public interface TaskScheduler {

	/**
	 * receive an incoming task
	 * 
	 * @param task
	 *            incoming task
	 */
	void submitTask(Task task);

	void addVM(Vm vm);

	void removeVM(Vm vm);

	/**
	 * Receive a notice of the completion of a subtask
	 * 
	 * @param st
	 *            a completed subtask
	 */
	void completed(Subtask st);

	List<Task> getTaskList();

	boolean isComplete();

	/**
	 * Map runnable subtasks to VMs, or migrate existing subtasks among existing
	 * VMs
	 * 
	 * @return the mapping of runnable subtasks to VMs
	 */
	Map<Subtask, Vm> schedule();
}
