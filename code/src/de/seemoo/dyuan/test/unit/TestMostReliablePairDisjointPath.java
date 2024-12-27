package de.seemoo.dyuan.test.unit;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.seemoo.dyuan.netgen.algo.DisjointPairPath;
import de.seemoo.dyuan.netgen.algo.KDAlgoCallback;
import de.seemoo.dyuan.netgen.algo.KDAlgorithm;
import de.seemoo.dyuan.netgen.algo.KDAlgorithm.KDEdge;
import de.seemoo.dyuan.netgen.algo.MostReliablePairDisjointPath;
import de.seemoo.dyuan.netgen.structure.Edge;
import de.seemoo.dyuan.netgen.structure.Node;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import junit.framework.TestCase;

public class TestMostReliablePairDisjointPath extends TestCase {

private UndirectedSparseGraph<Node, Edge> graph; 
	
	private MostReliablePairDisjointPath algo;
	
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
	
	public void testDisjointPair() {
		this.algo = new MostReliablePairDisjointPath(graph, n6, n1, 5);
		DisjointPairPath pair = this.algo.findMostReliablePair();
		List<Node> p1 = pair.getPath1();
		List<Node> p2 = pair.getPath2();
		assertNotNull(p1);
		assertNotNull(p2);
		assertEquals(4, p1.size());
		assertEquals(4, p2.size());
		assertEquals(n6, p1.get(0));
		assertEquals(n5, p1.get(1));
		assertEquals(n3, p1.get(2));
		assertEquals(n1, p1.get(3));
		assertEquals(n6, p2.get(0));
		assertEquals(n4, p2.get(1));
		assertEquals(n2, p2.get(2));
		assertEquals(n1, p2.get(3));
		assertEquals(Math.exp(-9)+(1-Math.exp(-9)) * Math.exp(-15), pair.getReliability());
	}
	
	public void testDisjointPair2() {
		this.algo = new MostReliablePairDisjointPath(graph, n6, n1, 3);
		DisjointPairPath pair = this.algo.findMostReliablePair();
		List<Node> p1 = pair.getPath1();
		List<Node> p2 = pair.getPath2();
		assertNotNull(p1);
		assertNotNull(p2);
		assertEquals(4, p1.size());
		assertEquals(4, p2.size());
		assertEquals(n6, p1.get(0));
		assertEquals(n5, p1.get(1));
		assertEquals(n3, p1.get(2));
		assertEquals(n1, p1.get(3));
		assertEquals(n6, p2.get(0));
		assertEquals(n4, p2.get(1));
		assertEquals(n2, p2.get(2));
		assertEquals(n1, p2.get(3));
		assertEquals(Math.exp(-9)+(1-Math.exp(-9)) * Math.exp(-15), pair.getReliability());
	}
	
	public void testDisjointPair3() {
		this.algo = new MostReliablePairDisjointPath(graph, n6, n1, 2);
		DisjointPairPath pair = this.algo.findMostReliablePair();
		assertNull(pair);
	}
}
