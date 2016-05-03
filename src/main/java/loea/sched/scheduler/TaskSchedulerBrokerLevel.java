package loea.sched.scheduler;

import java.util.List;
import java.util.Map;

import loea.sched.task.SubTask;

import org.cloudbus.cloudsim.Vm;

public class TaskSchedulerBrokerLevel implements TaskScheduler {

	@Override
	public Map<SubTask, Vm> schedule(List<SubTask> stList) {
		throw new UnsupportedOperationException("Unsupported operations.");
	}

	@Override
	public Map<SubTask, Vm> schedule() {
		throw new UnsupportedOperationException("Unsupported operations.");
	}

}
