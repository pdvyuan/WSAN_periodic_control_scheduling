package de.seemoo.dyuan.netgen.algo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.seemoo.dyuan.netgen.structure.Edge;
import de.seemoo.dyuan.netgen.structure.Node;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.util.Pair;

/**
 * 
 * Find the maximum number of shortest vertex/edge disjoint paths on a undirected graph
 * between two points using the Dinic's algorithm.
 * 
 * @author dyuan
 *
 */
public class DinicAlgorithm {
	
	/**
	 * True, if the paths are vertex disjoint.
	 * False, if the paths are edge disjoint.
	 */
	private boolean vertexDisjoint;
	
	private final UndirectedSparseGraph<Node, Edge> originalGraph;
	
	private DirectedSparseGraph<Node, EdgeInfo> auxiliaryGraph;
	
	private Node source;
	
	private Node destination;
	
	private Map<Node, Integer> labels;
	
	private LinkedList<Node> queue;
	
	private Map<NodePair, Integer> flows;
	
	private Set<Node> excludedNodes;
	
	private boolean foundOneHopPath;
	
	public static class NodePair {
		public Node node1;
		public Node node2;
		
		public boolean equals(Object o) {
			if (!(o instanceof NodePair))
				return false;
			NodePair op = (NodePair) o;
			return this.node1.equals(op.node1) && this.node2.equals(op.node2);
		}
		
		public int hashCode() {
			return node1.hashCode() * 31 + node2.hashCode();
		}
		
		public NodePair(Node node1, Node node2) {
			this.node1 = node1;
			this.node2 = node2;
		}
	}
	
	
	public static class EdgeInfo {	
	}
	
	public DinicAlgorithm(UndirectedSparseGraph<Node, Edge> graph) {
		this.originalGraph = graph;
	}
	
	/**
	 * 
	 * @return all shortest paths.
	 */
	private List<List<Node>> doGetShortestPaths(Node source, Node destination, boolean vertexDisjoint) {
		this.source = source;
		this.destination = destination;
		this.vertexDisjoint = vertexDisjoint;
		if (!buildAuxiliaryGraph())
			return null;
		dinicAlgorithm();
		return collectAllPaths();
	}
	
	/**
	 * 
	 * Use Dinic algorithm to get the max number of shortest paths (in hops) between two nodes.
	 * 
	 */
	public List<List<Node>> getShortestPaths(Node source, Node destination, boolean vertexDisjoint) { 
		this.foundOneHopPath = false;
		this.excludedNodes = new HashSet<Node>();
		return this.doGetShortestPaths(source, destination, vertexDisjoint);
	}
	
	/**
	 * return shortest paths, if the length of shortest paths is lower than bound.
	 * remove all nodes and find further shortest paths, and so on. 
	 * 
	 */
	public List<List<Node>> getVertexDisjointPaths(Node source, Node destination, int bound) {
		List<List<Node>> allPaths = new ArrayList<List<Node>>();
		this.excludedNodes = new HashSet<Node>();
		while (true) {
			List<List<Node>> paths = this.doGetShortestPaths(source, destination, true);
			if (paths == null || paths.get(0).size()-1 > bound)
				break;
			if (paths.get(0).size() == 2)
				this.foundOneHopPath = true;
			allPaths.addAll(paths);
			
			for (List<Node> path : paths) {
				for (int i=1; i<path.size()-1; i++) {
					Node node = path.get(i);
					this.excludedNodes.add(node);
				}
			}
		}
		return allPaths;
		
	}
	
	private List<List<Node>> collectAllPaths() {
		List<List<Node>> paths = new ArrayList<List<Node>>();
		Iterator<Node> secondLevelIter = this.auxiliaryGraph.getSuccessors(source).iterator();
		
		
		while (secondLevelIter.hasNext()) {
			Node secondNode = secondLevelIter.next();
			if (this.getCapacity(source, secondNode) == 0) {
				//we found a path.
				List<Node> path = new ArrayList<Node>();
				path.add(source);
				/**
				 * level 1 being the source, level 2 cannot have temp nodes.
				 */
				
				path.add(secondNode);
				Node n1 = secondNode;
				while (n1 != destination) {
					for (Node n2 : this.auxiliaryGraph.getSuccessors(n1)) {
						if (this.getCapacity(n1, n2) == 0) {
							if (n2.getId().charAt(0) != 't') { 
								path.add(n2);
							}
							this.incCapacity(n1, n2);
							n1 = n2;
							break;
						}
					}
				}
				paths.add(path);				
			}
		
		}		
		//printDisjointShortestPaths(paths);
		return paths;
	}

