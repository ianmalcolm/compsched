package loea.graph;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.JFrame;

import loea.sched.task.SubTask;

import org.jgraph.JGraph;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph.CycleFoundException;
import org.jgrapht.ext.JGraphModelAdapter;
import org.jgrapht.graph.DefaultEdge;
import org.junit.Test;

public class GraphTest {

	@Test
	public DirectedAcyclicGraph<SubTask, DefaultEdge> RandomDAG() {

		int numSubTask = 10;
		double probDep = 0.3;

		DirectedAcyclicGraph<SubTask, DefaultEdge> g = new DirectedAcyclicGraph<SubTask, DefaultEdge>(
				DefaultEdge.class);

		ArrayList<SubTask> l = new ArrayList<SubTask>();

		for (int i = 0; i < numSubTask; i++) {
			SubTask st = new SubTask();
			l.add(st);
			g.addVertex(st);
		}

		Random r = new Random();

		// create a DAG with a lower triangular matrix
		for (int i = 0; i < numSubTask; i++) {
			for (int j = 0; j < i; j++) {
				double dice = r.nextDouble();
				if (dice > probDep) {
					try {
						g.addDagEdge(l.get(i), l.get(j));
					} catch (CycleFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}

		System.out.println(g.toString());

		return g;
	}

	public static void main(String[] args) {
		JFrame frame = new JFrame("JFrame Source Demo");
		// Add a window listner for close button
		frame.addWindowListener(new WindowAdapter() {

			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});

		GraphTest gt = new GraphTest();
		DirectedAcyclicGraph<SubTask, DefaultEdge> g = gt.RandomDAG();
		// create a visualization using JGraph, via the adapter
		JGraph jgraph = new JGraph(new JGraphModelAdapter<SubTask, DefaultEdge>(g));

		// This is an empty content area in the frame
		frame.getContentPane().add(jgraph);
		frame.pack();
		frame.setVisible(true);
	}
}
