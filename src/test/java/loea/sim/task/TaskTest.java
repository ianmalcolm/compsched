package loea.sim.task;

import loea.sched.task.Subtask;
import loea.sched.task.Task;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class TaskTest {

	@Test
	public void criticalPathTest() {
		
		/* create a task graph
		 * 
		 *             st0
		 *              |
		 * -----------------------------
		 * |            |              |
		 * st1         st2            st3          st7
		 * |            |              |
		 * ------st4-----------st5------
		 * |      |             |
		 * |---------------------
		 *              |
		 *             st6
		 *      
		 */
		
		Task task = new Task();
		Subtask st0 = new Subtask(10);
		Subtask st1 = new Subtask(100);
		Subtask st2 = new Subtask(150);
		Subtask st3 = new Subtask(200);
		Subtask st4 = new Subtask(250);
		Subtask st5 = new Subtask(100);
		Subtask st6 = new Subtask(20);
		Subtask st7 = new Subtask(400);
		task.addSubtask(st0);
		task.addSubtask(st1);
		task.addSubtask(st2);
		task.addSubtask(st3);
		task.addSubtask(st4);
		task.addSubtask(st5);
		task.addSubtask(st6);
		task.addSubtask(st7);
		task.addDependency(st0, st1);
		task.addDependency(st0, st2);
		task.addDependency(st0, st3);
		task.addDependency(st1, st4);
		task.addDependency(st2, st4);
		task.addDependency(st2, st5);
		task.addDependency(st3, st5);
		task.addDependency(st1, st6);
		task.addDependency(st4, st6);
		task.addDependency(st5, st6);

		task.calcCriticalPathLength();

		assertEquals(st7.getCriticalPathToExit(), 400);
		assertEquals(st6.getCriticalPathToExit(), 20);
		assertEquals(st5.getCriticalPathToExit(), 120);
		assertEquals(st4.getCriticalPathToExit(), 270);
		assertEquals(st3.getCriticalPathToExit(), 320);
		assertEquals(st2.getCriticalPathToExit(), 420);
		assertEquals(st1.getCriticalPathToExit(), 370);
		assertEquals(st0.getCriticalPathToExit(), 430);
	}
	
	@Test
	public void heightTest() {
		
		/* create a task graph
		 * 
		 *             st0
		 *              |
		 * -----------------------------
		 * |            |              |
		 * st1         st2            st3          st7
		 * |            |              |
		 * ------st4-----------st5------
		 * |      |             |
		 * |---------------------
		 *              |
		 *             st6
		 *      
		 */
		
		Task task = new Task();
		Subtask st0 = new Subtask(10);
		Subtask st1 = new Subtask(100);
		Subtask st2 = new Subtask(150);
		Subtask st3 = new Subtask(200);
		Subtask st4 = new Subtask(250);
		Subtask st5 = new Subtask(100);
		Subtask st6 = new Subtask(20);
		Subtask st7 = new Subtask(400);
		task.addSubtask(st0);
		task.addSubtask(st1);
		task.addSubtask(st2);
		task.addSubtask(st3);
		task.addSubtask(st4);
		task.addSubtask(st5);
		task.addSubtask(st6);
		task.addSubtask(st7);
		task.addDependency(st0, st1);
		task.addDependency(st0, st2);
		task.addDependency(st0, st3);
		task.addDependency(st1, st4);
		task.addDependency(st2, st4);
		task.addDependency(st2, st5);
		task.addDependency(st3, st5);
		task.addDependency(st1, st6);
		task.addDependency(st4, st6);
		task.addDependency(st5, st6);

		task.calcHeight();

		assertEquals(st7.getHeight(), 0);
		assertEquals(st6.getHeight(), 3);
		assertEquals(st5.getHeight(), 2);
		assertEquals(st4.getHeight(), 2);
		assertEquals(st3.getHeight(), 1);
		assertEquals(st2.getHeight(), 1);
		assertEquals(st1.getHeight(), 1);
		assertEquals(st0.getHeight(), 0);
	}
}
