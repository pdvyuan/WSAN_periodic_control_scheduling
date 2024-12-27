package de.seemoo.dyuan.netgen.algo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.collections15.Transformer;

import de.seemoo.dyuan.netgen.structure.Edge;
import de.seemoo.dyuan.netgen.structure.Node;
import de.seemoo.dyuan.netgen.structure.ReliabilityCost;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

/**
 * An algorithm that calculates K shortest loopless path (min cost) with
 * a maximum number of arcs
 * 
 * See paper "An algorithm for calculating k shortest paths with a maximum number of arcs"
 * by Gomes et al.
 *
 * 
 * @author dyuan
 *
 */
public class KDAlgorithm {
	
	public static class KDEdge implements ReliabilityCost {
		private final double cost;
		
		private double reducedCost;
		
		public KDEdge(double cost) {
			this.cost = cost;
		}
		
		public double getReliabilityCost() {
			return this.cost;
		}
		
		public void setReducedCost(double cost) {
			this.reducedCost = cost;
		}
		
		public double getReducedCost() {
			return this.reducedCost;
		}
		
		
	}
	
	protected final UndirectedSparseGraph<Node, Edge> originalGraph;
	
	protected DirectedSparseGraph<Node, KDEdge> graph;

	protected final Node source;
	
	protected final Node destination;
	
	protected final int bound;
	
	protected final int pathCount;
	
	protected Map<Node, KDEdge> shortestPathTree;
	
	protected Map<Node, List<KDEdge>> sortedStarForm;
	
	protected KDAlgoCallback callback;
	
	
	protected int k;
	
	private long startTime;
	
	//the candidatePaths is sorted according to the cost of paths.
	
	public static class CandidatePath {
		public final List<Node> path;
		public final int deviationNode;
		public final double cost;
		
		public CandidatePath(List<Node> path, int deviationNode, double cost) {
			this.path = path;
			this.deviationNode = deviationNode;
			this.cost = cost;
		}
		
	}
	
	private Set<CandidatePath> candidatePaths;
	
	
	public double computePathCost(List<Node> path) {
		int id = 0;
		Node dst = path.get(path.size()-1);
		Node n1, n2;
		n1 = path.get(id++);
		double cost = 0;
		do {
			n2 = path.get(id++);
			KDEdge edge = graph.findEdge(n1, n2);
			cost += edge.cost;
			n1 = n2;
		} while (n1 != dst);
		return cost;
	}
	

	public KDAlgorithm(UndirectedSparseGraph<Node, Edge> graph, Node source, Node destination, int bound, int pathCount) {
		this.originalGraph = graph;
		this.source = source;
		this.destination = destination;
		this.bound = bound;
		this.pathCount = pathCount;
	}
	
	public void setCallback(KDAlgoCallback callback) {
		this.callback = callback;
	}
	
	/**
	 * perform the algorithm.
	 */
	public void perform() {
		prepareSteps();
		
		enumerateLooplessShortestPath();
	}


	protected void prepareSteps() {
		createDirectedTree();
		buildDijkstraTreeAtDest();
		computeReducedCost();
		arrangeArcsInSortedStarForm();
		initCandidatePathSet();
		startTime = System.currentTimeMillis();
	}
	
	private boolean isLoopless(List<Node> path) {
		Set<Long> nodeSet = new HashSet<Long>();
		for (Node node : path) {
			if (nodeSet.contains(node.getIndex()))
				return false;
			nodeSet.add(node.getIndex());
		}
		return true;
	}
	
	private boolean isLoopLessTillNode(List<Node> path, int lastNodeId) {
		HashSet<Long> nodeSet = new HashSet<Long>();
		for (int i=0; i<=lastNodeId; i++) {
			Node node = path.get(i);
			if (nodeSet.contains(node.getIndex())) {
				return false;
			}
			nodeSet.add(node.getIndex());
		}
		return true;
	}
	
	private void kdPathCallback(List<Node> path) {
		double cost = this.computePathCost(path);
		double reliability = Math.exp(-cost);
		if (this.callback != null) {
			this.callback.callback(path, reliability);
		}
	}
	
	public void printPath(CandidatePath candPath) {
		List<Node> path = candPath.path;
		for (int i=0; i<path.size()-1; i++) {
			Node node = path.get(i);
			System.out.print(node.getId()+" -> ");
		}
		
		System.out.println(path.get(path.size()-1).getId()+"\tcost="+this.computePathCost(path));
	}
	
