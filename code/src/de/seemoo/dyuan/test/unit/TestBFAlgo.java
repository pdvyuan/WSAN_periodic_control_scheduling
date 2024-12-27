package de.seemoo.dyuan.test.unit;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.seemoo.dyuan.netgen.algo.BellmanFordAlgorithm;
import de.seemoo.dyuan.netgen.algo.EdgeCostTransformer;
import de.seemoo.dyuan.netgen.algo.KDAlgorithm;
import de.seemoo.dyuan.netgen.structure.Edge;
import de.seemoo.dyuan.netgen.structure.Node;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import junit.framework.TestCase;

public class TestBFAlgo extends TestCase {

	private UndirectedSparseGraph<Node, Edge> graph; 
	
	private BellmanFordAlgorithm<Edge> bfAlgo;
	
	private Node n1, n2, n3, n4, n5, n6;
	
	private Edge e13, e12, e23, e24, e34, e35, e46, e54, e56;
	
	//the node is not connected to the graph.
	private Node n7;
	
	protected void setUp() {
		this.graph = new UndirectedSparseGraph<Node, Edge>();
		n1 = new Node("1");
		n2 = new Node("2");
		n3 = new Node("3");
		n4 = new Node("4");
		n5 = new Node("5");
		n6 = new Node("6");
		n7 = new Node("7");
		
		graph.addVertex(n1);
		graph.addVertex(n2);
		graph.addVertex(n3);
		graph.addVertex(n4);
		graph.addVertex(n5);
		graph.addVertex(n6);
		graph.addVertex(n7);
		
		e13 = new Edge(n1, n3, Math.exp(-4));
		e12 = new Edge(n1, n2, Math.exp(-6));
		e23 = new Edge(n2, n3, Math.exp(-2));
		e24 = new Edge(n2, n4, Math.exp(-2));
		e34 = new Edge(n3, n4, Math.exp(-1));
		e35 = new Edge(n3, n5, Math.exp(-2));
		e46 = new Edge(n4, n6, Math.exp(-7));
		e54 = new Edge(n5, n4, Math.exp(-1));
		e56 = new Edge(n5, n6, Math.exp(-3));
		
		graph.addEdge(e13, n1, n3);
		graph.addEdge(e12, n1, n2);
		graph.addEdge(e23, n2, n3);
		graph.addEdge(e24, n2, n4);
		graph.addEdge(e34, n3, n4);
		graph.addEdge(e35, n3, n5);
		graph.addEdge(e46, n4, n6);
		graph.addEdge(e54, n5, n4);
		graph.addEdge(e56, n5, n6);
		
		this.bfAlgo = new BellmanFordAlgorithm<Edge>();
	}
	
	private EdgeCostTransformer<Edge> transformer = new EdgeCostTransformer<Edge>() {

		@Override
		public double getCost(Edge e) {
			return e.getReliabilityCost();
		}
		
	};
	
	public void testBFAlgo() {
		Node src = n6;
		Node dst = n1;
		Set<Node> excludedNodes = new HashSet<Node>();
		List<Node> path = bfAlgo.findShortestPathOfLengthConstraints(excludedNodes, 
				graph, src, dst, 3, transformer);
		System.out.println(path);
		assertEquals(4, path.size());
		path = bfAlgo.findShortestPathOfLengthConstraints(excludedNodes, 
				graph, src, dst, 4, transformer);
		System.out.println(path);
		assertEquals(4, path.size());
		path = bfAlgo.findShortestPathOfLengthConstraints(excludedNodes, 
				graph, src, dst, 2, transformer);
		assertNull(path);
		path = bfAlgo.findShortestPathOfLengthConstraints(excludedNodes, 
				graph, src, dst, 3, transformer);
		excludedNodes.addAll(path.subList(1, path.size()-1));
		path = bfAlgo.findShortestPathOfLengthConstraints(excludedNodes, 
				graph, src, dst, 3, transformer);
		System.out.println(path);
		assertEquals(4, path.size());
		excludedNodes.addAll(path.subList(1, path.size()-1));
		path = bfAlgo.findShortestPathOfLengthConstraints(excludedNodes, 
				graph, src, dst, 3, transformer);
		assertNull(path);
	}
	
	
	
}
