package de.seemoo.dyuan.test.unit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import de.seemoo.dyuan.netgen.algo.BhandariAlgorithm;
import de.seemoo.dyuan.netgen.algo.BhandariAlgorithm.EdgeWeightTransformer;
import de.seemoo.dyuan.netgen.structure.Edge;
import de.seemoo.dyuan.netgen.structure.Node;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

public class TestBhandariAlgorithm extends TestCase implements EdgeWeightTransformer {
	
	private UndirectedSparseGraph<Node, Edge> graph;
	
	private Map<Edge, Integer> weightTable;
	
	private Node source;
	
	private Node destination;
	
	public void testBhandariExample() {
		graph = new UndirectedSparseGraph<Node, Edge>();
		Node a = new Node("A");
		Node b = new Node("B");
		Node c = new Node("C");
		Node d = new Node("D");
		Node e = new Node("E");
		Node f = new Node("F");
		Node g = new Node("G");
		Node z = new Node("Z");
		graph.addVertex(a);
		graph.addVertex(b);
		graph.addVertex(c);
		graph.addVertex(d);
		graph.addVertex(e);
		graph.addVertex(f);
		graph.addVertex(g);
		graph.addVertex(z);
		this.weightTable = new HashMap<Edge, Integer>();
		Edge ab = new Edge(a, b, 1);
		weightTable.put(ab, 1);
		graph.addEdge(ab, a, b);
		
		Edge ae = new Edge(a, e, 1);
		weightTable.put(ae, 1);
		graph.addEdge(ae, a, e);
		
		Edge ag = new Edge(a, g, 1);
		weightTable.put(ag, 7);
		graph.addEdge(ag, a, g);
		
		Edge bc = new Edge(b, c, 1);
		weightTable.put(bc, 1);
		graph.addEdge(bc, b, c);
		
		Edge be = new Edge(b, e, 1);
		weightTable.put(be, 1);
		graph.addEdge(be, b, e);
		
		Edge bf = new Edge(b, f, 1);
		weightTable.put(bf, 1);
		graph.addEdge(bf, b, f);
		
		Edge cd = new Edge(c, d, 1);
		weightTable.put(cd, 1);
		graph.addEdge(cd, c, d);
		
		Edge cg = new Edge(c, g, 1);
		weightTable.put(cg, 1);
		graph.addEdge(cg, c, g);
		
		Edge df = new Edge(d, f, 1);
		weightTable.put(df, 1);
		graph.addEdge(df, d, f);
		
		Edge dz = new Edge(d, z, 1);
		weightTable.put(dz, 1);
		graph.addEdge(dz, d, z);
		
		Edge ef = new Edge(e, f, 1);
		weightTable.put(ef, 3);
		graph.addEdge(ef, e, f);
		
		Edge fz = new Edge(f, z, 1);
		weightTable.put(fz, 4);
		graph.addEdge(fz, f, z);
		
		Edge gz = new Edge(g, z, 1);
		weightTable.put(gz, 2);
		graph.addEdge(gz, g, z);
		
		
		BhandariAlgorithm ba = new BhandariAlgorithm(graph, this);
		this.source = a;
		this.destination = z;
		List<List<Node>> paths = ba.getPathsOfSmallestSumCost(source, destination, true, 2);
		assertEquals(2, paths.size());
		assertEquals(11.0, ba.getSumPathCost(paths));
		
		paths = ba.getPathsOfSmallestSumCost(source, destination, false, 2);
		assertEquals(2, paths.size());
		assertEquals(10.0, ba.getSumPathCost(paths));
		
		System.out.println("=============================");
		paths = ba.getPathsOfSmallestSumCost(source, destination, true, 3);
		System.out.println(ba.getSumPathCost(paths));
		assertEquals(3, paths.size());
		assertEquals(21.0, ba.getSumPathCost(paths));
		
		System.out.println("=============================");
		paths = ba.getPathsOfSmallestSumCost(source, destination, false, 3);
		System.out.println(ba.getSumPathCost(paths));
		assertEquals(3, paths.size());
		assertEquals(20.0, ba.getSumPathCost(paths));
	
	}
	