	private boolean containLoop(List<Node> path, int deviationNodeId, Node headNode) {
		for (int i=0; i<deviationNodeId; i++) {
			if (path.get(i) == headNode) {
				return true;
			}
		}
		return false;
	}
	
	private void addShortestPathAfterNode(List<Node> path, Node startingNode) {
		KDEdge edge = this.shortestPathTree.get(startingNode);
		while (edge != null) {
			Node next = this.graph.getSource(edge);
			path.add(next);
			edge = this.shortestPathTree.get(next);
		}		
	}
	
	public static long COMPUTATION_LIMIT = 100*1000;
	
	public List<Node> getNextLooplessShortestPath() {
		List<Node> pathFound = null;
		while (this.k < this.pathCount && !this.candidatePaths.isEmpty()) {
			Iterator<CandidatePath> iter = this.candidatePaths.iterator();
			CandidatePath candPath = iter.next();
			iter.remove();
//			System.out.print("remove ");
//			this.printPath(candPath);
			List<Node> path = candPath.path;
			if (path.size()-1 <= this.bound && isLoopless(path)) {
				this.k++;
//				printPath(candPath);
				pathFound = path;
			}
			int deviationNodeId = candPath.deviationNode;
			int maxDeviationNodeId;
			if (path.size()-1 == this.bound && deviationNodeId < this.bound - 1) {
				maxDeviationNodeId = this.bound - 2;
			} else if (path.size()-1 != this.bound && deviationNodeId < this.bound) {
				maxDeviationNodeId = Math.min(path.size()-2, this.bound-1);
			} else {
				maxDeviationNodeId = -1;
			}
			
				
			if (maxDeviationNodeId != -1) {
				int currDevNodeId = deviationNodeId;
				do {
					Node n1 = path.get(currDevNodeId);
					Node n2 = path.get(currDevNodeId+1);
					KDEdge deviationEdge = this.graph.findEdge(n1, n2);
					List<KDEdge> edges = this.sortedStarForm.get(n1);
					int id = edges.indexOf(deviationEdge)+1;
					Node head = null;
					while (id < edges.size()) {
						KDEdge e = edges.get(id);
						Node n = graph.getDest(e);
						if (!this.containLoop(path, currDevNodeId, n)) {
							head = n;
							break;
						}
						id++;
					}
					if (head != null) {
						// we found a new path to be put into candidate paths.
						List<Node> newPath = new ArrayList<Node>();
						for (int i=0; i<=currDevNodeId; i++) {
							newPath.add(path.get(i));
						}
						newPath.add(head);
						this.addShortestPathAfterNode(newPath, head);
						this.addCandidatePath(newPath, currDevNodeId);
					}
					if (currDevNodeId == maxDeviationNodeId) {
						maxDeviationNodeId = -1;
					} else {
						currDevNodeId++;
					}
				} while (isLoopLessTillNode(path, currDevNodeId) && maxDeviationNodeId != -1);
			}
			if (pathFound != null) {
				return pathFound;
			}
			
			if (System.currentTimeMillis() - this.startTime > COMPUTATION_LIMIT) {
				System.out.println("KDAlgorithm timeout");
				break;
			}
		}
		return null;
	}
	
	public void enumerateLooplessShortestPath() {
		List<Node> path;
		while ((path=this.getNextLooplessShortestPath()) != null) {
			this.kdPathCallback(path);
		}
	}


	public void initCandidatePathSet() {
		
		k = 0;
		this.candidatePaths = new TreeSet<CandidatePath>(new Comparator<CandidatePath>() {

			@Override
			public int compare(CandidatePath c1, CandidatePath c2) {
				if (c1.cost < c2.cost)
					return -1;
				if (c1.cost > c2.cost)
					return 1;
				return c1.hashCode() - c2.hashCode();
			}
			
		});
		List<Node> path = findShortestPathFromSrc2Dst();
		if (path != null) {
			this.addCandidatePath(path, 0);
		}
	}
	
	public Set<CandidatePath> getCandidatePaths() {
		return this.candidatePaths;
	}
	
	private void addCandidatePath(List<Node> path, int deviationNode) {
		double cost = this.computePathCost(path);
		CandidatePath cp = new CandidatePath(path, deviationNode, cost);
		this.candidatePaths.add(cp);
//		System.out.print("add cand id = "+deviationNode+": ");
//		this.printPath(cp);
	}
	
