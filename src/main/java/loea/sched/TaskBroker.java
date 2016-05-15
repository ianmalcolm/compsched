package loea.sched;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import loea.sched.scheduler.TaskScheduler;
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
public class TaskBroker extends DatacenterBroker {

	private final TaskScheduler scheduler;
	private final static double SCHEDULING_PERIOD = 0.2;

	private List<Task> futureTask;

	public TaskBroker(String name, TaskScheduler _scheduler) throws Exception {
		super(name);
		scheduler = _scheduler;
	}

	public void submitTaskList(List<Task> list) {
		futureTask = new ArrayList<Task>();
		for (Task t : list) {
			if (t.getArrivalTime() <= CloudSim.clock()) {
				scheduler.submitTask(t);
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
					CompSchedTags.TASK_INCOMING, t);
		}
		futureTask = null;
	}

	/**
	 * Processes events available for this Broker.
	 * 
	 * @param ev
	 *            a SimEvent object
	 * @pre ev != null
	 * @post $none
	 */
	@Override
	public void processEvent(SimEvent ev) {
		switch (ev.getTag()) {
		// Resource characteristics request
		case CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST:
			processResourceCharacteristicsRequest(ev);
			break;
		// Resource characteristics answer
		case CloudSimTags.RESOURCE_CHARACTERISTICS:
			processResourceCharacteristics(ev);
			break;
		// VM Creation answer
		case CloudSimTags.VM_CREATE_ACK:
			processVmCreate(ev);
			break;
		// A finished cloudlet returned
		case CloudSimTags.CLOUDLET_RETURN:
			processCloudletReturn(ev);
			break;
		// if the simulation finishes
		case CloudSimTags.END_OF_SIMULATION:
			shutdownEntity();
			break;
		// Receiving incoming Task
		case CloudSimTags.EXPERIMENT:
			CompSchedEvent csEv = new CompSchedEvent(ev);
			processCompSchedEvent(csEv);
			break;
		// other unknown tags are processed by this method
		default:
			processOtherEvent(ev);
			break;
		}
	}

	/**
	 * Gets the task list.
	 * 
	 * @param <T>
	 *            the generic type
	 * @return the task list
	 */
	public List<Task> getTaskList() {
		return scheduler.getTaskList();
	}

	/**
	 * Submit cloudlets to the created VMs.
	 * 
	 * @pre $none
	 * @post $none
	 */
	protected void submitCloudlets() {
		Map<Subtask, Vm> map = scheduler.schedule();
		if (!map.isEmpty()) {
			submitCloudlets(map);
		}
		processScheduling();
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
			scheduler.addVM(vm);
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

		scheduler.completed((Subtask) cloudlet);
		Map<Subtask, Vm> map = scheduler.schedule();
		if (!map.isEmpty()) {
			submitCloudlets(map);
		}

		if (scheduler.isComplete()) { // all subtasks executed
			Log.printLine(CloudSim.clock() + ": " + getName()
					+ ": All tasks executed. Idle...");
//			clearDatacenters();
//			finishExecution();
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
			scheduler.removeVM(vm);
		}

		getVmsCreatedList().clear();
	}

	public TaskScheduler getScheduler() {
		return scheduler;
	}

	protected void send(int entityId, double delay, CompSchedTags _tags,
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
		scheduler.submitTask(t);
		submitCloudlets();
	}

	protected void processScheduling(CompSchedEvent ev) {
//		Log.printLine(CloudSim.clock() + ": " + getName()
//				+ ": recevie a scheduling request from " + ev.getEntSrc());

		processScheduling();
	}

	protected void processScheduling() {
		if (!scheduler.isComplete()) {
			send(getId(), SCHEDULING_PERIOD, CompSchedTags.SCHEDULING, null);
		}
	}

	protected void processCompSchedEvent(CompSchedEvent ev) {

		switch (ev.getTag()) {

		case TASK_INCOMING:
			processIncomingTask(ev);
			break;
		case SCHEDULING:
			processScheduling(ev);
			break;
		default:
			break;
		}
	}

}