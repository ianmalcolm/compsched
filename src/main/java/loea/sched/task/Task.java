package loea.sched.task;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.jgrapht.Graph;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph.CycleFoundException;
import org.jgrapht.ext.ComponentAttributeProvider;
import org.jgrapht.ext.EdgeProvider;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.DOTImporter;
import org.jgrapht.ext.ImportException;
import org.jgrapht.ext.VertexNameProvider;
import org.jgrapht.ext.VertexProvider;
import org.jgrapht.graph.DefaultEdge;

/**
 * @author ian
 *
 */
public class Task implements Iterable<Subtask> {

	private final DirectedAcyclicGraph<Subtask, DefaultEdge> graph = new DirectedAcyclicGraph<Subtask, DefaultEdge>(
			DefaultEdge.class);

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

	public static final Comparator<Task> arrivalTimeComparator = new Comparator<Task>() {
		@Override
		public int compare(Task arg0, Task arg1) {
			if (arg0.getArrivalTime() < arg1.getArrivalTime()) {
				return -1;
			} else if (arg0.getArrivalTime() == arg1.getArrivalTime()) {
				return 0;
			} else {
				return 1;
			}
		}
	};

	public static final Comparator<Task> deadlineComparator = new Comparator<Task>() {
		@Override
		public int compare(Task arg0, Task arg1) {
			if (arg0.getDeadline() < arg1.getDeadline()) {
				return -1;
			} else if (arg0.getDeadline() == arg1.getDeadline()) {
				return 0;
			} else {
				return 1;
			}
		}
	};

	public static final Comparator<Subtask> criticalPathComparator = new Comparator<Subtask>() {
		@Override
		public int compare(Subtask arg0, Subtask arg1) {
			if (arg0.getCriticalPathToExit() < arg1.getCriticalPathToExit()) {
				return -1;
			} else if (arg0.getCriticalPathToExit() == arg1
					.getCriticalPathToExit()) {
				return 0;
			} else {
				return 1;
			}
		}
	};

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
	public void calcHeight() {
		for (Subtask st : this) {
			for (DefaultEdge edge : graph.incomingEdgesOf(st)) {
				int srcHeight = graph.getEdgeSource(edge).getHeight();
				if (st.getHeight() < srcHeight + 1) {
					st.setHeight(srcHeight + 1);
				}
			}
		}
	}

	/**
	 * Calculate the critical path length from every vertex to the exit of the
	 * graph
	 */
	public void calcCriticalPathLength() {

		DirectedAcyclicGraph<Subtask, DefaultEdge> tempGraph = new DirectedAcyclicGraph<Subtask, DefaultEdge>(
				DefaultEdge.class) {
			/**
					 * 
					 */
			private static final long serialVersionUID = 5689924817978034026L;

			/**
			 * @see Graph#getEdgeWeight(Object)
			 */
			@Override
			public double getEdgeWeight(DefaultEdge e) {
				return getEdgeSource(e).getCloudletLength();
			}
		};

		// duplicate graph
		for (Subtask st : graph.vertexSet()) {
			tempGraph.addVertex(st);
		}
		for (DefaultEdge e : graph.edgeSet()) {
			Subtask src = graph.getEdgeSource(e);
			Subtask dst = graph.getEdgeTarget(e);
			try {
				tempGraph.addDagEdge(src, dst);
			} catch (CycleFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		// add dummy exit
		List<Subtask> realExitList = new ArrayList<Subtask>();
		for (Subtask st : tempGraph.vertexSet()) {
			if (tempGraph.outDegreeOf(st) == 0) {
				realExitList.add(st);
			}
		}
		Subtask dummyExit = new Subtask(0);
		tempGraph.addVertex(dummyExit);
		for (Subtask realExit : realExitList) {
			try {
				tempGraph.addDagEdge(realExit, dummyExit);
			} catch (CycleFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		LongestPath<Subtask, DefaultEdge> lp = new LongestPath<Subtask, DefaultEdge>(
				tempGraph);

		for (Subtask st : graph.vertexSet()) {
			long cplen = (long) lp.getPathLength(st);
			st.setCriticalPathToExit(cplen);
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

		final XPathFactory xFactory = XPathFactory.instance();
		final SAXBuilder builder = new SAXBuilder();
		final XPathExpression<Element> taskExpr = xFactory.compile(
				"/Customer/Task[@ref]", Filters.element());
		final String ARRIVALTIME = "arrivalTime";
		final String DEADLINE = "deadline";
		final XPathExpression<Element> subtExpr = xFactory.compile(
				"Subtask[@ref]", Filters.element());
		final XPathExpression<Element> depeExpr = xFactory.compile(
				"Dependency[@src and @dst]", Filters.element());

		List<Task> taskList = new ArrayList<Task>();

		try {

			Document doc = (Document) builder.build(file);
			doc.getRootElement();

			for (Element taskEle : taskExpr.evaluate(doc)) {

				Task task = new Task();

				String _ref = taskEle.getAttributeValue("ref");
				if (_ref != null && !_ref.isEmpty()) {
					task.setRef(Integer.parseInt(_ref));
				}

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

				task.calcCriticalPathLength();
				task.calcHeight();

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
