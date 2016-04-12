package loea.sched;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import loea.sched.task.Task;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.lists.VmList;

public class TaskSchedBroker extends DatacenterBroker {

	/** The task list. */
	protected List<? extends Task> taskList;

	public TaskSchedBroker(String name) throws Exception {
		super(name);
		taskList = new ArrayList<Task>();
	}

	public void submitTaskList(List<? extends Task> list) {
		getTaskList().addAll(list);
	}

	protected Comparator<Vm> loadComp = new Comparator<Vm>() {
		@Override
		public int compare(Vm o1, Vm o2) {
			int load1 = getTotalLoad(o1);
			int load2 = getTotalLoad(o2);
			if (load1 > load2) {
				return 1;
			} else if (load1 == load2) {
				return 0;
			} else {
				return -1;
			}
		}

		private int getTotalLoad(Vm o) {
			return o.getCloudletScheduler().runningCloudlets();
		}
	};

	/**
	 * Gets the task list.
	 * 
	 * @param <T>
	 *            the generic type
	 * @return the task list
	 */
	@SuppressWarnings("unchecked")
	public <T extends Task> List<T> getTaskList() {
		return (List<T>) taskList;
	}

	/**
	 * Submit cloudlets to the created VMs.
	 * 
	 * @pre $none
	 * @post $none
	 */
	protected void submitCloudlets() {
		List<Cloudlet> runnableCloudlets = new ArrayList<Cloudlet>();
		for (Task t : taskList) {
			List<Cloudlet> list = t.getRunnableSubtasks();
			runnableCloudlets.addAll(list);
		}

		for (Cloudlet cloudlet : runnableCloudlets) {
			submitCloudlet(cloudlet);
		}
	}

	/**
	 * Submit a Cloudlet to the created VMs
	 * 
	 * @param cloudlet
	 */
	protected void submitCloudlet(Cloudlet cloudlet) {

		Vm vm;
		// if user didn't bind this cloudlet and it has not been executed
		// yet
		if (cloudlet.getVmId() == -1) {
			List<Vm> vmLoadList = new ArrayList<Vm>();
			vmLoadList.addAll(getVmsCreatedList());
			Collections.sort(vmLoadList, Collections.reverseOrder(loadComp));
			vm = vmLoadList.get(0);
		} else { // submit to the specific vm
			vm = VmList.getById(getVmsCreatedList(), cloudlet.getVmId());
			if (vm == null) { // vm was not created
				Log.printLine(CloudSim.clock() + ": " + getName()
						+ ": Postponing execution of cloudlet "
						+ cloudlet.getCloudletId() + ": bount VM not available");
			}
		}

		Log.printLine(CloudSim.clock() + ": " + getName()
				+ ": Sending cloudlet " + cloudlet.getCloudletId() + " to VM #"
				+ vm.getId());
		cloudlet.setVmId(vm.getId());
		sendNow(getVmsToDatacentersMap().get(vm.getId()),
				CloudSimTags.CLOUDLET_SUBMIT, cloudlet);
		cloudletsSubmitted++;
	}

	/**
	 * Process a cloudlet return event.
	 * 
	 * @param ev
	 *            a SimEvent object
	 * @pre ev != $null
	 * @post $none
	 */
	protected void processCloudletReturn(SimEvent ev) {
		Cloudlet cloudlet = (Cloudlet) ev.getData();
		getCloudletReceivedList().add(cloudlet);
		Log.printLine(CloudSim.clock() + ": " + getName() + ": Cloudlet "
				+ cloudlet.getCloudletId() + " received");
		cloudletsSubmitted--;

		for (Task t : taskList) {
			if (t.contains(cloudlet)) {
				t.complete(cloudlet);
				List<Cloudlet> readylist = t.getRunnableSubtasks(cloudlet);
				for (Cloudlet readyTask : readylist) {
					submitCloudlet(readyTask);
				}
			}
		}

		if (getCloudletList().size() == 0 && cloudletsSubmitted == 0) { // all
																		// cloudlets
																		// executed
			Log.printLine(CloudSim.clock() + ": " + getName()
					+ ": All Cloudlets executed. Finishing...");
			clearDatacenters();
			finishExecution();
		} else { // some cloudlets haven't finished yet
			if (getCloudletList().size() > 0 && cloudletsSubmitted == 0) {
				// all the cloudlets sent finished. It means that some bount
				// cloudlet is waiting its VM be created
				clearDatacenters();
				createVmsInDatacenter(0);
			}
		}
	}

}
