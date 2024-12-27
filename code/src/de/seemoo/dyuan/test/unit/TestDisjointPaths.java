package de.seemoo.dyuan.test.unit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;
import de.seemoo.dyuan.netgen.algo.DinicAlgorithm;
import de.seemoo.dyuan.netgen.algo.VertexDisjointBoundedPathHeuristic;
import de.seemoo.dyuan.netgen.structure.Edge;
import de.seemoo.dyuan.netgen.structure.Node;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

public class TestDisjointPaths extends TestCase {
	
	private List<List<Node>> paths;
	
	private UndirectedSparseGraph<Node, Edge> graph;
	
	private boolean disjointVertex;
	
	private Node source;
	
	private Node destination;
	
	private void assertPathCountAndLength(int expectedCount, int expectedLen) {
		assertEquals(expectedCount, paths.size());
		if (paths.size() > 0) {
			int len = paths.get(0).size();
			assertEquals(expectedLen, len-1);
			for (int i=1; i<paths.size(); i++) {
				assertEquals(len, paths.get(i).size());
			}
		}
	}
	
	private Edge graphGetEdge(Node n1, Node n2) {
		for (Edge edge : graph.getIncidentEdges(n1)) {
			if (edge.getTheOtherNode(n1) == n2)
				return edge;
		}
		return null;
	}
	
	private void assertDisjointPath() {
		for (List<Node> path : paths) {
			assertSame(path.get(0), source);
			assertSame(path.get(path.size()-1), destination);
		}
		if (disjointVertex) {
			Set<Node> nodeSet = new HashSet<Node>();
			for (List<Node> path : paths) {
				for (int i=1; i<path.size()-1; i++) {
					Node node = path.get(i);
					assertFalse(nodeSet.contains(node));
					nodeSet.add(node);					
				}
			}
		}
		Set<Edge> edgeSet = new HashSet<Edge>();
		for (List<Node> path : paths) {
			for (int i=0; i<path.size()-1; i++) {
				Node n1 = path.get(i);
				Node n2 = path.get(i+1);
				Edge edge = this.graphGetEdge(n1, n2);
				assertNotNull(edge);
				assertFalse(edgeSet.contains(edge));
				edgeSet.add(edge);
			}
		}
	}
	
	private DinicAlgorithm finder;	

	public void testAuxiliaryGraph1() {
		this.graph = new UndirectedSparseGraph<Node, Edge>();
		int n = 10;
		Node[] nodes = new Node[n+1];
		for (int i=1; i<=n; i++) {
			Node node = new Node(i+"");
			graph.addVertex(node);
			nodes[i] = node;
		}
		graph.addEdge(new Edge(nodes[1], nodes[2], 1), nodes[1], nodes[2]);
		graph.addEdge(new Edge(nodes[1], nodes[3], 1), nodes[1], nodes[3]);
		//graph.addEdge(new Object(), nodes[1], nodes[5]);
		graph.addEdge(new Edge(nodes[2], nodes[4], 1), nodes[2], nodes[4]);
		graph.addEdge(new Edge(nodes[3], nodes[5], 1), nodes[3], nodes[5]);
		graph.addEdge(new Edge(nodes[4], nodes[5], 1), nodes[4], nodes[5]);
		graph.addEdge(new Edge(nodes[4], nodes[6], 1), nodes[4], nodes[6]);
		graph.addEdge(new Edge(nodes[4], nodes[7], 1), nodes[4], nodes[7]);
		graph.addEdge(new Edge(nodes[5], nodes[7], 1), nodes[5], nodes[7]);
		graph.addEdge(new Edge(nodes[6], nodes[7], 1), nodes[6], nodes[7]);
		graph.addEdge(new Edge(nodes[6], nodes[8], 1), nodes[6], nodes[8]);
		graph.addEdge(new Edge(nodes[7], nodes[8], 1), nodes[7], nodes[8]);
		
		graph.addEdge(new Edge(nodes[1], nodes[9], 1), nodes[1], nodes[9]);
		graph.addEdge(new Edge(nodes[9], nodes[10], 1), nodes[9], nodes[10]);
		graph.addEdge(new Edge(nodes[10], nodes[8], 1), nodes[10], nodes[8]);
		finder = new DinicAlgorithm(graph);
		this.source = nodes[1];
		this.destination = nodes[7];
		this.disjointVertex = true;
		this.paths = finder.getShortestPaths(source, destination, disjointVertex);
		this.assertPathCountAndLength(2, 3);
		this.assertDisjointPath();
		
		this.disjointVertex = false;
		this.paths = finder.getShortestPaths(source, destination, disjointVertex);
		this.assertPathCountAndLength(2, 3);
		this.assertDisjointPath();	
		
		VertexDisjointBoundedPathHeuristic heurist = new VertexDisjointBoundedPathHeuristic(graph);
		heurist.getMaxBoundedPaths(source, destination, 4);
	}
	
