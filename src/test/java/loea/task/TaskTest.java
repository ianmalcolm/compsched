package loea.task;

import loea.sched.task.Task;

import org.junit.Test;

public class TaskTest {

	@Test
	public void SerializationTest() {

	}

	public static void main(String[] args) {

		Task s = Task.randomTask(8, 0.5, 0);

		String g = s.exportGraph();

		System.out.println(g);

		// DOTImporter<Cloudlet, DefaultEdge> importer = new
		// DOTImporter<Cloudlet, DefaultEdge>(
		// new VertexProvider<Cloudlet>() {
		// @Override
		// public Cloudlet buildVertex(String label,
		// Map<String, String> attributes) {
		// int id = Integer.parseInt(label);
		// long length = Long.parseLong(attributes
		// .get(Task.CLOUDLETLENGTH));
		// return new Cloudlet(id, length, Task.DEFAULTPESNUMBER,
		// Task.DEFAULTFILESIZE, Task.DEFAULTFILESIZE,
		// Task.DEFAULTUTILIZATIONMODEL,
		// Task.DEFAULTUTILIZATIONMODEL,
		// Task.DEFAULTUTILIZATIONMODEL);
		// }
		// }, new EdgeProvider<Cloudlet, DefaultEdge>() {
		// @Override
		// public DefaultEdge buildEdge(Cloudlet from, Cloudlet to,
		// String label, Map<String, String> attributes) {
		// // TODO Auto-generated method stub
		// return new DefaultEdge();
		// }
		// }, null);
		//
		// Task t = new Task();
		// try {
		// importer.read(writer.toString(), t);
		// } catch (ImportException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		//
		// StringWriter writer2 = new StringWriter();
		// exporter.export(writer2, t);
		// System.out.println(writer2.toString());
	}
}
