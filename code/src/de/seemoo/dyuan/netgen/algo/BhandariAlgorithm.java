package de.seemoo.dyuan.netgen.algo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.seemoo.dyuan.netgen.structure.Edge;
import de.seemoo.dyuan.netgen.structure.Node;
import de.seemoo.dyuan.netgen.structure.TempNode;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;

/**
 * An algorithm that finds K >= 2 number of vertex/edge disjoint paths with the minimum 
 * sum cost. The weight of edges must be nonnegative.
 * The same functionality can also be achieved by the Suurballe algorithm, but the Bhandari algorithm is 
 * conceptually much easier.
 * 
 * For Suurballe algorithm. c.f. "Disjoint paths in a Networks", Networks, 4(1974) 125-145
 * and "A Quick Method for Finding Shortest Pairs of Disjoint Paths", Networks, 14(1984) 325-336
 * 
 * For BhandariAlgorithm, c.f. "Optimal Physical Diversity Algorithms and Survivable Networks". 
 * 
 * Note this implementation is done on a undirected graph
 * 
 * @author dyuan
 *
 */
public class BhandariAlgorithm {
	
	public static interface EdgeWeightTransformer {
		public double transform(Edge edge);
	}
	
	/**
	 * If sender != null, directional edge.
	 * if sender == null, bidirectional edge.
	 *
	 */
	public static class EdgeStruct {
		public Node sender;
		public double weight;
		public EdgeStruct(Node sender, double weight) {
			this.sender = sender;
			this.weight = weight;
		}
		public String toString() {
			return "\n"+sender+" "+weight;
		}
	}
	
	private UndirectedSparseGraph<Node, Edge> originalGraph;
	
	private Node source;
	
	private Node destination;
	
	private boolean vertexDisjoint;
	
	private UndirectedSparseGraph<Node, EdgeStruct> graph;
	
	private EdgeWeightTransformer weightTransformer;
	
	private Set<Node> scanSet;
	
	private Map<Node, NodeInfo> nodesMap; 
	
	private List<List<Node>> paths;	
	

	public BhandariAlgorithm(UndirectedSparseGraph<Node, Edge> graph, EdgeWeightTransformer weightTransformer) {
		this.originalGraph = graph;
		this.weightTransformer = weightTransformer;
	}
	
	/**
	 * Return K paths of the shortest sum length
	 * @param source the source node
	 * @param destination the destination node
	 * @param vertexDisjoint whether vertex disjoint or edge disjoint.
	 * @param numPath number of paths.
	 * @return no more than numPath paths.
	 */
	public List<List<Node>> getPathsOfSmallestSumCost(Node source, Node destination, boolean vertexDisjoint,
			int numPaths) { 
		this.source = source;
		this.destination = destination;
		this.vertexDisjoint = vertexDisjoint;
		this.paths = new ArrayList<List<Node>>();
		int num = 0;
		while (num < numPaths) {
			initCopyGraph();
			boolean hasPath = modifiedDijkstra();
			if (!hasPath)
				break;
			num++;
		}
//		for (List<Node> path : this.paths) {
//			System.out.println(path);
//		}
		//checkPathVertexDisjoint();
		return this.paths;
	}
	
	private void checkPathVertexDisjoint() {
		HashSet<Node> set = new HashSet<Node>();
		for (List<Node> path : this.paths) {
			if (path.get(0) != this.source || path.get(path.size()-1) != this.destination) {
				throw new IllegalArgumentException("source or destination node wrong!");
			}
			for (int i = 0; i<path.size()-1; i++) {
				Node node1 = path.get(i);
				Node node2 = path.get(i+1);
				Edge edge = this.originalGraph.findEdge(node1, node2);
				if (edge == null) {
					throw new IllegalArgumentException("the edge should be exisiting");
				}
				if (i != 0) {
					if (set.contains(node1)) {
						throw new IllegalArgumentException("not vertex disjoint");
					}
					set.add(node1);
				}
			}
		}
	}

