package loea.sim.task;

import java.util.List;

import org.junit.Test;

import loea.sched.task.Task;

public class LongestPathTest extends TaskTest {

	@Test
	public void performanceTest() {

		List<Task> tasks = Task.XMLImporter("configs/cust_s1_t1_st1000_dep.1.xml");
		Task task = tasks.get(0);
		task.calcCriticalPathLength();

	}
}
