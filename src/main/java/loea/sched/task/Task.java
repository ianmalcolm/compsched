package loea.sched.task;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph.CycleFoundException;
import org.jgrapht.ext.ComponentAttributeProvider;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.DOTImporter;
import org.jgrapht.ext.EdgeProvider;
import org.jgrapht.ext.ImportException;
import org.jgrapht.ext.VertexNameProvider;
import org.jgrapht.ext.VertexProvider;
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

	public Task(int _id, int tArriving, int prio, String graphFile) {

		DOTImporter<Cloudlet, DefaultEdge> importer = new DOTImporter<Cloudlet, DefaultEdge>(
				new VertexProvider<Cloudlet>() {
					@Override
					public Cloudlet buildVertex(String label,
							Map<String, String> attributes) {
						int id = Integer.parseInt(label);
						long length = Long.parseLong(attributes
								.get(Task.CLOUDLETLENGTH));
						return new Cloudlet(id, length, Task.DEFAULTPESNUMBER,
								Task.DEFAULTFILESIZE, Task.DEFAULTFILESIZE,
								Task.DEFAULTUTILIZATIONMODEL,
								Task.DEFAULTUTILIZATIONMODEL,
								Task.DEFAULTUTILIZATIONMODEL);
					}
				}, new EdgeProvider<Cloudlet, DefaultEdge>() {
					@Override
					public DefaultEdge buildEdge(Cloudlet from, Cloudlet to,
							String label, Map<String, String> attributes) {
						// TODO Auto-generated method stub
						return new DefaultEdge();
					}
				}, null);

		graph = new DirectedAcyclicGraph<Cloudlet, DefaultEdge>(
				DefaultEdge.class);

		try {
			byte[] encoded = Files.readAllBytes(Paths.get(graphFile));
			importer.read(new String(encoded), graph);
		} catch (ImportException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		timeArriving = tArriving;
		priority = prio;
		completedSubTask = new ArrayList<Cloudlet>();
		id = _id;
	}

	public void setUserId(int _id) {
		Iterator<Cloudlet> it = graph.iterator();
		while (it.hasNext()) {
			it.next().setUserId(_id);
		}
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
		if (completedSubTask.contains(cl)) {
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

	public String exportGraph() {
		DOTExporter<Cloudlet, DefaultEdge> exporter = new DOTExporter<Cloudlet, DefaultEdge>(
				new VertexNameProvider<Cloudlet>() {
					@Override
					public String getVertexName(Cloudlet vertex) {
						return String.valueOf(vertex.getCloudletId());
					}
				}, null, null, new ComponentAttributeProvider<Cloudlet>() {
					@Override
					public Map<String, String> getComponentAttributes(
							Cloudlet component) {
						Map<String, String> map = new HashMap<String, String>();
						map.put(Task.CLOUDLETLENGTH,
								String.valueOf(component.getCloudletLength()));
						return map;
					}
				}, null);

		StringWriter writer = new StringWriter();
		exporter.export(writer, this.graph);
		return writer.toString();
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

	public int getTimeArriving() {
		return timeArriving;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public int getId() {
		return id;
	}
}