	public double getSumPathCost(List<List<Node>> paths) {
		double sum = 0;
		if (paths != null) {
			for (List<Node> path : paths) {
				for (int i=0; i<path.size()-1; i++) {
					Node current = path.get(i);
					Node next = path.get(i+1);
					Edge edge = this.originalGraph.findEdge(current, next);
					if (this.weightTransformer == null)
						sum += 1;
					else
						sum += this.weightTransformer.transform(edge);
				}
			}
		}
		return sum;
	}
	
	private void initCopyGraph() {
		this.graph = new UndirectedSparseGraph<Node, EdgeStruct>();
		for (Node node : this.originalGraph.getVertices()) {
			this.graph.addVertex(node);
		}
		for (Edge edge : this.originalGraph.getEdges()) {
			Pair<Node> ends = this.originalGraph.getEndpoints(edge);
			double weight = 1;
			if (this.weightTransformer != null) {
				weight = this.weightTransformer.transform(edge);
			}
			EdgeStruct ue = new EdgeStruct(null, weight);
			this.graph.addEdge(ue, ends, EdgeType.UNDIRECTED);
		}
		if (this.vertexDisjoint) {
			prepareNodeSplitGraph();
		} else {
			invertPathEdges();
		}
		
	}

	private void invertPathEdges() {
		for (List<Node> path : this.paths) {
			for (int i=0; i<path.size()-1; i++) {
				Node current = path.get(i);
				Node next = path.get(i+1);
				EdgeStruct es = this.graph.findEdge(current, next);
				es.weight = -es.weight;
				es.sender = next;
			}
		}
	}

	private void prepareNodeSplitGraph() {
		for (List<Node> path : this.paths) {
			preparePathNodeSplitGraph(path);
		}
	}

	private void preparePathNodeSplitGraph(List<Node> path) {
		TempNode oldtmp2;
		TempNode tmp1 = null;
		TempNode tmp2 = null;
		
		/**
		 * There is a path of direct connection between source and destination.
		 */
		if (path.size() == 2) {
			EdgeStruct es = graph.findEdge(this.source, this.destination);
			es.sender = this.destination;
			es.weight = -es.weight;
			return;
		}
		
		for (int i=1; i < path.size()-1; i++) {			
			Node current = path.get(i);
			Node pred = path.get(i-1);
			Node next = path.get(i+1);
			//split a node.
			oldtmp2 = tmp2;
			tmp1 = new TempNode(current, true);
			tmp2 = new TempNode(current, false);
			graph.addVertex(tmp1);
			graph.addVertex(tmp2);
			//add 0 edge. tmp2->tmp1
			graph.addEdge(new EdgeStruct(tmp2, 0.0), tmp2, tmp1);
			for (Node neighbour : this.graph.getNeighbors(current)) {
				if (neighbour == pred) {
					EdgeStruct struct = this.graph.findEdge(current, neighbour);
					//the edge connected with the 
					if (pred == source)
						graph.addEdge(new EdgeStruct(tmp1, -struct.weight), tmp1, pred);
					else
						graph.addEdge(new EdgeStruct(tmp1, -struct.weight), tmp1, oldtmp2);
				} else if (neighbour != next) {
					//the connection with the next hop is proposed when the next node becomes the current.
					EdgeStruct struct = this.graph.findEdge(current, neighbour);
					if (struct.sender == null) {
						//undirected edge connected with a neighbour.
						graph.addEdge(new EdgeStruct(neighbour, struct.weight), neighbour, tmp1);
						graph.addEdge(new EdgeStruct(tmp2, struct.weight), tmp2, neighbour);
					} else if (struct.sender == current) {
						//outgoing link to a split node.
						graph.addEdge(new EdgeStruct(tmp2, struct.weight), tmp2, neighbour);								
					} else {
						//incoming link from a split node.
						graph.addEdge(new EdgeStruct(neighbour, struct.weight), neighbour, tmp1);
					}
				} else {
					//neighbour == next.
					if (i == path.size()-2) {
					//the last split node on the path.
						EdgeStruct struct = this.graph.findEdge(current, destination);
						graph.addEdge(new EdgeStruct(destination, -struct.weight), destination, tmp2);
					}
				}
			}
		}
		//remove nodes on the original path.
		for (int i=1; i<path.size()-1; i++) {
			Node backNode = path.get(i);
			graph.removeVertex(backNode);
		}
	}
	