	public void testAuxiliaryGraph2() {
		graph = new UndirectedSparseGraph<Node, Edge>();
		Node[] nodes = new Node[8];
		for (int i=1; i<=7; i++) {
			Node node = new Node(i+"");
			graph.addVertex(node);
			nodes[i] = node;
		}
		graph.addEdge(new Edge(nodes[1], nodes[2], 1), nodes[1], nodes[2]);
		graph.addEdge(new Edge(nodes[1], nodes[3], 1), nodes[1], nodes[3]);
		graph.addEdge(new Edge(nodes[2], nodes[4], 1), nodes[2], nodes[4]);
		graph.addEdge(new Edge(nodes[3], nodes[4], 1), nodes[3], nodes[4]);
		
		graph.addEdge(new Edge(nodes[4], nodes[5], 1), nodes[4], nodes[5]);
		graph.addEdge(new Edge(nodes[4], nodes[6], 1), nodes[4], nodes[6]);
		graph.addEdge(new Edge(nodes[5], nodes[7], 1), nodes[5], nodes[7]);
		graph.addEdge(new Edge(nodes[6], nodes[7], 1), nodes[6], nodes[7]);

		finder = new DinicAlgorithm(graph);
		this.source = nodes[1];
		this.destination = nodes[7];
		this.disjointVertex = true;
		this.paths = finder.getShortestPaths(source, destination, disjointVertex);
		this.assertPathCountAndLength(1, 4);
		this.assertDisjointPath();
		
		this.disjointVertex = false;
		this.paths = finder.getShortestPaths(source, destination, disjointVertex);
		this.assertPathCountAndLength(2, 4);
		this.assertDisjointPath();	
		
	}
	
	public void testAuxiliaryGraph3() {
		graph = new UndirectedSparseGraph<Node, Edge>();
		Node[] nodes = new Node[7];
		for (int i=1; i<=6; i++) {
			Node node = new Node(i+"");
			graph.addVertex(node);
			nodes[i] = node;
		}
		graph.addEdge(new Edge(nodes[1], nodes[2], 1), nodes[1], nodes[2]);
		graph.addEdge(new Edge(nodes[1], nodes[3], 1), nodes[1], nodes[3]);
		graph.addEdge(new Edge(nodes[2], nodes[3], 1), nodes[2], nodes[3]);
		graph.addEdge(new Edge(nodes[2], nodes[5], 1), nodes[2], nodes[5]);
		graph.addEdge(new Edge(nodes[3], nodes[5], 1), nodes[3], nodes[5]);
		graph.addEdge(new Edge(nodes[2], nodes[4], 1), nodes[2], nodes[4]);
		graph.addEdge(new Edge(nodes[5], nodes[4], 1), nodes[5], nodes[4]);
		graph.addEdge(new Edge(nodes[4], nodes[6], 1), nodes[4], nodes[6]);
		graph.addEdge(new Edge(nodes[5], nodes[6], 1), nodes[5], nodes[6]);
		
		finder = new DinicAlgorithm(graph);
		this.source = nodes[1];
		this.destination = nodes[6];
		this.disjointVertex = true;
		this.paths = finder.getShortestPaths(source, destination, disjointVertex);
		this.assertPathCountAndLength(2, 3);
		this.assertDisjointPath();
		
		this.disjointVertex = false;
		this.paths = finder.getShortestPaths(source, destination, disjointVertex);
		this.assertPathCountAndLength(2, 3);
		this.assertDisjointPath();	
		
	}
	