	private void printDisjointShortestPaths(List<List<Node>> paths) {
		System.out.println("start printing shortest disjoint paths");
		for (List<Node> aPath : paths) {
			for (Node n : aPath) {
				System.out.print(n.getId()+" ");
			}
			System.out.println();
		}
	}

	/**
	 * Build further auxiliary graph
	 * @return true if auxiliary graph contains destination.
	 */
	private boolean buildFurtherAuxiliaryGraph() {
		queue.clear();
		labels.clear();
		queue.addLast(source);
		labels.put(source, 0);
		boolean metDestination = false;
		while (!queue.isEmpty()) {
			Node node = queue.removeFirst();
			int label = labels.get(node);
			for (Node neighbour : this.auxiliaryGraph.getNeighbors(node)) {
				Integer neighbourLabel = labels.get(neighbour);
				if (neighbourLabel == null && this.getCapacity(node, neighbour) > 0) {
					queue.addLast(neighbour);
					labels.put(neighbour, label+1);
					if (neighbour == destination) {
						metDestination = true;
					}
				} 
			}
		}
		return metDestination;
	}

	/**
	 * Build the auxiliary graph with BFS method.
	 * @return true has at least one path. else no path.
	 */
	private boolean buildAuxiliaryGraph() {
		auxiliaryGraph = new DirectedSparseGraph<Node, EdgeInfo>();
		auxiliaryGraph.addVertex(this.source);
		queue = new LinkedList<Node>();
		labels = new HashMap<Node, Integer>();
		queue.addLast(source);
		labels.put(source, 0);
		int shortestDist = -1;
		OUTER:
		while (!queue.isEmpty()) {
			Node node = queue.removeFirst();
			int label = labels.get(node);
			for (Node neighbour : this.originalGraph.getNeighbors(node)) {
				if (this.excludedNodes.contains(neighbour))
					continue;
				if (this.foundOneHopPath && node == source && neighbour == destination)
					continue;
				Integer neighbourLabel = labels.get(neighbour);
				if (neighbourLabel == null) {
					if (shortestDist >= 0 && label == shortestDist) {
						//find a node of level greater than destination. all nodes of level no greater
						//than destination have been found.
						break OUTER;
					}
					auxiliaryGraph.addVertex(neighbour);
					auxiliaryGraph.addEdge(new EdgeInfo(), node, neighbour);
					queue.addLast(neighbour);
					labels.put(neighbour, label+1);
					//find the shortest path.
					if (neighbour == destination) {
						shortestDist = label+1;
					}
				} else {
					if (neighbourLabel == label+1) {
						auxiliaryGraph.addEdge(new EdgeInfo(), node, neighbour);
					}
				}
				
			}
		}
		if (shortestDist == -1) {
			//return there is no path between the source and destination.
			return false;
		}
		
		if (this.vertexDisjoint) {
			//when vertex disjoint, add temp nodes.
			List<Node> allNodes = new ArrayList<Node>(this.auxiliaryGraph.getVertices());
			for (Node node : allNodes) {
				if (node != source && node != destination) {
					if (auxiliaryGraph.getPredecessorCount(node) > 1 && auxiliaryGraph.getSuccessorCount(node) > 1) {
						//split the vertex into two vertex.
						List<EdgeInfo> edgesToRemove = new ArrayList<EdgeInfo>();
						for (EdgeInfo e : auxiliaryGraph.getOutEdges(node)) {
							edgesToRemove.add(e);
						}
						Node tmpNode = new Node("t"+node.toString());
						auxiliaryGraph.addVertex(tmpNode);
						for (Node n : auxiliaryGraph.getSuccessors(node)) {
							auxiliaryGraph.addEdge(new EdgeInfo(), tmpNode, n);
						}		
						auxiliaryGraph.addEdge(new EdgeInfo(), node, tmpNode);
						for (EdgeInfo e : edgesToRemove) {
							auxiliaryGraph.removeEdge(e);
						}
					}
				}
			}
			//redo BFS once to update levels.
			labels.clear();
			queue.clear();
			queue.addLast(source);
			labels.put(source, 0);
			while (!queue.isEmpty()) {
				Node node = queue.removeFirst();
				int label = labels.get(node);
				for (Node neighbour : this.auxiliaryGraph.getSuccessors(node)) {
					Integer neighbourLabel = labels.get(neighbour);
					if (neighbourLabel == null) {
						queue.addLast(neighbour);
						labels.put(neighbour, label+1);
					}
				}
			}
		}	
		this.flows = new HashMap<NodePair, Integer>();
		for (EdgeInfo e : auxiliaryGraph.getEdges()) {
			Pair<Node> pair = auxiliaryGraph.getEndpoints(e);
			NodePair np = new NodePair(pair.getFirst(), pair.getSecond());
			this.flows.put(np, 1);
			np = new NodePair(pair.getSecond(), pair.getFirst());
			this.flows.put(np, 0);
		}
		return true;
	}
	