	public static class NodeInfo {
		public Node pred;
		public double distance;
	}
	
	private Node getAnotherNodeOfEdge(EdgeStruct edge, Node node) {
		Pair<Node> nodePair = this.graph.getEndpoints(edge);
		if (nodePair.getFirst() == node)
			return nodePair.getSecond();
		if (nodePair.getSecond() == node)
			return nodePair.getFirst();
		throw new IllegalArgumentException("Node "+node+" is not in the graph!");
	}
	
	private Node findNodeInScanSetWithMinDistance() {
		double distance = Double.MAX_VALUE;
		Node minDistNode = null;
		for (Node node : this.scanSet) {
			NodeInfo nodeInfo = this.nodesMap.get(node);
			if (nodeInfo.distance < distance) {
				distance = nodeInfo.distance;
				minDistNode = node;
			}
		}
		return minDistNode;
	}

	
	/**
	 * @return true. found a new path.
	 */
	private boolean modifiedDijkstra() {
		scanSet = new HashSet<Node>();
		nodesMap = new HashMap<Node, NodeInfo>();
		NodeInfo sourceInfo = new NodeInfo();
		sourceInfo.distance = 0;
		nodesMap.put(this.source, sourceInfo);
		for (EdgeStruct edge : this.graph.getIncidentEdges(this.source)) {
			if (edge.sender == null || edge.sender == this.source) {
				Node other = this.getAnotherNodeOfEdge(edge, this.source);
				NodeInfo nodeInfo = new NodeInfo();
				nodeInfo.pred = this.source;
				nodeInfo.distance = edge.weight;
				nodesMap.put(other, nodeInfo);
				scanSet.add(other);
			}
		}
		while (!scanSet.isEmpty()) {
			Node minDistNode = this.findNodeInScanSetWithMinDistance();
			double minDist = this.nodesMap.get(minDistNode).distance;
			this.scanSet.remove(minDistNode);
			if (minDistNode == this.destination) {
				//we have found a path.
				buildPath();
				return true;
			} 
			for (EdgeStruct edge : this.graph.getIncidentEdges(minDistNode)) {
				if (edge.sender == null || edge.sender == minDistNode) {
					Node other = this.getAnotherNodeOfEdge(edge, minDistNode);
					NodeInfo nodeInfo = this.nodesMap.get(other);
					//look at here. take case there might be floating point arithmetic error.
					if (nodeInfo == null || nodeInfo.distance > minDist + edge.weight) {
						if (nodeInfo == null) {
							nodeInfo = new NodeInfo();
						}
						nodeInfo.pred = minDistNode;
						nodeInfo.distance = minDist + edge.weight;
						this.nodesMap.put(other, nodeInfo);
						this.scanSet.add(other);
					}
				}
			}
		}
		return false;
	}

	//problematic
	private void buildPath() {
		List<Node> path = new ArrayList<Node>();
		Node node = this.destination;
		//System.err.println("before");
//		while (node != source) {
//			System.err.print(node+" ");
//			NodeInfo nodeInfo = this.nodesMap.get(node);
//			node = nodeInfo.pred;
//		}
//		System.err.println();
//		node = this.destination;
		while (node != source) {
			NodeInfo nodeInfo = this.nodesMap.get(node);
			if (node instanceof TempNode) {
				node = ((TempNode)node).getBackNode();
			} 
			path.add(0, node);
			Node pred = nodeInfo.pred;
			if (pred instanceof TempNode) {
				TempNode tNode = (TempNode) pred;
				if (tNode.getBackNode() != node) {
					node = pred;
				} else {
					//else ignore the tempNode.
					node = this.nodesMap.get(pred).pred;
				}
			} else {
				node = pred;
			}
			
		}
//		System.err.println("after");
		path.add(0, this.source);
		if (this.vertexDisjoint)
			mergeIntoCurrentPathVertexDisjoint(path);
		else {
			mergeIntoCurrentPathEdgeDisjoint(path);
		}
	}
	