	public void testFullConnectedGraph() {
		graph = new UndirectedSparseGraph<Node, Edge>();
		int numNodes = 10;
		Node[] nodes = new Node[numNodes];
		for (int i=0; i<numNodes; i++) {
			Node node = new Node(i+"");
			nodes[i] = node;
		}
		for (int i=0; i<numNodes; i++) {
			for (int j=i+1; j<numNodes; j++) {
				graph.addEdge(new Edge(nodes[i], nodes[j], 1), nodes[i], nodes[j]);
			}
		}
		this.source = nodes[0];
		this.destination = nodes[numNodes-1];
		VertexDisjointBoundedPathHeuristic heurist = new VertexDisjointBoundedPathHeuristic(graph);
		heurist.getMaxBoundedPaths(source, destination, 1);
	}
	
	public void testSimpleGraphMaxBoundedPaths() {
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
		
		this.source = a;
		this.destination = z;
		VertexDisjointBoundedPathHeuristic heurist = new VertexDisjointBoundedPathHeuristic(graph);
		List<List<Node>> paths = heurist.getMaxBoundedPaths(source, destination, 1);
		assertEquals(1, paths.size());
		assertEquals(2, paths.get(0).size());
		
		paths = heurist.getMaxBoundedPaths(source, destination, 2);
		assertEquals(2, paths.size());
		ArrayList<Integer> lens = new ArrayList<Integer>();
		for (List<Node> path : paths) {
			lens.add(path.size());
		}
		Collections.sort(lens);
		assertEquals(2, paths.get(0).size());
		assertEquals(3, paths.get(1).size());
		
		paths = heurist.getMaxBoundedPaths(source, destination, 4);
		assertEquals(2, paths.size());
		lens = new ArrayList<Integer>();
		for (List<Node> path : paths) {
			lens.add(path.size());
		}
		Collections.sort(lens);
		assertEquals(2, paths.get(0).size());
		assertEquals(3, paths.get(1).size());
	}
	
//	public BasicVisualizationServer<Node, EdgeInfo> getGraphicComponent(final DirectedSparseGraph<Node, EdgeInfo> graph) {
//		Layout<Node, EdgeInfo> layout = new FRLayout<Node, EdgeInfo>(graph);
//
//		layout.setSize(new Dimension(500, 500));
//		BasicVisualizationServer<Node, EdgeInfo> server = new BasicVisualizationServer<Node, EdgeInfo>(layout);
//		server.setPreferredSize(new Dimension(500, 500));
//		server.getRenderContext().setVertexLabelTransformer(new Transformer<Node, String>() {
//
//			@Override
//			public String transform(Node node) {
//				return node.getId();
//			}
//		});
//		Transformer<Node, Font> fontTransformer = new Transformer<Node, Font>() {
//
//			@Override
//			public Font transform(Node node) {
//				return new Font("Arial", Font.PLAIN, 15);
//			}
//			
//		};
//		Transformer<Node, Shape> shapeTransformer = new Transformer<Node, Shape>() {
//
//			@Override
//			public Shape transform(Node node) {
//				return new Ellipse2D.Float(-5f, -5f, 10.0f, 10.0f);
//			}
//			
//		};
//		server.getRenderContext().setVertexShapeTransformer(shapeTransformer);
//		server.getRenderContext().setVertexFontTransformer(fontTransformer);
//		server.getRenderer().getVertexLabelRenderer().setPosition(Position.AUTO);
//		
//		Transformer<Node, Paint> vertexPaint = new Transformer<Node, Paint>() {
//
//			@Override
//			public Paint transform(Node node) {
//				if (node.getType() == Node.GATEWAY)
//					return Color.RED;
//				if (node.getType() == Node.NORMAL) {
//					if (node.isSourceOrDest())
//						return Color.MAGENTA;
//					return Color.GREEN;
//				}
//				return Color.YELLOW;
//			}
//			
//		};
//		server.getRenderContext().setVertexFillPaintTransformer(vertexPaint);
//		server.getRenderContext().setEdgeLabelTransformer(new Transformer<ShortestDisjointPathFinder.EdgeInfo, String>() {
//
//			@Override
//			public String transform(EdgeInfo edge) {
//				Pair<Node> pair = graph.getEndpoints(edge);
//				int posCap = finder.getCapacity(pair.getFirst(), pair.getSecond());
//				int negCap = finder.getCapacity(pair.getSecond(), pair.getFirst());
//				return finder.getLevel(pair.getFirst())+"/"+finder.getLevel(pair.getSecond())
//						+" ("+posCap+","+negCap+")";
//			}
//			
//		});
//		return server;
//	}
}
