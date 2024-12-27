package de.seemoo.dyuan.test.unit;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import de.seemoo.dyuan.netgen.algo.KDAlgoCallback;
import de.seemoo.dyuan.netgen.algo.KDAlgorithm;
import de.seemoo.dyuan.netgen.algo.KDAlgorithm.KDEdge;
import de.seemoo.dyuan.netgen.structure.Edge;
import de.seemoo.dyuan.netgen.structure.Node;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

public class TestKDAlgo extends TestCase {
	
	private UndirectedSparseGraph<Node, Edge> graph; 
	
	private KDAlgorithm algo;
	
	private Node n1, n2, n3, n4, n5, n6;
	
	private Edge e13, e12, e23, e24, e34, e35, e46, e54, e56;
	
	//the node is not connected to the graph.
	private Node n7; 
	
	private int pathLengthBound;
	
	private int maxPathCount;
	
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
	}
	
	public void testKD10() {
		this.pathLengthBound = 10;
		this.maxPathCount = 100;
		this.algo = new KDAlgorithm(this.graph, n6, n1, this.pathLengthBound, this.maxPathCount);
		this.algo.createDirectedTree();
		this.algo.buildDijkstraTreeAtDest();
		for (Node node : this.graph.getVertices()) {
			double expCost;
			if (node == n1) {
				expCost = 0;
			} else if (node == n2) {
				expCost = 6;
			} else if (node == n3) {
				expCost = 4;
			} else if (node == n4) {
				expCost = 5;
			} else if (node == n5) {
				expCost = 6;
			} else if (node == n6) {
				expCost = 9;
			} else {
				expCost = Double.POSITIVE_INFINITY;
			}
			assertEquals(expCost, node.getDistanceToDest());
		}
		
		//test reduced costs
		this.algo.computeReducedCost();
		
		DirectedSparseGraph<Node, KDAlgorithm.KDEdge> graph = this.algo.getGraph();
		assertEquals(18, graph.getEdgeCount());
		for (KDEdge edge : graph.getEdges()) {
			Node src = graph.getSource(edge);
			Node dst = graph.getDest(edge);
			double expReducedCost = dst.getDistanceToDest() - src.getDistanceToDest() + edge.getReliabilityCost();
			assertEquals(expReducedCost, edge.getReducedCost());
			System.out.println(src.getId()+"->"+dst.getId()+" red-cost: "+edge.getReducedCost());
		}
		
		//test sorted star form
		this.algo.arrangeArcsInSortedStarForm();
		Map<Node, List<KDEdge>> starForm = this.algo.getSortedStarForm();
		assertEquals(7, starForm.keySet().size());
		List<KDEdge> star1 = starForm.get(n1);
		assertEquals(2, star1.size());
		KDEdge e13 = star1.get(0);
		assertEquals(n1, algo.getGraph().getSource(e13));
		assertEquals(n3, algo.getGraph().getDest(e13));
		KDEdge e12 = star1.get(1);
		assertEquals(n1, algo.getGraph().getSource(e12));
		assertEquals(n2, algo.getGraph().getDest(e12));
		
		List<KDEdge> star4 = starForm.get(n4);
		assertEquals(4, star4.size());
		KDEdge e43 = star4.get(0);
		KDEdge e45 = star4.get(1);
		KDEdge e42 = star4.get(2);
		KDEdge e46 = star4.get(3);
		assertEquals(n4, algo.getGraph().getSource(e43));
		assertEquals(n3, algo.getGraph().getDest(e43));
		assertEquals(n4, algo.getGraph().getSource(e45));
		assertEquals(n5, algo.getGraph().getDest(e45));
		assertEquals(n4, algo.getGraph().getSource(e42));
		assertEquals(n2, algo.getGraph().getDest(e42));
		assertEquals(n4, algo.getGraph().getSource(e46));
		assertEquals(n6, algo.getGraph().getDest(e46));
		
		List<KDEdge> star5 = starForm.get(n5);
		assertEquals(3, star5.size());
		KDEdge e53 = star5.get(0);
		KDEdge e54 = star5.get(1);
		KDEdge e56 = star5.get(2);
		assertEquals(n3, algo.getGraph().getDest(e53));
		assertEquals(n4, algo.getGraph().getDest(e54));
		assertEquals(n6, algo.getGraph().getDest(e56));
		
		//test find the shortest path
		this.algo.initCandidatePathSet();
		assertEquals(1, this.algo.getCandidatePaths().size());
		
		Iterator<KDAlgorithm.CandidatePath> iter = this.algo.getCandidatePaths().iterator();
		
		KDAlgorithm.CandidatePath candPath = iter.next();
		List<Node> shortestPath = candPath.path;
		assertEquals(4, shortestPath.size());
		assertEquals(n6, shortestPath.get(0));
		assertEquals(n5, shortestPath.get(1));
		assertEquals(n3, shortestPath.get(2));
		assertEquals(n1, shortestPath.get(3));
		
		//test path cost;
		assertEquals(9.0, candPath.cost);
		
		//test finding all paths.
		KDCallback callback = new KDCallback(this.graph, n6, n1, this.pathLengthBound);
		algo.setCallback(callback);
		algo.enumerateLooplessShortestPath();
		assertEquals(13, callback.getNumberPaths());		
	}
	
	
	
	public void testKD3() {
		this.pathLengthBound = 3;
		this.maxPathCount = 100;
		this.algo = new KDAlgorithm(this.graph, n6, n1, this.pathLengthBound, this.maxPathCount);
		KDCallback callback = new KDCallback(this.graph, n6, n1, this.pathLengthBound);
		algo.setCallback(callback);
		this.algo.perform();	
		assertEquals(3, callback.getNumberPaths());
	}
	
	public void testKD4() {
		this.pathLengthBound = 4;
		this.maxPathCount = 100;
		this.algo = new KDAlgorithm(this.graph, n6, n1, this.pathLengthBound, this.maxPathCount);
		KDCallback callback = new KDCallback(this.graph, n6, n1, this.pathLengthBound);
		this.algo.setCallback(callback);
		this.algo.perform();	
		assertEquals(9, callback.getNumberPaths());
		
		this.pathLengthBound = 4;
		this.maxPathCount = 5;
		this.algo = new KDAlgorithm(this.graph, n6, n1, this.pathLengthBound, this.maxPathCount);
		callback = new KDCallback(this.graph, n6, n1, this.pathLengthBound);
		this.algo.setCallback(callback);
		this.algo.perform();	
		assertEquals(5, callback.getNumberPaths());
	}
	
	public void testKD5() {
		this.pathLengthBound = 5;
		this.maxPathCount = 100;
		this.algo = new KDAlgorithm(this.graph, n6, n1, this.pathLengthBound, this.maxPathCount);
		KDCallback callback = new KDCallback(this.graph, n6, n1, this.pathLengthBound);
		this.algo.setCallback(callback);
		this.algo.perform();	
		assertEquals(13, callback.getNumberPaths());
		
		this.pathLengthBound = 5;
		this.maxPathCount = 10;
		this.algo = new KDAlgorithm(this.graph, n6, n1, this.pathLengthBound, this.maxPathCount);
		callback = new KDCallback(this.graph, n6, n1, this.pathLengthBound);
		this.algo.setCallback(callback);
		this.algo.perform();	
		assertEquals(10, callback.getNumberPaths());
	}
	
	
	private static class KDCallback extends TestCase implements KDAlgoCallback {
		
		private int count;
		
		private double lastReliability;
		
		private Node src, dst;
		
		private int pathLengthBound;
		
		private UndirectedSparseGraph<Node, Edge> graph; 
		
		
		public KDCallback(UndirectedSparseGraph<Node, Edge> graph, Node src, Node dst, int pathLengthBound) {
			this.graph = graph;
			this.src = src;
			this.dst = dst;
			this.pathLengthBound = pathLengthBound;
			this.count = 0;
			this.lastReliability = 1;
			System.out.println("---------------");
		}
		
		@Override
		public void callback(List<Node> path, double pathReliability) {
			HashSet<Node> nodeSet = new HashSet<Node>();
			count++;
			assertEquals(src, path.get(0));
			assertEquals(dst, path.get(path.size()-1));
			assertTrue(path.size()-1 <= pathLengthBound);
			for (int i = 0; i < path.size()-1; i++) {
				Node n1 = path.get(i);
				Node n2 = path.get(i+1);
				Edge edge = graph.findEdge(n1, n2);
				assertNotNull(edge);
				assertTrue(pathReliability <= lastReliability);
				this.lastReliability = pathReliability;
			}
			
			for (int i=0; i<path.size(); i++) {
				if (nodeSet.contains(path.get(i))) {
					fail("path contains cycle!");
				}
			}
			System.out.println(path+" reliability: "+(-Math.log(pathReliability)) );
		}
		
		public int getNumberPaths() {
			return this.count;
		}
		
		
	}
	
	

}
