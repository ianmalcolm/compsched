package loea.sched.task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.graph.DefaultEdge;

public class Task extends DirectedAcyclicGraph<Cloudlet, DefaultEdge> implements
		Iterable<Cloudlet> {

	private double timeArriving = 0;
	private int priority = 0; // -20~19, -20 is the highest priority

	/**
	 * 
	 */
	private static final long serialVersionUID = -1511248946618648743L;

	public static final long DEFAULTFILESIZE = 100;
	public static final int DEFAULTPESNUMBER = 1;
	public static final UtilizationModel DEFAULTUTILIZATIONMODEL = new UtilizationModelFull();
	public static final int MAXIMUMLENGTH = 1000000;
	public static final String CLOUDLETLENGTH = "Length";

	/**
	 * 
	 */
	public Task() {
		this(0, 0);
	}

	public Task(double tArriving, int prio) {
		super(DefaultEdge.class);
		timeArriving = tArriving;
		priority = prio;
	}

	public static Task randomTask(int numV, double probE) {

		Task task = new Task();
		ArrayList<Cloudlet> listCloudlet = new ArrayList<Cloudlet>();
		Random r = new Random();

		for (int i = 0; i < numV; i++) {
			Cloudlet cl = new Cloudlet(i, r.nextInt(MAXIMUMLENGTH),
					DEFAULTPESNUMBER, DEFAULTFILESIZE, DEFAULTFILESIZE,
					DEFAULTUTILIZATIONMODEL, DEFAULTUTILIZATIONMODEL,
					DEFAULTUTILIZATIONMODEL);
			listCloudlet.add(cl);
			task.addVertex(cl);
		}

		// create a DAG with a lower triangular matrix
		for (int i = 0; i < numV; i++) {
			for (int j = 0; j < i; j++) {
				double dice = r.nextDouble();
				if (dice < probE) {
					try {
						task.addDagEdge(listCloudlet.get(j),
								listCloudlet.get(i));
					} catch (CycleFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}

		return task;

	}

}
