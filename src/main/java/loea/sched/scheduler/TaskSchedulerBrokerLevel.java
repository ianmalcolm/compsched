package loea.sched.scheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import loea.sched.task.Subtask;
import loea.sched.task.Task;

import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.Vm;

public class TaskSchedulerBrokerLevel implements TaskScheduler {

	private final List<Vm> vmList;
	private final List<Task> taskExecList;
	private final List<Task> taskPausedList;
	private final List<Task> taskFinishedList;

	public TaskSchedulerBrokerLevel() {
		vmList = new ArrayList<Vm>();
		taskExecList = new ArrayList<Task>();
		taskPausedList = new ArrayList<Task>();
		taskFinishedList = new ArrayList<Task>();
	}

	@Override
	public Map<Subtask, Vm> schedule() {
		List<Subtask> runnableCloudlets = new ArrayList<Subtask>();
		for (Task t : taskExecList) {
			List<Subtask> list = t.getRunnableSubtasks();
			runnableCloudlets.addAll(list);
		}

		Map<Subtask, Vm> map = mapSubtasks2VMs(runnableCloudlets);
		return map;
	}

	public void completed(Subtask st) {
		st.getParent().completed(st);
		if (st.getParent().isComplete()) {
			taskExecList.remove(st.getParent());
			taskFinishedList.add(st.getParent());
		}
	}

	@Override
	public boolean isComplete() {
		if (taskExecList.isEmpty() && taskPausedList.isEmpty()) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void submitTask(Task task) {
		getTaskExecList().add(task);
	}

	@Override
	public void addVM(Vm vm) {
		getVmList().add(vm);
	}

	/**
	 * map subtasks to the VMs
	 * 
	 * @param cloudlets
	 */
	protected Map<Subtask, Vm> mapSubtasks2VMs(List<Subtask> subtasks) {

		Map<Subtask, Vm> map = new HashMap<Subtask, Vm>();

		// map the binded subtasks
		{
			List<Subtask> toRemove = new ArrayList<Subtask>();
			for (Subtask st : subtasks) {
				int vmId = st.getVmId();
				if (vmId != -1) {
					Vm vm = findVMById(vmId);
					map.put(st, vm);
					toRemove.add(st);
				}
			}
			subtasks.removeAll(toRemove);
		}

		// process the unbound subtasks

		List<VMandLoad> vmLoadList = new ArrayList<VMandLoad>();

		for (Vm vm : vmList) {
			vmLoadList.add(new VMandLoad(vm));
		}

		for (Subtask st : subtasks) {
			Collections.sort(vmLoadList);
			Vm vm = vmLoadList.get(0).vm;
			map.put(st, vm);
			vmLoadList.get(0).add(st.getCloudletLength());
		}

		return map;
	}

	private Vm findVMById(int id) {
		for (Vm vm : vmList) {
			if (vm.getId() == id) {
				return vm;
			}
		}
		return null;
	}

	@Override
	public List<Task> getTaskList() {
		List<Task> tasks = new ArrayList<Task>();
		tasks.addAll(taskExecList);
		tasks.addAll(taskPausedList);
		tasks.addAll(taskFinishedList);
		return tasks;
	}

	@Override
	public void removeVM(Vm vm) {
		vmList.remove(vm);
	}

	protected List<Task> getTaskExecList() {
		return taskExecList;
	}

	protected List<Task> getTaskPausedList() {
		return taskPausedList;
	}

	protected List<Task> getTaskFinishedList() {
		return taskFinishedList;
	}

	protected List<Vm> getVmList() {
		return vmList;
	}

	class VMandLoad implements Comparable<VMandLoad> {
		Vm vm;
		long load;

		public VMandLoad(Vm _vm) {
			vm = _vm;

			CloudletScheduler cs = vm.getCloudletScheduler();
			if (cs instanceof SubtaskScheduler) {
				Map<Subtask, Long> _loads = ((SubtaskScheduler) cs)
						.getRemainingWorkload();
				for (long _load : _loads.values()) {
					load += _load;
				}

			} else {
				throw new UnsupportedOperationException();
			}
		}

		public void add(long newload) {
			load += newload;
		}

		@Override
		public int compareTo(VMandLoad arg0) {
			if (load > arg0.load) {
				return 1;
			} else if (load < arg0.load) {
				return -1;
			} else {
				return 0;
			}
		}

	}

}