	public void testSimpleGraph() {
		Node a = new Node("A");
		Node b = new Node("B");
		Node c = new Node("C");
		Node d = new Node("D");
		Node z = new Node("Z");
		
		Edge ab = new Edge(a, b, 1);
		Edge ac = new Edge(a, c, 1);
		Edge bc = new Edge(b, c, 1);
		Edge cd = new Edge(c, d, 1);
		Edge dz = new Edge(d, z, 1);
		Edge cz = new Edge(c, z, 1);
		Edge az = new Edge(a, z, 1);
		
		this.source = a;
		this.destination = z;
		
		graph = new UndirectedSparseGraph<Node, Edge>();
		graph.addVertex(a);
		graph.addVertex(b);
		graph.addVertex(c);
		graph.addVertex(d);
		graph.addVertex(z);
		
		graph.addEdge(ab, a, b);
		graph.addEdge(bc, b, c);
		graph.addEdge(cz, c, z);
		graph.addEdge(az, a, z);
		graph.addEdge(ac, a, c);
		graph.addEdge(cd, c, d);
		graph.addEdge(dz, d, z);
		
		BhandariAlgorithm ba = new BhandariAlgorithm(graph, null);		
		List<List<Node>> paths = ba.getPathsOfSmallestSumCost(source, destination, true, 3);
		assertEquals(2, paths.size());
		assertEquals(3.0, ba.getSumPathCost(paths));
		
		System.out.println("======================");
		paths = ba.getPathsOfSmallestSumCost(source, destination, false, 3);
		assertEquals(3, paths.size());
		assertEquals(7.0, ba.getSumPathCost(paths));
	}
	
	public void testExampleInSuurballePaper() {
		graph = new UndirectedSparseGraph<Node, Edge>();
		Node a = new Node("A");
		Node b = new Node("B");
		Node c = new Node("C");
		Node d = new Node("D");
		Node e = new Node("E");
		Node f = new Node("F");
		Node g = new Node("G");
		Node z = new Node("Z");
		graph.addVertex(a);
		graph.addVertex(b);
		graph.addVertex(c);
		graph.addVertex(d);
		graph.addVertex(e);
		graph.addVertex(f);
		graph.addVertex(g);
		graph.addVertex(z);
		
		this.weightTable = new HashMap<Edge, Integer>();
		Edge ab = new Edge(a, b, 1);
		this.weightTable.put(ab, 1);
		graph.addEdge(ab, a, b);
		
		Edge bc = new Edge(b, c, 1);
		this.weightTable.put(bc, 1);
		graph.addEdge(bc, b, c);
		
		Edge cd = new Edge(c, d, 1);
		this.weightTable.put(cd, 1);
		graph.addEdge(cd, c, d);
		
		Edge de = new Edge(d, e, 1);
		this.weightTable.put(de, 1);
		graph.addEdge(de, d, e);
		
		Edge ez = new Edge(e, z, 1);
		this.weightTable.put(ez, 1);
		graph.addEdge(ez, e, z);
		
		Edge zg = new Edge(z, g, 1);
		this.weightTable.put(zg, 1);
		graph.addEdge(zg, z, g);
		
		Edge gf = new Edge(g, f, 1);
		this.weightTable.put(gf, 6);
		graph.addEdge(gf, g, f);
		
		Edge fa = new Edge(f, a, 1);
		this.weightTable.put(fa, 1);
		graph.addEdge(fa, f, a);
		
		Edge bz = new Edge(b, z, 1);
		this.weightTable.put(bz, 8);
		graph.addEdge(bz, b, z);
		
		Edge ae = new Edge(a, e, 1);
		this.weightTable.put(ae, 8);
		graph.addEdge(ae, a, e);
		
		Edge cf = new Edge(c, f, 1);
		this.weightTable.put(cf, 2);
		graph.addEdge(cf, c, f);
		
		Edge dg = new Edge(d, g, 1);
		this.weightTable.put(dg, 2);
		graph.addEdge(dg, d, g);
		
		this.source = a;
		this.destination = z;
		
		BhandariAlgorithm ba = new BhandariAlgorithm(graph, this);		
		List<List<Node>> paths = ba.getPathsOfSmallestSumCost(source, destination, true, 3);
		assertEquals(3, paths.size());
		assertEquals(25.0, ba.getSumPathCost(paths));
	
	}

	@Override
	public double transform(Edge edge) {
		return this.weightTable.get(edge);
	}

}
