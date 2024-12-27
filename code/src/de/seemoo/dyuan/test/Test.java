package de.seemoo.dyuan.test;

import java.util.Map;

import org.apache.commons.collections15.Transformer;


import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.util.Pair;


public class Test {


	/**
	 * Test Dijkstra shortest path tree.
	 */
	public static void main(String[] args) {
		UndirectedSparseGraph<Node, Edge> graph = new UndirectedSparseGraph<Node, Edge>(); 
		Node n1 = new Node(1);
		Node n2 = new Node(2);
		Node n3 = new Node(3);
		Node n4 = new Node(4);
		Node n5 = new Node(5);
		Node n6 = new Node(6);
		
		graph.addVertex(n1);
		graph.addVertex(n2);
		graph.addVertex(n3);
		graph.addVertex(n4);
		graph.addVertex(n5);
		graph.addVertex(n6);
		
		Edge e13 = new Edge(4);
		Edge e12 = new Edge(6);
		Edge e23 = new Edge(2);
		Edge e24 = new Edge(2);
		Edge e34 = new Edge(1);
		Edge e35 = new Edge(2);
		Edge e46 = new Edge(7);
		Edge e54 = new Edge(1);
		Edge e56 = new Edge(3);
		
		graph.addEdge(e13, n1, n3);
		graph.addEdge(e12, n1, n2);
		graph.addEdge(e23, n2, n3);
		graph.addEdge(e24, n2, n4);
		graph.addEdge(e34, n3, n4);
		graph.addEdge(e35, n3, n5);
		graph.addEdge(e46, n4, n6);
		graph.addEdge(e54, n5, n4);
		graph.addEdge(e56, n5, n6);
		
		Transformer<Edge, Double> costTrans = new Transformer<Edge, Double>() {

			@Override
			public Double transform(Edge edge) {
				return edge.getCost();
			}
			
		};
		DijkstraShortestPath<Node, Edge> pathFinder = new DijkstraShortestPath<Node, Edge>(graph, costTrans); 
		Map<Node, Edge> edgeMap = pathFinder.getIncomingEdgeMap(n1);
		for (Map.Entry<Node, Edge> entry : edgeMap.entrySet()) {
			Node node = entry.getKey();
			Edge edge = entry.getValue();
			if (edge == null) {
				node.setCostToSource(0);
			} else {
				Pair<Node> nodePair = graph.getEndpoints(edge);
				Node otherNode;
				if (nodePair.getFirst() == node) {
					otherNode = nodePair.getSecond();
				} else {
					otherNode = nodePair.getFirst();
				}
				node.setCostToSource(otherNode.getCostToSource()+edge.getCost());
			}
			System.out.println(node.getId()+" cost: "+node.getCostToSource());
		}
		System.out.println("----------------------");
		
		Map<Node, Number> distMap = pathFinder.getDistanceMap(n1);
		for (Map.Entry<Node, Number> entry : distMap.entrySet()) {
			Node node = entry.getKey();
			Number cost = entry.getValue();
			System.out.println(node.getId()+" cost: "+cost);
		}
		System.out.println("---------------------");
		
		System.out.println(Double.POSITIVE_INFINITY < Double.POSITIVE_INFINITY);
		System.out.println(Double.POSITIVE_INFINITY == Double.POSITIVE_INFINITY);
		System.out.println(Double.POSITIVE_INFINITY >  Double.POSITIVE_INFINITY);
		System.out.println(Double.POSITIVE_INFINITY < 100);
		System.out.println(Double.POSITIVE_INFINITY == 100);
		System.out.println(Double.POSITIVE_INFINITY > 100);
		
		System.err.println(Double.MAX_VALUE > Double.MAX_VALUE);
	}

}

class Node {
	private final int id;
	
	private double costToSrc;
	
	public Node(int id) {
		this.id = id;
	}
	
	public int getId() {
		return this.id;
	}
	
	public void setCostToSource(double cost) {
		this.costToSrc = cost;
	}
	
	public double getCostToSource() {
		return this.costToSrc;
	}
}

class Edge {
	private final double cost;
	
	public Edge(double cost) {
		this.cost = cost;
	}
	
	public double getCost() {
		return this.cost;
	}
}