	public List<Node> findShortestPathFromSrc2Dst() {
		KDEdge edge = this.shortestPathTree.get(this.source);
		if (edge == null)
			return null;
		List<Node> res = new ArrayList<Node>();
		res.add(this.source);
		while (edge != null) {
			Node next = this.graph.getSource(edge);
			res.add(next);
			edge = this.shortestPathTree.get(next);
		}
		if (res.get(res.size()-1) != this.destination) {
			throw new RuntimeException("this should not happen!");
		}
		return res;		
	}

	/**
	 * Arrange arcs in sorted star form
	 */
	public void arrangeArcsInSortedStarForm() {
		this.sortedStarForm = new HashMap<Node, List<KDEdge>>();
		
		for (Node node : graph.getVertices()) {
			List<KDEdge> edges = new ArrayList<KDEdge>();
			edges.addAll(graph.getOutEdges(node));
			Collections.sort(edges, new Comparator<KDEdge>() {
				@Override
				public int compare(KDEdge e1, KDEdge e2) {
					if (e1.getReducedCost() < e2.getReducedCost())
						return -1;
					if (e1.getReducedCost() > e2.getReducedCost())
						return 1;
					return 0;
				}
				
			});
			this.sortedStarForm.put(node, edges);
			
			KDEdge lastHop = this.shortestPathTree.get(node);
			if (lastHop != null) {
				Node src = graph.getSource(lastHop);
				Node dst = graph.getDest(lastHop);
				KDEdge edgeOnShortestPath = graph.findEdge(dst, src);
				if (!edges.remove(edgeOnShortestPath))
					throw new RuntimeException("star form should contains this edge!");
				edges.add(0, edgeOnShortestPath);
			}
		}
	}
	
	
	public Map<Node, List<KDEdge>> getSortedStarForm() {
		return this.sortedStarForm;
	}

	public DirectedSparseGraph<Node, KDEdge> getGraph() {
		return this.graph;
	}
	
	public void createDirectedTree() {
		graph = new DirectedSparseGraph<Node, KDEdge>();
		for (Node node : this.originalGraph.getVertices()) {
			graph.addVertex(node);
		}
		for (Edge edge : this.originalGraph.getEdges()) {
			Node n1 = edge.getSmallerNode();
			Node n2 = edge.getBiggerNode();
			double cost = edge.getReliabilityCost();
			KDEdge edge1 = new KDEdge(cost);
			KDEdge edge2 = new KDEdge(cost);
			graph.addEdge(edge1, n1, n2);
			graph.addEdge(edge2, n2, n1);
		}
	}

	public void computeReducedCost() {
		for (KDEdge edge : this.graph.getEdges()) {
			Node src = this.graph.getSource(edge);
			Node dest = this.graph.getDest(edge);
			//when src = inf, then dst must be inf. 
			//if dst = inf, then src can be not inf. in this case, the edge is not
			if (dest.getDistanceToDest() == Double.POSITIVE_INFINITY || src.getDistanceToDest() == Double.POSITIVE_INFINITY) {
				edge.setReducedCost(Double.POSITIVE_INFINITY);
			} else {
				edge.setReducedCost(dest.getDistanceToDest() - src.getDistanceToDest() + edge.getReliabilityCost());
			}
		}
	}

	/**
	 * Build Dijkstra tree rooted at destination node.
	 */
	public void buildDijkstraTreeAtDest() {
		Transformer<KDEdge, Double> edgeTransformer = new Transformer<KDEdge, Double>() {

			@Override
			public Double transform(KDEdge edge) {
				return edge.cost;
			}
			
		};
		DijkstraShortestPath<Node, KDEdge> shortestPathFinder = new DijkstraShortestPath<Node, KDEdge>(this.graph, edgeTransformer);
		this.shortestPathTree = shortestPathFinder.getIncomingEdgeMap(destination);
		for (Node node : this.graph.getVertices()) {
			node.setDistanceToDest(Double.POSITIVE_INFINITY);
		}
		for (Map.Entry<Node, KDEdge> entry : this.shortestPathTree.entrySet()) {
			Node node = entry.getKey();
			KDEdge edge = entry.getValue();
			if (node == this.destination) {
				node.setDistanceToDest(0);
			} else {
				Node otherNode = graph.getSource(edge);
				if (otherNode.getDistanceToDest() == Double.POSITIVE_INFINITY) {
					throw new IllegalArgumentException("this should not happen!");
				}
				double dist = otherNode.getDistanceToDest() + edge.getReliabilityCost();
				node.setDistanceToDest(dist);
			}
		}
		
	}

	
	
}
