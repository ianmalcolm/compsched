package loea.sched.scheduler;

import java.util.HashMap;
import java.util.Map;

import loea.sched.task.SubTask;

import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.ResCloudlet;

public class SubTaskSchedulerCloudletLevel extends CloudletSchedulerTimeShared
		implements SubTaskScheduler {

	@Override
	public Map<SubTask, Long> getRemainingWorkload() {
		Map<SubTask, Long> length = new HashMap<SubTask, Long>();
		for (ResCloudlet rcl : getCloudletExecList()) {
			length.put((SubTask) rcl.getCloudlet(),
					rcl.getRemainingCloudletLength());
		}
		return length;
	}

}
