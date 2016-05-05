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

public class Task implements Iterable<Subtask> {

	private DirectedAcyclicGraph<Subtask, DefaultEdge> graph = null;

	private int timeArriving = 0;

	private int priority = 0; // -20~19, -20 is the highest priority

	private int id = 0;

	public static final long DEFAULTFILESIZE = 100;
	public static final int DEFAULTPESNUMBER = 1;
	public static final UtilizationModel DEFAULTUTILIZATIONMODEL = new UtilizationModelFull();
	public static final int MAXIMUMLENGTH = 1000000;
	public static final String CLOUDLETLENGTH = "Length";

	protected final List<Subtask> completedSubtasks;
	protected final List<Subtask> issuedSubtasks;

	/**
	 * 
	 */
	public Task() {
		this(0, 0, 0);
	}

	public Task(int _id, int tArriving, int prio, String graphFile) {

		this(_id, tArriving, prio);

		DOTImporter<Subtask, DefaultEdge> importer = new DOTImporter<Subtask, DefaultEdge>(
				new VertexProvider<Subtask>() {
					@Override
					public Subtask buildVertex(String label,
							Map<String, String> attributes) {
						int id = Integer.parseInt(label);
						long length = Long.parseLong(attributes
								.get(Task.CLOUDLETLENGTH));
						return new Subtask(id, length);
					}
				}, new EdgeProvider<Subtask, DefaultEdge>() {
					@Override
					public DefaultEdge buildEdge(Subtask from, Subtask to,
							String label, Map<String, String> attributes) {
						return new DefaultEdge();
					}
				}, null);

		graph = new DirectedAcyclicGraph<Subtask, DefaultEdge>(
				DefaultEdge.class);

		try {
			byte[] encoded = Files.readAllBytes(Paths.get(graphFile));
			importer.read(new String(encoded), graph);
		} catch (ImportException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (Subtask st : this) {
			st.setParent(this);
			st.setHeight(0);
		}

		calcHeight();
	}

	private Task(int _id, int tArriving, int prio) {
		timeArriving = tArriving;
		priority = prio;
		completedSubtasks = new ArrayList<Subtask>();
		issuedSubtasks = new ArrayList<Subtask>();
		id = _id;
	}

	public boolean isComplete() {
		if (completedSubtasks != null) {
			if (completedSubtasks.size() == graph.vertexSet().size()) {
				return true;
			}
		}
		return false;
	}

	public void setUserId(int _id) {
		Iterator<Subtask> it = graph.iterator();
		while (it.hasNext()) {
			it.next().setUserId(_id);
		}
	}

	public void addSubtask(Subtask st) {
		graph.addVertex(st);
		st.setParent(this);

		// when first added to a task, the subtask has no dependency.
		// therefore the height must be 0
		st.setHeight(0);
	}

	public boolean addDependency(Subtask src, Subtask dst) {
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
		for (Subtask st : this) {
			for (DefaultEdge edge : graph.incomingEdgesOf(st)) {
				int srcHeight = graph.getEdgeSource(edge).getHeight();
				if (st.getHeight() < srcHeight + 1) {
					st.setHeight(srcHeight + 1);
				}
			}
		}
	}

	public boolean completed(Subtask st) {
		if (!graph.containsVertex(st)) {
			return false;
		}
		if (completedSubtasks.contains(st)) {
			return false;
		}
		completedSubtasks.add(st);
		issuedSubtasks.remove(st);
		return true;
	}

	protected boolean issued(Subtask st) {
		if (st.getParent() != this) {
			return false;
		} else {
			issuedSubtasks.add(st);
			return true;
		}
	}

	public List<Subtask> getRunnableSubtasks() {

		List<Subtask> runnableSubtasks = new ArrayList<Subtask>();
		Iterator<Subtask> it = graph.iterator();

		while (it.hasNext()) {
			Subtask st = it.next();

			if (graph.inDegreeOf(st) == 0 && !completedSubtasks.contains(st)
					&& !issuedSubtasks.contains(st)) {
				runnableSubtasks.add(st);
			} else if (isReady(st)) {
				runnableSubtasks.add(st);
			}

		}

		return runnableSubtasks;
	}

	private boolean isReady(Subtask st) {
		if (!graph.containsVertex(st)) {
			return false;
		}
		if (completedSubtasks.contains(st) || issuedSubtasks.contains(st)) {
			return false;
		}
		Set<DefaultEdge> edges = graph.incomingEdgesOf(st);
		boolean allPrecedentComplete = true;
		for (DefaultEdge e : edges) {
			Cloudlet src = graph.getEdgeSource(e);
			if (!completedSubtasks.contains(src)) {
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

	public String exportGraph() {
		DOTExporter<Subtask, DefaultEdge> exporter = new DOTExporter<Subtask, DefaultEdge>(
				new VertexNameProvider<Subtask>() {
					@Override
					public String getVertexName(Subtask vertex) {
						return String.valueOf(vertex.getCloudletId());
					}
				}, null, null, new ComponentAttributeProvider<Subtask>() {
					@Override
					public Map<String, String> getComponentAttributes(
							Subtask component) {
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

	public boolean contains(Subtask v) {
		return graph.containsVertex(v);
	}

	public boolean contains(Cloudlet v) {
		return graph.containsVertex((Subtask) v);
	}

	protected List<Subtask> getIssuedSubtask() {
		return issuedSubtasks;
	}

	public static Task randomTask(int numV, double probE, int brokerId) {

		Task task = new Task();
		ArrayList<Subtask> subtasks = new ArrayList<Subtask>();
		Random r = new Random();

		for (int i = 0; i < numV; i++) {
			Subtask st = new Subtask(i, r.nextInt(MAXIMUMLENGTH));
			st.setVmId(-1);
			st.setUserId(brokerId);
			subtasks.add(st);
			task.addSubtask(st);
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
	public Iterator<Subtask> iterator() {
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
