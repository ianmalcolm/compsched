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

public class Task implements Iterable<SubTask> {

	private DirectedAcyclicGraph<SubTask, DefaultEdge> graph = null;

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

		DOTImporter<SubTask, DefaultEdge> importer = new DOTImporter<SubTask, DefaultEdge>(
				new VertexProvider<SubTask>() {
					@Override
					public SubTask buildVertex(String label,
							Map<String, String> attributes) {
						int id = Integer.parseInt(label);
						long length = Long.parseLong(attributes
								.get(Task.CLOUDLETLENGTH));
						return new SubTask(id, length);
					}
				}, new EdgeProvider<SubTask, DefaultEdge>() {
					@Override
					public DefaultEdge buildEdge(SubTask from, SubTask to,
							String label, Map<String, String> attributes) {
						// TODO Auto-generated method stub
						return new DefaultEdge();
					}
				}, null);

		graph = new DirectedAcyclicGraph<SubTask, DefaultEdge>(
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

		calcHeight();
	}

	public void setUserId(int _id) {
		Iterator<SubTask> it = graph.iterator();
		while (it.hasNext()) {
			it.next().setUserId(_id);
		}
	}

	public Task(int _id, int tArriving, int prio) {
		graph = new DirectedAcyclicGraph<SubTask, DefaultEdge>(
				DefaultEdge.class);
		timeArriving = tArriving;
		priority = prio;
		completedSubTask = new ArrayList<Cloudlet>();
		id = _id;
	}

	public void addSubTask(SubTask st) {
		graph.addVertex(st);
		st.setParent(this);

		// when first added to a task, the subtask has no dependency.
		// therefore the height must be 0
		st.setHeight(0);
	}

	public boolean addDependency(SubTask src, SubTask dst) {
		try {
			graph.addDagEdge(src, dst);
			return true;
		} catch (CycleFoundException e) {
			e.printStackTrace();
			return false;
		}
	}

	// calculate the height of all subtasks
	private void calcHeight() {
		for (SubTask st : this) {
			for (DefaultEdge edge : graph.incomingEdgesOf(st)) {
				int srcHeight = graph.getEdgeSource(edge).getHeight();
				if (st.getHeight() < srcHeight + 1) {
					st.setHeight(srcHeight + 1);
				}
			}
		}
	}

	public boolean complete(SubTask cl) {
		if (!graph.containsVertex(cl)) {
			return false;
		}
		if (completedSubTask.contains(cl)) {
			return false;
		}
		completedSubTask.add(cl);
		return true;
	}

	public boolean complete(Cloudlet cl) {
		return complete((SubTask) cl);
	}

	public List<Cloudlet> getRunnableSubtasks() {

		List<Cloudlet> runnableSubtasks = new ArrayList<Cloudlet>();
		Iterator<SubTask> it = graph.iterator();

		while (it.hasNext()) {
			SubTask st = it.next();

			if (graph.inDegreeOf(st) == 0) {
				if (!completedSubTask.contains(st)) {
					runnableSubtasks.add(st);
				}
			} else {
				if (isReady(st)) {
					runnableSubtasks.add(st);
				}
			}
		}

		return runnableSubtasks;
	}

	private boolean isReady(SubTask st) {
		if (!graph.containsVertex(st)) {
			return false;
		}
		if (completedSubTask.contains(st)) {
			return false;
		}
		Set<DefaultEdge> edges = graph.incomingEdgesOf(st);
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

	public List<SubTask> getRunnableSubtasks(SubTask src) {

		List<SubTask> runnableSubtasks = new ArrayList<SubTask>();

		if (!graph.containsVertex(src)) {
			return null;
		}
		if (!completedSubTask.contains(src)) {
			return null;
		}

		Set<DefaultEdge> edges = graph.outgoingEdgesOf(src);
		for (DefaultEdge e : edges) {
			SubTask dst = graph.getEdgeTarget(e);
			if (isReady(dst)) {
				runnableSubtasks.add(dst);
			}
		}

		return runnableSubtasks;
	}

	public List<Cloudlet> getRunnableSubtasks(Cloudlet src) {
		List<Cloudlet> clResults = new ArrayList<Cloudlet>();
		List<SubTask> stResults = getRunnableSubtasks((SubTask) src);
		for (SubTask st : stResults) {
			clResults.add(st);
		}
		return clResults;
	}

	public String exportGraph() {
		DOTExporter<SubTask, DefaultEdge> exporter = new DOTExporter<SubTask, DefaultEdge>(
				new VertexNameProvider<SubTask>() {
					@Override
					public String getVertexName(SubTask vertex) {
						return String.valueOf(vertex.getCloudletId());
					}
				}, null, null, new ComponentAttributeProvider<SubTask>() {
					@Override
					public Map<String, String> getComponentAttributes(
							SubTask component) {
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

	public boolean contains(SubTask v) {
		return graph.containsVertex(v);
	}

	public boolean contains(Cloudlet v) {
		return graph.containsVertex((SubTask) v);
	}

	public static Task randomTask(int numV, double probE, int brokerId) {

		Task task = new Task();
		ArrayList<SubTask> subtasks = new ArrayList<SubTask>();
		Random r = new Random();

		for (int i = 0; i < numV; i++) {
			SubTask st = new SubTask(i, r.nextInt(MAXIMUMLENGTH));
			st.setVmId(-1);
			st.setUserId(brokerId);
			subtasks.add(st);
			task.addSubTask(st);
		}

		// create a DAG with a lower triangular matrix
		for (int i = 0; i < numV; i++) {
			for (int j = 0; j < i; j++) {
				double dice = r.nextDouble();
				if (dice < probE) {
					task.addDependency(subtasks.get(j), subtasks.get(i));
				}
			}
		}

		task.calcHeight();

		return task;

	}

	@Override
	public Iterator<SubTask> iterator() {
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