	private void dinicAlgorithm() {
		do {
			List<Node> path;
			while ((path = findAPathWithDFS()) != null) {
				//printPath(path);
			}
		} while (buildFurtherAuxiliaryGraph());
		
	}
	
	private void printPath(List<Node> path) {
		for (Node n : path) {
			System.out.print(n.getId()+" ");
		}
		System.out.println();
	}

	/**
	 * Use DFS to find a path from the source node to the destination.
	 * @return the path found, or null if no more path available.
	 */
	private List<Node> findAPathWithDFS() {
		queue.clear();
		queue.add(source);
		Set<Node> scannedNodes = new HashSet<Node>();
		scannedNodes.add(source);
		while (!queue.isEmpty()) {
			Node node = queue.peekLast();
			int label = labels.get(node);
			boolean hasNextLevel = false;
			for (Node neighbour : this.auxiliaryGraph.getNeighbors(node)) {
				Integer neighbourLabel = labels.get(neighbour);
				//only nodes not visited, with one level above and has capacity is of interest to us.
				if (neighbourLabel != null && neighbourLabel.intValue() == label+1
						&& !scannedNodes.contains(neighbour) && this.getCapacity(node, neighbour) > 0) {
					scannedNodes.add(neighbour);
					queue.add(neighbour);
					if (neighbour == destination) {
						//we have found a path to the destination.
						List<Node> path = new ArrayList<Node>();
						for (int i=0; i<queue.size(); i++) {
							path.add(queue.get(i));
							if (i != queue.size()-1) {
								this.decCapacity(queue.get(i), queue.get(i+1));
								this.incCapacity(queue.get(i+1), queue.get(i));
							}
							
						}
						
						return path;
					}
					hasNextLevel = true;
					break;
				}
			}
			if (!hasNextLevel) {
				//retrace.
				queue.removeLast();
			}
			
		}
		return null;
		
	}

	public DirectedSparseGraph<Node, EdgeInfo> getAuxiliaryGraph() {
		return this.auxiliaryGraph;
	}
	
	/**
	 * Get the level of node. 
	 * 
	 */
	public int getLevel(Node node) {
		Integer level = this.labels.get(node);
		if (level == null)
			return -1;
		return level.intValue();
	}
	
	public int getCapacity(NodePair np) {
		return this.flows.get(np);
	}
	
	public int getCapacity(Node n1, Node n2) {
		return this.flows.get(new NodePair(n1, n2));
	}
	
	public void incCapacity(Node n1, Node n2) {
		NodePair key = new NodePair(n1, n2);
		Integer old = this.flows.get(key);
		this.flows.put(key, old+1);
	}
	
	public void decCapacity(Node n1, Node n2) {
		NodePair key = new NodePair(n1, n2);
		Integer old = this.flows.get(key);
		this.flows.put(key, old-1);
	}
	

}
