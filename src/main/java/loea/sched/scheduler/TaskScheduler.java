package loea.sched.scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import loea.sched.CompSchedEvent;
import loea.sched.CompSchedTag;
import loea.sched.task.Subtask;
import loea.sched.task.Task;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.lists.VmList;

/**
 * @author ian
 *
 */
public class TaskScheduler extends DatacenterBroker {

	private final static double SCHEDULING_PERIOD = 0.2;

	private final List<Task> taskExecList = new ArrayList<Task>();
	private final List<Task> taskPausedList = new ArrayList<Task>();
	private final List<Task> taskFinishedList = new ArrayList<Task>();
	private final List<Task> futureTask = new ArrayList<Task>();

	public TaskScheduler(String name) throws Exception {
		super(name);
	}

	public void submitTaskList(List<Task> list) {
		for (Task t : list) {
			if (t.getArrivalTime() <= CloudSim.clock()) {
				taskExecList.add(t);
			} else {
				futureTask.add(t);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see cloudsim.core.SimEntity#startEntity()
	 */
	@Override
	public void startEntity() {
		Log.printLine(getName() + " is starting...");
		schedule(getId(), 0, CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST);
		for (Task t : futureTask) {
			send(getId(), t.getArrivalTime() - CloudSim.clock(),
					CompSchedTag.TASK_INCOMING, t);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.cloudbus.cloudsim.DatacenterBroker#processOtherEvent(org.cloudbus
	 * .cloudsim.core.SimEvent)
	 */
	@Override
	protected void processOtherEvent(SimEvent ev) {
		switch (ev.getTag()) {
		case CloudSimTags.EXPERIMENT:
			CompSchedEvent csEv = new CompSchedEvent(ev);
			processCompSchedEvent(csEv);
			break;
		// other unknown tags are processed by this method
		default:
			super.processOtherEvent(ev);

		}
	}

	/**
	 * Submit cloudlets to the created VMs.
	 * 
	 * @pre $none
	 * @post $none
	 */
	protected void submitCloudlets() {
		Map<Subtask, Vm> map = schedule();
		if (!map.isEmpty()) {
			submitCloudlets(map);
		}
		processScheduling();
	}

	private Map<Subtask, Vm> schedule() {
		List<Subtask> runnableCloudlets = new ArrayList<Subtask>();
		for (Task t : taskExecList) {
			List<Subtask> list = t.getRunnableSubtasks();
			runnableCloudlets.addAll(list);
		}

		Map<Subtask, Vm> map = mapSubtasks2VMs(runnableCloudlets);
		return map;
	}

	/**
	 * map subtasks to the VMs
	 * 
	 * @param cloudlets
	 */
	private Map<Subtask, Vm> mapSubtasks2VMs(List<Subtask> subtasks) {

		Map<Subtask, Vm> map = new HashMap<Subtask, Vm>();

		// map the binded subtasks
		// TODO

		return map;
	}

	/**
	 * Submit subtasks to the created VMs
	 * 
	 * @param cloudlets
	 */
	protected void submitCloudlets(Map<Subtask, Vm> map) {
		Iterator<Entry<Subtask, Vm>> it = map.entrySet().iterator();
		while (it.hasNext()) {
			Entry<Subtask, Vm> entry = it.next();
			submitCloudlet(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Submit a Cloudlet to the created VMs
	 * 
	 * @param cloudlets
	 */
	protected void submitCloudlet(Subtask subtask, Vm vm) {
		Log.printLine(CloudSim.clock() + ": " + getName()
				+ ": Sending cloudlet " + subtask.getCloudletId() + " to VM #"
				+ vm.getId());
		subtask.setVmId(vm.getId());
		getCloudletList().add(subtask);
		subtask.issued();
		sendNow(getVmsToDatacentersMap().get(vm.getId()),
				CloudSimTags.CLOUDLET_SUBMIT, subtask);
	}

	/**
	 * Process the ack received due to a request for VM creation.
	 * 
	 * @param ev
	 *            a SimEvent object
	 * @pre ev != null
	 * @post $none
	 */
	protected void processVmCreate(SimEvent ev) {
		int[] data = (int[]) ev.getData();
		int datacenterId = data[0];
		int vmId = data[1];
		int result = data[2];

		if (result == CloudSimTags.TRUE) {
			getVmsToDatacentersMap().put(vmId, datacenterId);
			Vm vm = VmList.getById(getVmList(), vmId);
			getVmsCreatedList().add(vm);
			Log.printLine(CloudSim.clock()
					+ ": "
					+ getName()
					+ ": VM #"
					+ vmId
					+ " has been created in Datacenter #"
					+ datacenterId
					+ ", Host #"
					+ VmList.getById(getVmsCreatedList(), vmId).getHost()
							.getId());
		} else {
			Log.printLine(CloudSim.clock() + ": " + getName()
					+ ": Creation of VM #" + vmId + " failed in Datacenter #"
					+ datacenterId);
		}

		incrementVmsAcks();

		// all the requested VMs have been created
		if (getVmsCreatedList().size() == getVmList().size()
				- getVmsDestroyed()) {
			submitCloudlets();
		} else {
			// all the acks received, but some VMs were not created
			if (getVmsRequested() == getVmsAcks()) {
				// find id of the next datacenter that has not been tried
				for (int nextDatacenterId : getDatacenterIdsList()) {
					if (!getDatacenterRequestedIdsList().contains(
							nextDatacenterId)) {
						createVmsInDatacenter(nextDatacenterId);
						return;
					}
				}

				// all datacenters already queried
				if (getVmsCreatedList().size() > 0) { // if some vm were created
					submitCloudlets();
				} else { // no vms created. abort
					Log.printLine(CloudSim.clock()
							+ ": "
							+ getName()
							+ ": none of the required VMs could be created. Aborting");
					finishExecution();
				}
			}
		}
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
		completed((Subtask) cloudlet);

		Map<Subtask, Vm> map = schedule();
		if (!map.isEmpty()) {
			submitCloudlets(map);
		}

		if (isComplete()) { // all subtasks executed
			Log.printLine(CloudSim.clock() + ": " + getName()
					+ ": All tasks executed. Idle...");
			// clearDatacenters();
			// finishExecution();
		}
	}

	/**
	 * Destroy the virtual machines running in datacenters.
	 * 
	 * @pre $none
	 * @post $none
	 */
	protected void clearDatacenters() {
		for (Vm vm : getVmsCreatedList()) {
			Log.printLine(CloudSim.clock() + ": " + getName()
					+ ": Destroying VM #" + vm.getId());
			sendNow(getVmsToDatacentersMap().get(vm.getId()),
					CloudSimTags.VM_DESTROY, vm);
		}

		getVmsCreatedList().clear();
	}

	protected void send(int entityId, double delay, CompSchedTag _tags,
			Object _data) {
		Object data = CompSchedEvent.createTagDataPair(_tags, _data);
		send(entityId, delay, CloudSimTags.EXPERIMENT, data);
	}

	/**
	 * 
	 * @param ev
	 */

	protected void processIncomingTask(CompSchedEvent ev) {
		Task t = (Task) ev.getData();
		taskExecList.add(t);
		submitCloudlets();
	}

	protected void processScheduling(CompSchedEvent ev) {
		// Log.printLine(CloudSim.clock() + ": " + getName()
		// + ": recevie a scheduling request from " + ev.getEntSrc());

		processScheduling();
	}

	protected void processScheduling() {
		if (isComplete()) {
			send(getId(), SCHEDULING_PERIOD, CompSchedTag.PERIODIC_TASK_SCHEDULING, null);
		}
	}

	protected void processCompSchedEvent(CompSchedEvent ev) {

		switch (ev.getTag()) {

		case TASK_INCOMING:
			processIncomingTask(ev);
			break;
		case PERIODIC_TASK_SCHEDULING:
			processScheduling(ev);
			break;
		default:
			throw new UnsupportedOperationException();
		}
	}

	private void completed(Subtask st) {
		st.getParent().completed(st);
		if (st.getParent().isCompleted()) {
			taskExecList.remove(st.getParent());
			taskFinishedList.add(st.getParent());
		}
	}

	private boolean isComplete() {
		if (taskExecList.isEmpty() && taskPausedList.isEmpty()) {
			return true;
		} else {
			return false;
		}
	}
}