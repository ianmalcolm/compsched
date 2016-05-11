package loea.sched;

import java.util.Calendar;
import java.util.List;

import loea.sched.scheduler.TaskSchedulerBrokerLevel;
import loea.sched.task.Subtask;
import loea.sched.task.Task;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;

public class ScalableSimulator {

	public static void main(String[] args) {
		Log.printLine("Starting Scalable Simulator...");

		try {
			// Initialize the CloudSim package. It should be called
			// before creating any entities.
			int num_user = 1; // number of cloud users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = true; // mean trace events

			// Initialize the CloudSim library
			CloudSim.init(num_user, calendar, trace_flag);

			@SuppressWarnings("unused")
			List<Datacenter> centerList = ProviderImporter
					.XMLImporter("configs/provider1.xml");

			TaskBroker broker = new TaskBroker("TaskBroker",
					new TaskSchedulerBrokerLevel());

			// VM XMLImporter
			List<Vm> vmList = VMImporter.XMLImporter("configs/customer1.xml",
					broker.getId());
			// submit VM list to broker
			broker.submitVmList(vmList);

			List<Task> taskList = Task.XMLImporter("configs/customer1.xml");
			for (Task t : taskList) {
				for (Subtask st : t) {
					st.setUserId(broker.getId());
				}
			}
			broker.submitTaskList(taskList);

			// Starts the simulation
			CloudSim.startSimulation();

			CloudSim.stopSimulation();

			// Final step: Print results when simulation is over
			List<Cloudlet> newList = broker.getCloudletReceivedList();
			Simulator.printCloudletList(newList);

			Log.printLine("Scalable Simulator finished!");
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}
	}
}