	private List<Node> createAPath(List<Node> firstHops, Map<Node, Node> links) {
		if (firstHops.isEmpty() || links.isEmpty())
			return null;
		List<Node> path = new ArrayList<Node>();
		path.add(this.source);
		Node node = firstHops.remove(0);
		while (node != destination) {
			path.add(node);
			node = links.remove(node);
			if (node == null)
				throw new IllegalStateException();
		}
		path.add(destination);
		return path;
	}
	
	private void mergeIntoCurrentPathEdgeDisjoint(List<Node> path) {
		this.paths.add(path);
		List<Node> firstHops = new ArrayList<Node>();
		Set<Pair<Node>> nodePairs = new HashSet<Pair<Node>>();
		List<Node> directPath = null;
		for (List<Node> aPath : this.paths) {
			if (aPath.size() == 2) {
				directPath = aPath;
				continue;
			}
			for (int i=0; i<aPath.size()-1; i++) {
				Node current = aPath.get(i);
				Node next = aPath.get(i+1);
				if (i == 0) {
					firstHops.add(next);
				} else {
					Pair<Node> pair = new Pair<Node>(current, next);
					if (nodePairs.contains(pair))
						throw new IllegalStateException();
					Pair<Node> reversePair = new Pair<Node>(next, current);
					if (nodePairs.contains(reversePair)) {
						nodePairs.remove(reversePair);
					} else {
						nodePairs.add(pair);	
					}
				}
			}
		}
		this.paths = new ArrayList<List<Node>>();
		if (directPath != null) {
			this.paths.add(directPath);
		}
		List<Node> newPath;
		while ((newPath = this.createAPath(firstHops, nodePairs)) != null) {
			//System.err.println("path: "+newPath);
			this.paths.add(newPath);
		}
	}

	private List<Node> createAPath(List<Node> firstHops,
			Set<Pair<Node>> links) {
		if (firstHops.isEmpty() || links.isEmpty())
			return null;
		List<Node> path = new ArrayList<Node>();
		path.add(this.source);
		Node node = firstHops.remove(0);
		while (node != destination) {
			path.add(node);
			Iterator<Pair<Node>> iter = links.iterator();
			Node next = null;
			while (iter.hasNext()) {
				Pair<Node> pair = iter.next();
				if (pair.getFirst() == node) {
					next = pair.getSecond();
					iter.remove();
					break;
				}
			}
			if (next == null)
				throw new IllegalStateException();
			node = next;
		}
		path.add(destination);
		
		return path;
	}

	private void mergeIntoCurrentPathVertexDisjoint(List<Node> path) {
		this.paths.add(path);
//		for (List<Node> p : paths) {
//			System.err.println(p);
//		}			
		List<Node> firstHops = new ArrayList<Node>();
		Map<Node, Node> links = new HashMap<Node, Node>();
		List<Node> directPath = null;
		for (List<Node> oldPath : this.paths) {
			if (oldPath.size() == 2) {
				directPath = oldPath;
				continue;
			}
			for (int i=1; i<oldPath.size()-1; i++) {
				Node sender = oldPath.get(i);
				if (i == 1) {
					firstHops.add(sender);
				} 
				Node receiver = oldPath.get(i+1);
				Node value = links.get(receiver);
				if (value == null) {
					if (links.get(sender) != null)
						throw new IllegalStateException();
					links.put(sender, receiver);
				}
				else {
					if (value == sender) {
						links.remove(receiver);
					} else {
						if (links.get(sender) != null)
							throw new IllegalStateException();
						links.put(sender, receiver);
					}
					
				}
			}
		}
		this.paths = new ArrayList<List<Node>>();
		if (directPath != null)
			this.paths.add(directPath);
		List<Node> newPath;
		while ((newPath = this.createAPath(firstHops, links)) != null) {
			//System.err.println("path: "+newPath);
			this.paths.add(newPath);
		}
		
	}
}
