package loea.sched.task;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph.CycleFoundException;
import org.jgrapht.graph.DefaultEdge;

public class Task implements Iterable<Cloudlet> {

	private DirectedAcyclicGraph<Cloudlet, DefaultEdge> graph = null;

	private int timeArriving = 0;
	private int priority = 0; // -20~19, -20 is the highest priority
	private int id = 0;

	public static final long DEFAULTFILESIZE = 100;
	public static final int DEFAULTPESNUMBER = 1;
	public static final UtilizationModel DEFAULTUTILIZATIONMODEL = new UtilizationModelFull();
	public static final int MAXIMUMLENGTH = 1000000;
	public static final String CLOUDLETLENGTH = "Length";

	private List<Cloudlet> completedSubTask = null;

	/**
	 * 
	 */
	public Task() {
		this(0, 0, 0);
	}

	public Task(int _id, int tArriving, int prio) {
		graph = new DirectedAcyclicGraph<Cloudlet, DefaultEdge>(
				DefaultEdge.class);
		timeArriving = tArriving;
		priority = prio;
		completedSubTask = new ArrayList<Cloudlet>();
		id = _id;
	}

	public void addSubTask(Cloudlet cl) {
		graph.addVertex(cl);
	}

	public boolean addDependency(Cloudlet src, Cloudlet dst) {
		try {
			graph.addDagEdge(src, dst);
			return true;
		} catch (CycleFoundException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean complete(Cloudlet cl) {
		if (!graph.containsVertex(cl)) {
			return false;
		}
		if (completedSubTask.contains(cl)) {
			return false;
		}
		completedSubTask.add(cl);
		return true;
	}

	public List<Cloudlet> getRunnableSubtasks() {

		List<Cloudlet> runnableSubtasks = new ArrayList<Cloudlet>();
		Iterator<Cloudlet> it = graph.iterator();

		while (it.hasNext()) {
			Cloudlet cl = it.next();

			if (graph.inDegreeOf(cl) == 0) {
				if (!completedSubTask.contains(cl)) {
					runnableSubtasks.add(cl);
				}
			} else {
				if (isReady(cl)) {
					runnableSubtasks.add(cl);
				}
			}
		}

		return runnableSubtasks;
	}

	private boolean isReady(Cloudlet cl) {
		if (!graph.containsVertex(cl)) {
			return false;
		}
		if (!completedSubTask.contains(cl)) {
			return false;
		}
		Set<DefaultEdge> edges = graph.incomingEdgesOf(cl);
		boolean allPrecedentComplete = true;
		for (DefaultEdge e : edges) {
			Cloudlet src = graph.getEdgeSource(e);
			if (!completedSubTask.contains(src)) {
				allPrecedentComplete = false;
				break;
			}
		}
		if (allPrecedentComplete) {
			return true;
		} else {
			return false;
		}
	}

	public List<Cloudlet> getRunnableSubtasks(Cloudlet src) {

		List<Cloudlet> runnableSubtasks = new ArrayList<Cloudlet>();

		if (!graph.containsVertex(src)) {
			return null;
		}
		if (!completedSubTask.contains(src)) {
			return null;
		}

		Set<DefaultEdge> edges = graph.outgoingEdgesOf(src);
		for (DefaultEdge e : edges) {
			Cloudlet dst = graph.getEdgeTarget(e);
			if (isReady(dst)) {
				runnableSubtasks.add(dst);
			}
		}

		return runnableSubtasks;
	}

	public boolean contains(Cloudlet v) {
		return graph.containsVertex(v);
	}

	public static Task randomTask(int numV, double probE, int brokerId) {

		Task task = new Task();
		ArrayList<Cloudlet> listCloudlet = new ArrayList<Cloudlet>();
		Random r = new Random();

		for (int i = 0; i < numV; i++) {
			Cloudlet cl = new Cloudlet(i, r.nextInt(MAXIMUMLENGTH),
					DEFAULTPESNUMBER, DEFAULTFILESIZE, DEFAULTFILESIZE,
					DEFAULTUTILIZATIONMODEL, DEFAULTUTILIZATIONMODEL,
					DEFAULTUTILIZATIONMODEL);
			cl.setVmId(-1);
			cl.setUserId(brokerId);
			listCloudlet.add(cl);
			task.addSubTask(cl);
		}

		// create a DAG with a lower triangular matrix
		for (int i = 0; i < numV; i++) {
			for (int j = 0; j < i; j++) {
				double dice = r.nextDouble();
				if (dice < probE) {
					task.addDependency(listCloudlet.get(j), listCloudlet.get(i));
				}
			}
		}

		return task;

	}

	@Override
	public Iterator<Cloudlet> iterator() {
		return graph.iterator();
	}

}
