package loea.task;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import loea.sched.task.Task;

import org.cloudbus.cloudsim.Cloudlet;
import org.jgrapht.ext.ComponentAttributeProvider;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.DOTImporter;
import org.jgrapht.ext.EdgeProvider;
import org.jgrapht.ext.ImportException;
import org.jgrapht.ext.VertexNameProvider;
import org.jgrapht.ext.VertexProvider;
import org.jgrapht.graph.DefaultEdge;
import org.junit.Test;

public class TaskTest {

	@Test
	public void SerializationTest() {

	}

	public static void main(String[] args) {
		
		Task s = Task.randomTask(8, 0.5);
		
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
		exporter.export(writer, s);
		System.out.println(writer.toString());

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

		Task t = new Task();
		try {
			importer.read(writer.toString(), t);
		} catch (ImportException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		StringWriter writer2 = new StringWriter();
		exporter.export(writer2, t);
		System.out.println(writer2.toString());
	}
}
