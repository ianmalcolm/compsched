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

		submitCloudlets(runnableCloudlets);
	}

	/**
	 * Submit Cloudlets to the created VMs
	 * 
	 * @param cloudlets
	 */
	protected void submitCloudlets(List<Cloudlet> cloudlets) {

		class VManditsLoad implements Comparable<VManditsLoad> {
			Vm vm;
			int load;

			public VManditsLoad(Vm _vm) {
				vm = _vm;
				load = vm.getCloudletScheduler().runningCloudlets();
			}

			public void add(int newload) {
				load += newload;
			}

			@Override
			public int compareTo(VManditsLoad arg0) {
				if (load > arg0.load) {
					return 1;
				} else if (load < arg0.load) {
					return -1;
				} else {
					return 0;
				}
			}

		}

		// process binded cloudlets
		{
			List<Cloudlet> toRemove = new ArrayList<Cloudlet>();
			for (Cloudlet cloudlet : cloudlets) {
				if (cloudlet.getVmId() != -1) {
					// submit to the specific vm
					Vm vm = VmList.getById(getVmsCreatedList(),
							cloudlet.getVmId());
					if (vm == null) { // vm was not created
						Log.printLine(CloudSim.clock() + ": " + getName()
								+ ": Postponing execution of cloudlet "
								+ cloudlet.getCloudletId()
								+ ": bount VM not available");
					}
					submitCloudlet(cloudlet, vm);
					toRemove.add(cloudlet);
				}
			}
			cloudlets.removeAll(toRemove);
		}

		// if user didn't bind the cloudlets and they have not been executed
		// yet

		List<VManditsLoad> vmLoadList = new ArrayList<VManditsLoad>();

		for (Vm vm : getVmsCreatedList()) {
			vmLoadList.add(new VManditsLoad(vm));
		}

		for (Cloudlet cloudlet : cloudlets) {
			Collections.sort(vmLoadList);
			Vm vm = vmLoadList.get(0).vm;
			submitCloudlet(cloudlet, vm);
			vmLoadList.get(0).add(1);
		}
	}

	/**
	 * Submit a Cloudlet to the created VMs
	 * 
	 * @param cloudlets
	 */
	protected void submitCloudlet(Cloudlet cloudlet, Vm vm) {
		Log.printLine(CloudSim.clock() + ": " + getName()
				+ ": Sending cloudlet " + cloudlet.getCloudletId() + " to VM #"
				+ vm.getId());
		cloudlet.setVmId(vm.getId());
		getCloudletList().add(cloudlet);
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

		List<Cloudlet> toAdd = new ArrayList<Cloudlet>();

		for (Task t : taskList) {
			if (t.contains(cloudlet)) {
				t.complete(cloudlet);
				List<Cloudlet> readylist = t.getRunnableSubtasks(cloudlet);
				for (Cloudlet readyTask : readylist) {
					toAdd.add(readyTask);
				}
			}
		}
		if (!toAdd.isEmpty()) {
			submitCloudlets(toAdd);
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
