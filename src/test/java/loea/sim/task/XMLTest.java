package loea.sim.task;

import java.util.List;

import loea.sched.task.Subtask;
import loea.sched.task.Task;

public class XMLTest {

	public static void main(String[] args) {
		List<Task> taskList = Task.XMLImporter("configs/customer1.xml");
		for (Task t : taskList) {
			System.out.println(t.id);
			for (Subtask st : t) {
				System.out.println("\t" + st.getCloudletId());
			}
		}
	}
}
