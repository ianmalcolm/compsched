package loea.sched.scheduler;

import java.util.HashMap;
import java.util.Map;

import loea.sched.task.Subtask;

import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.ResCloudlet;

public class SubTaskSchedulerCloudletLevel extends CloudletSchedulerTimeShared
		implements SubtaskScheduler {

	@Override
	public Map<Subtask, Long> getRemainingWorkload() {
		Map<Subtask, Long> length = new HashMap<Subtask, Long>();
		for (ResCloudlet rcl : getCloudletExecList()) {
			length.put((Subtask) rcl.getCloudlet(),
					rcl.getRemainingCloudletLength());
		}
		return length;
	}

}
