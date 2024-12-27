package de.seemoo.dyuan.test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import de.seemoo.dyuan.netgen.NetworkModel;
import de.seemoo.dyuan.netgen.UniformGen;
import de.seemoo.dyuan.netgen.algo.VertexDisjointBoundedPathHeuristic;
import de.seemoo.dyuan.netgen.structure.Node;

public class TestBoundedDisjointPath {
	
	public void run() {
		final NetworkModel network = new NetworkModel();
		UniformGen gen = new UniformGen(network, 50, 100, 100);
		int numNodes = 100;
		gen.addSensors(numNodes, 200);
		gen.buildEdgeAndRandomLQ();
		
		Random rand = new Random();
		int sourceId = rand.nextInt(numNodes);
		int destId;
		while ((destId = rand.nextInt(numNodes)) == sourceId) 
			;
		Node source = network.getNormalNodes().get(sourceId);
		Node dest = network.getNormalNodes().get(destId);
		VertexDisjointBoundedPathHeuristic heurist = new VertexDisjointBoundedPathHeuristic(network.getGraph());
		List<List<Node>> paths = heurist.getMaxBoundedPaths(source, dest, 200);
		Set<Node> nodeSet = new HashSet<Node>();
		for (List<Node> path : paths) {
			for (int i=1; i<path.size()-1; i++) {
				Node node = path.get(i);
				if (nodeSet.contains(node)) {
					throw new IllegalStateException("should not happen.");
				}
				nodeSet.add(node);
			}
		}
		System.out.println("total paths:"+paths.size());
		
//		JFrame frame = new JFrame("Test Network Model");
//		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		JPanel panel = new JPanel(new BorderLayout());
//		frame.getContentPane().add(panel);
//		final BasicVisualizationServer<Node, Edge> netComp = network.getGraphicComponent(); 
//		panel.add(netComp, BorderLayout.CENTER);
//		frame.pack();
//		frame.setVisible(true);
		
	}
	
	public static void main(String[] args) {
		TestBoundedDisjointPath tester = new TestBoundedDisjointPath();
		for (int i=0; i<1; i++) {
			System.out.println("round "+i);
			tester.run();
		}
	}

}
