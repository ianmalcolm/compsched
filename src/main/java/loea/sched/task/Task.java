package loea.sched.task;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import org.cloudbus.cloudsim.*;
import org.jdom2.*;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.*;

import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph.CycleFoundException;
import org.jgrapht.ext.*;
import org.jgrapht.graph.DefaultEdge;

public class Task implements Iterable<Subtask> {

	private DirectedAcyclicGraph<Subtask, DefaultEdge> graph = null;

	private double arrivalTime = 0;
	private double deadline = Double.POSITIVE_INFINITY;

	private int ref;

	private int priority = 0; // -20~19, -20 is the highest priority

	private static int ID_COUNT = 0;
	public final int id;

	public static final long DEFAULTFILESIZE = 100;
	public static final int DEFAULTPESNUMBER = 1;
	public static final UtilizationModel DEFAULTUTILIZATIONMODEL = new UtilizationModelFull();
	public static final int MAXIMUMLENGTH = 1000000;
	public static final String CLOUDLETLENGTH = "Length";

	protected final List<Subtask> completedSubtasks;
	protected final List<Subtask> issuedSubtasks;

	private static final XPathFactory xFactory = XPathFactory.instance();
	private static final SAXBuilder builder = new SAXBuilder();
	private static final XPathExpression<Element> taskExpr = xFactory.compile(
			"/Customer/Task[@ref]", Filters.element());
	private static final String ARRIVALTIME = "arrivalTime";
	private static final String DEADLINE = "deadline";
	private static final XPathExpression<Element> subtExpr = xFactory.compile(
			"Subtask[@ref]", Filters.element());
	private static final XPathExpression<Element> depeExpr = xFactory.compile(
			"Dependency[@src and @dst]", Filters.element());

	/**
	 * 
	 */
	public Task() {
		this(0, 0);
	}

	public Task(int tArriving, int prio, String graphFile) {

		this(tArriving, prio);

		DOTImporter<Subtask, DefaultEdge> importer = new DOTImporter<Subtask, DefaultEdge>(
				new VertexProvider<Subtask>() {
					@Override
					public Subtask buildVertex(String label,
							Map<String, String> attributes) {
						long length = Long.parseLong(attributes
								.get(Task.CLOUDLETLENGTH));
						return new Subtask(length);
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

	private Task(int tArriving, int prio) {
		arrivalTime = tArriving;
		priority = prio;
		completedSubtasks = new ArrayList<Subtask>();
		issuedSubtasks = new ArrayList<Subtask>();
		graph = new DirectedAcyclicGraph<Subtask, DefaultEdge>(
				DefaultEdge.class);
		id = ID_COUNT++;
	}

	public boolean isCompleted() {
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

	private boolean addDependency(int srcRef, int dstRef) {
		Subtask src = findSubtaskbyRef(srcRef);
		Subtask dst = findSubtaskbyRef(dstRef);
		if (src != null && dst != null) {
			return addDependency(src, dst);
		} else {
			return false;
		}
	}

	private Subtask findSubtaskbyRef(int _stRef) {
		for (Subtask st : this) {
			if (st.getRef() == _stRef) {
				return st;
			}
		}
		return null;
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
			if (isReady(st)) {
				runnableSubtasks.add(st);
			}
		}
		return runnableSubtasks;
	}

	protected boolean isReady(Subtask st) {
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
			Subtask st = new Subtask(r.nextInt(MAXIMUMLENGTH) + 200);
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

	public static List<Task> XMLImporter(String file) {

		List<Task> taskList = new ArrayList<Task>();

		try {

			Document doc = (Document) builder.build(file);
			doc.getRootElement();

			for (Element taskEle : taskExpr.evaluate(doc)) {

				Task task = new Task();

				String arri = taskEle.getChildTextNormalize(ARRIVALTIME);
				if (arri != null && !arri.isEmpty()) {
					task.setArrivalTime(Double.parseDouble(arri));
				}

				String dead = taskEle.getChildTextNormalize(DEADLINE);
				if (dead != null && !dead.isEmpty()) {
					task.setDeadline(Double.parseDouble(dead));
				}

				for (Element subtEle : subtExpr.evaluate(taskEle)) {
					Subtask subtask = Subtask.XMLImporter(subtEle);
					task.addSubtask(subtask);
				}

				for (Element depeEle : depeExpr.evaluate(taskEle)) {
					String srcAtt = depeEle.getAttributeValue("src");
					String dstAtt = depeEle.getAttributeValue("dst");
					if (srcAtt != null && dstAtt != null) {
						int src = Integer.parseInt(srcAtt);
						int dst = Integer.parseInt(dstAtt);
						task.addDependency(src, dst);
					}
				}

				taskList.add(task);
			}

		} catch (IOException io) {
			System.out.println(io.getMessage());
		} catch (JDOMException jdomex) {
			System.out.println(jdomex.getMessage());
		}

		return taskList;
	}

	@Override
	public Iterator<Subtask> iterator() {
		return graph.iterator();
	}

	public double getArrivalTime() {
		return arrivalTime;
	}

	void setArrivalTime(double _arrivalTime) {
		arrivalTime = _arrivalTime;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int _priority) {
		priority = _priority;
	}

	public double getDeadline() {
		return deadline;
	}

	public void setDeadline(double _deadline) {
		deadline = _deadline;
	}

	public int getId() {
		return id;
	}

	public int getRef() {
		return ref;
	}

	public void setRef(int _ref) {
		ref = _ref;
	}
}
