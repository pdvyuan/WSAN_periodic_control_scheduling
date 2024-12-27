package de.seemoo.dyuan.netgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections15.Transformer;

import de.seemoo.dyuan.netgen.algo.BellmanFordAlgorithm;
import de.seemoo.dyuan.netgen.algo.BhandariAlgorithm;
import de.seemoo.dyuan.netgen.algo.DinicAlgorithm;
import de.seemoo.dyuan.netgen.algo.DisjointPairPath;
import de.seemoo.dyuan.netgen.algo.EdgeCostTransformer;
import de.seemoo.dyuan.netgen.algo.MostReliablePairDisjointPath;
import de.seemoo.dyuan.netgen.structure.Edge;
import de.seemoo.dyuan.netgen.structure.Node;
import de.seemoo.dyuan.utils.Global;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.util.Pair;

/**
 * 
 * @author dyuan
 * 
 * A Flow is a connection between one source and one destination.
 * 
 * The connection is divided into upflow and downflow. Each can contains a number of subpaths.
 * 
 * The model wait-at-GWs is implemented.
 * 
 * If a subpath belongs to the upflow, it must end with a GW. If a subpath belongs to the downflow,
 * it must starts with a GW.
 * 
 * In addition, all gateways are fully connected and we suppose the delay of transmitting
 * packet between gateways needs 0 delay.
 * 
 * Slots are enumerated from 0 onward.
 */
public class Flow implements Route {
	
	
	private final NetworkModel network;
	
	//The source and the destination are normal nodes.
	private final Node source;
	
	private final Node destination;
	
	private List<Subpath> upflows;
	
	private List<Subpath> downflows;
	
	//period measured in slots
	private int period;
	
	//deadline measured in slots
	private int deadline;
	
	private int flowId;
	
	private static final BellmanFordAlgorithm<Edge> bfAlgo;
		
	static {
		bfAlgo = new BellmanFordAlgorithm<Edge>();
	}
	
	private static EdgeCostTransformer<Edge> reliabilityCostTrans = new EdgeCostTransformer<Edge>() {

		@Override
		public double getCost(Edge e) {
			return e.getReliabilityCost();
		}
		
	};

	
	/**
	 * Constructor
	 * 
	 */
	public Flow(NetworkModel network, Node source, Node destination) {
		this.network = network;
		this.source = source;
		this.destination = destination;
		this.upflows = new ArrayList<Subpath>();
		this.downflows = new ArrayList<Subpath>();
	}
	
	public NetworkModel getNetwork() {
		return this.network;
	}
	
	public Node getSource() {
		return this.source;
	}
	
	public Node getDestination() {
		return this.destination;
	}
	
	
	/**
	 * It uses Bellman-Ford algorithm.
	 */
	private List<Subpath> getMostReliableVertexDisjointSubpathsOfLengthConstraint(boolean toGWs, int nPaths, int maxLength) {
		List<Subpath> ret = new ArrayList<Subpath>();
		Set<Node> excludedNodes = new HashSet<Node>();
		for (int i=0; i<nPaths; i++) {
			List<Node> path;
			if (toGWs) {
				path = bfAlgo.findShortestPathOfLengthConstraints(excludedNodes, this.network.copyGraph, 
						source, this.virtualGW, maxLength, reliabilityCostTrans);
			} else {
				path = bfAlgo.findShortestPathOfLengthConstraints(excludedNodes, this.network.copyGraph, 
						this.virtualGW, this.destination, maxLength, reliabilityCostTrans);
			}
			
			if (path != null) {
				ret.add(this.transformToSubpath(path, toGWs));
				for (int j=1; j<path.size()-1; j++) {
					excludedNodes.add(path.get(j));
				}
			} else {
				break;
			}
		}
		return ret;
	}
	
	/**
	 * Find the msot reliable subpaths of this flow, excluding all nodes except source
	 * or destination.
	 */
	private List<Subpath> getMostReliableVertexDisjointSubpaths(boolean toGWs, int nPaths) {
		List<Subpath> ret = new ArrayList<Subpath>();
		final Set<Node> excludedNodes = new HashSet<Node>();
		Transformer<Edge, Double> wtTransformer = new Transformer<Edge, Double>() {
			@Override
			public Double transform(Edge edge) {
				if (excludedNodes.contains(edge.getBiggerNode())
						|| excludedNodes.contains(edge.getSmallerNode())) {
					//if the link is used, the edge is positive infinity.
					return Double.MAX_VALUE;
				} else {
					return edge.getReliabilityCost();
				}
				
			}
			
		};
		if (toGWs) {
			for (int i=0; i<nPaths; i++) {
				DijkstraShortestPath<Node, Edge> algo = new DijkstraShortestPath<Node, Edge>(this.network.copyGraph, wtTransformer);
				List<Edge> subpath = algo.getPath(this.source, this.virtualGW);
				double r = Subpath.getSubPathReliability(subpath, excludedNodes);
				if (r > 0.0) {
					//upflow
					subpath = subpath.subList(0, subpath.size()-1);
					Subpath uppath = new Subpath(subpath, source);
					List<Node> senders = uppath.getSenderNodes();
					excludedNodes.addAll(senders.subList(1, senders.size()));
					excludedNodes.add(uppath.getLastNode());
					ret.add(uppath);
				} else {
					break;
				}
			}
		} else {
			for (int i=0; i<nPaths; i++) {
//				if (!this.network.copyGraph.containsVertex(this.destination)) {
//					System.err.println("destination is not contained in the graph");
//				}
				DijkstraShortestPath<Node, Edge> algo = new DijkstraShortestPath<Node, Edge>(this.network.copyGraph, wtTransformer);
				List<Edge> subpath = algo.getPath(this.virtualGW, this.destination);
				double r = Subpath.getSubPathReliability(subpath, excludedNodes);
				if (r > 0.0) {
					Node gateway = subpath.get(0).getTheOtherNode(this.virtualGW);
					subpath = subpath.subList(1, subpath.size());
					Subpath downpath = new Subpath(subpath, gateway);
					List<Node> senders = downpath.getSenderNodes();
					excludedNodes.addAll(senders);
					ret.add(downpath);
				} else {
					break;
				}
			}
		}
		return ret;
	}
	
	/**
	 * 
	 * @return -1, if there is no path.
	 */
	public int getDeadlineLowerBound() {
		int len = 0;
		this.network.copyGraph = this.network.copyGraph();
		this.network.copyGraph.addVertex(virtualGW);
		//add 100% link quality links betweeen virtual GW and real GWs.
		for (Node gw : this.network.getGateways()) {
			this.network.addEdgeToCopyGraph(gw, virtualGW, 1.0);
		}
		
		Transformer<Edge, Integer> wtTransformer = new Transformer<Edge, Integer>() {
			@Override
			public Integer transform(Edge edge) {
				return 1;
			}
		};
		
		DijkstraShortestPath<Node, Edge> algo = new DijkstraShortestPath<Node, Edge>(this.network.copyGraph, wtTransformer);
		Number dist = algo.getDistance(this.source, this.virtualGW);
		if (dist == null) {
			return -1;
		}
		len += (dist.intValue() - 1);
		dist = algo.getDistance(this.virtualGW, this.destination);
		if (dist == null) {
			return -1;
		}
		len += (dist.intValue() - 1);
		return len;
	}
	
	private List<Subpath> getShortestVertexDisjointSubpaths(boolean toGWs, int nPaths) {
		List<Subpath> ret = new ArrayList<Subpath>();
		final Set<Node> excludedNodes = new HashSet<Node>();
		Transformer<Edge, Double> wtTransformer = new Transformer<Edge, Double>() {
			@Override
			public Double transform(Edge edge) {
				if (excludedNodes.contains(edge.getBiggerNode())
						|| excludedNodes.contains(edge.getSmallerNode())) {
					//if the link is used, the edge is positive infinity.
					return Double.MAX_VALUE;
				} else {
					return 1.0;
				}
				
			}
			
		};
		if (toGWs) {
			for (int i=0; i<nPaths; i++) {
				DijkstraShortestPath<Node, Edge> algo = new DijkstraShortestPath<Node, Edge>(this.network.copyGraph, wtTransformer);
				List<Edge> subpath = algo.getPath(this.source, this.virtualGW);
				int l = Subpath.getSubpathLength(subpath, excludedNodes);
				if (l != Integer.MAX_VALUE) {
					subpath = subpath.subList(0, subpath.size()-1);
					Subpath uppath = new Subpath(subpath, source);
					List<Node> senders = uppath.getSenderNodes();
					excludedNodes.addAll(senders.subList(1, senders.size()));
					excludedNodes.add(uppath.getLastNode());
					ret.add(uppath);
				} else {
					break;
				}
			}
		} else {
			for (int i=0; i<nPaths; i++) {
				DijkstraShortestPath<Node, Edge> algo = new DijkstraShortestPath<Node, Edge>(this.network.copyGraph, wtTransformer);
				List<Edge> subpath = algo.getPath(this.virtualGW, this.destination);
				int l = Subpath.getSubpathLength(subpath, excludedNodes);
				if (l != Integer.MAX_VALUE) {
					Node gateway = subpath.get(0).getTheOtherNode(this.virtualGW);
					subpath = subpath.subList(1, subpath.size());
					Subpath downpath = new Subpath(subpath, gateway);
					List<Node> senders = downpath.getSenderNodes();
					excludedNodes.addAll(senders);
					ret.add(downpath);
				} else {
					break;
				}
			}
		}
		return ret;
	}
	
	
	public void addSubpath(Subpath subpath, boolean upflow) {
		List<Subpath> subpaths = this.getSubpaths(upflow);
		//set subpath id.
		subpath.setSubpathId(subpaths.size());
		subpaths.add(subpath);
		subpath.setParentFlow(this);
		subpath.setUpflow(upflow);
		
	}
	
	private BhandariAlgorithm.EdgeWeightTransformer lossRateTransformer = new BhandariAlgorithm.EdgeWeightTransformer() {
		@Override
		public double transform(Edge edge) {
			//[0, 5e4]
			return Math.round((1.0 - edge.getLinkQuality()) * 100000);
		}
	};
	
	private BhandariAlgorithm.EdgeWeightTransformer negativeLogReceptionRate = new BhandariAlgorithm.EdgeWeightTransformer() {
		
		
		@Override
		public double transform(Edge edge) {
			double v = edge.getReliabilityCost();
			//[0, 3e4]
			return Math.round(v * 100000);
		}
	};
	
	private BhandariAlgorithm.EdgeWeightTransformer etxTransformer = new BhandariAlgorithm.EdgeWeightTransformer() {
		
		@Override
		public double transform(Edge edge) {
			double v = 1/edge.getLinkQuality();
			//[1e4, 2e4]
			return Math.round(v * 10000);
		}
	};
	
	private BhandariAlgorithm.EdgeWeightTransformer totalHopsTransformer = new BhandariAlgorithm.EdgeWeightTransformer() {

		@Override
		public double transform(Edge edge) {
			return 1;
		}
		
	};
	
	private Subpath transformToSubpath(List<Node> nodes, boolean upflow) {
		List<Edge> edges = new ArrayList<Edge>();
		Node sender;
		if (upflow) {
			if (nodes.get(0) != this.source || nodes.get(nodes.size() - 1) != virtualGW) {
				throw new IllegalStateException("the first or last node is wrong!");
			}
			for (int i=0; i<nodes.size()-2; i++) {
				Edge edge = this.network.graph.findEdge(nodes.get(i), nodes.get(i+1));
				if (edge == null) {
					throw new IllegalArgumentException();
				}
				edges.add(edge);
			}
			sender = nodes.get(0);
		} else {
			if (nodes.get(0) != this.virtualGW || nodes.get(nodes.size()-1) != this.destination) {
				throw new IllegalStateException("Wrong source or destination node!");
			}
			for (int i=1; i<nodes.size()-1; i++) {
				Edge edge = this.network.graph.findEdge(nodes.get(i), nodes.get(i+1));
				if (edge == null) {
					throw new IllegalArgumentException();
				}
				edges.add(edge);
			}
			sender = nodes.get(1);
		}
		
		return new Subpath(edges, sender);
	}
	
	private Node virtualGW = new Node("vGW");
	

	
	private List<Subpath> transformToSubPaths(List<List<Node>> paths, boolean up) {
		List<Subpath> res = new ArrayList<Subpath>();
		for (List<Node> path : paths) {
			res.add(this.transformToSubpath(path, up));
		}
		return res;
	}
	
	private List<Subpath> transformToSubPaths(DisjointPairPath pair, boolean up) {
		List<Subpath> res = new ArrayList<Subpath>();
		if (pair != null) {
			Subpath path = this.transformToSubpath(pair.getPath1(), up);
			res.add(path);
			path = this.transformToSubpath(pair.getPath2(), up);
			res.add(path);
		}
		return res;
	}
	
	private Map<Node, Double> copiedNodeUtils;
	
	
	/**
	 * This method is for doing routing with length constraint, 
	 * if upRoutingFirst, it will first do upflow routing, then downflow.
	 * 
	 */
	public void createVertexDisjointRoutingLCPart(int nPaths, int routingMethod, 
			boolean upRoutingFirst, boolean auditNodeUtil) {
		if (routingMethod == NetworkModel.ROUTING_DISJOINT_PAIR_MOST_RELIABLE_LC && nPaths != 2) {
			throw new IllegalArgumentException("for routing method disjoint pair lc we can only generate a pair of paths");
		}
		this.upflows.clear();
		this.downflows.clear();
		
		this.network.copyGraph = this.network.copyGraph();
		//pre-process graph
		copiedNodeUtils = null;
		List<Edge> edgesOnEnd = null;
		Node end = null;
		boolean removeEnd = false;
		
		this.network.copyGraph.addVertex(virtualGW);
		//add 100% link quality links betweeen virtual GW and real GWs.
		for (Node gw : this.network.getGateways()) {
			this.network.addEdgeToCopyGraph(gw, virtualGW, 1.0);
		}
		
		//if not piggyback (opportunistic aggregation) and period is already determined.
		if (auditNodeUtil) {
			//copy the remaining utilization of all nodes.
			copiedNodeUtils = new HashMap<Node, Double>(network.nodeUtils);		
			
			end = (upRoutingFirst ? this.destination : this.source);
			removeEnd =  (copiedNodeUtils.get(end) < 2 * (1.0/this.period));
			if (removeEnd) {
				edgesOnEnd = new ArrayList<Edge>(this.network.copyGraph.getIncidentEdges(end));
			}
			
			if (!organizeCopyGraphBeforeRouting(nPaths, upRoutingFirst)) {
				return;
			}			
			
		}
		
		int bound = this.network.copyGraph.getVertexCount()-1;
		if (this.deadline != 0) {
			bound = Math.min(this.deadline/2 + 1, bound);
		}
		List<Subpath> paths1 = null;
		switch (routingMethod) {
		case NetworkModel.ROUTING_DISJOINT_PAIR_MOST_RELIABLE_LC:
			MostReliablePairDisjointPath finder;
			if (upRoutingFirst) {
				finder = new MostReliablePairDisjointPath(this.network.copyGraph,
						this.source, virtualGW, bound); 
			} else {
				finder = new MostReliablePairDisjointPath(this.network.copyGraph,
						virtualGW, this.destination, bound);
			}
			
			DisjointPairPath pair = finder.findMostReliablePair();
			paths1 = this.transformToSubPaths(pair, upRoutingFirst);
			break;
		case NetworkModel.ROUTING_DISJOINT_BF_MOST_RELIABLE:
			paths1 = this.getMostReliableVertexDisjointSubpathsOfLengthConstraint(upRoutingFirst, nPaths, bound);
			break;
		default:
			throw new RuntimeException("routing method "+routingMethod+" unknown!");
		}
	
		if (paths1.size() == nPaths) {
			//found n uppaths
			//modified node utilities.
			if (auditNodeUtil) {
				updateUtilizationAfterRouting(nPaths, paths1, upRoutingFirst);
				
				//process copyGraph for downpath routing.
				//added by pdv possibly restore end.
				if (removeEnd) {
					this.network.copyGraph.addVertex(end);
					for (Edge edge : edgesOnEnd) {
						if (this.network.copyGraph.containsVertex(edge.getTheOtherNode(end))) {
							this.network.copyGraph.addEdge(edge, edge.getSmallerNode(), edge.getBiggerNode());
						}
					}
				}
//				this.network.copyGraph = this.network.copyGraph();
//				this.network.copyGraph.addVertex(virtualGW);
//				//add 100% link quality links betweeen virtual GW and real GWs.
//				for (Node gw : this.network.getGateways()) {
//					this.network.addEdgeToCopyGraph(gw, virtualGW, 1.0);
//				}
				
				if (!organizeCopyGraphBeforeRouting(nPaths, !upRoutingFirst)) {
					return;
				}				
			}
			
			int maxFlow1Hops = this.getMaxHopsAmongPaths(paths1);
			
			bound = this.deadline - maxFlow1Hops + 1;
			bound = Math.min(bound, this.network.copyGraph.getVertexCount()-1);
			
			List<Subpath> paths2 = null;
			
			switch (routingMethod) {
			case NetworkModel.ROUTING_DISJOINT_PAIR_MOST_RELIABLE_LC:
				MostReliablePairDisjointPath finder;
				if (upRoutingFirst) {
					finder = new MostReliablePairDisjointPath(this.network.copyGraph,
							virtualGW, this.destination, bound);
				} else {
					finder = new MostReliablePairDisjointPath(this.network.copyGraph,
							this.source, virtualGW, bound);
				}
				
				DisjointPairPath pair = finder.findMostReliablePair();
				paths2 = this.transformToSubPaths(pair, !upRoutingFirst);
				break;
			case NetworkModel.ROUTING_DISJOINT_BF_MOST_RELIABLE:
				paths2 = this.getMostReliableVertexDisjointSubpathsOfLengthConstraint(!upRoutingFirst, nPaths, bound);
				break;
			default:
				throw new RuntimeException("routing method "+routingMethod+" unknown!");
			}

			
			if (paths2.size() == nPaths) {
				if (auditNodeUtil) {
					//compute the utilization of all nodes.
					//destination node
					updateUtilizationAfterRouting(nPaths, paths2, !upRoutingFirst);
					
				}
				
				int maxFlow2Hops = this.getMaxHopsAmongPaths(paths2);
				
				if (maxFlow1Hops + maxFlow2Hops > this.deadline) {
					throw new RuntimeException("should not happen!");
					//System.out.println("routing paths too long for period");
					//return;
				}
				
				for (Subpath path : paths1) {
					this.addSubpath(path, upRoutingFirst);
				}
				
				for (Subpath path : paths2) {
					this.addSubpath(path, !upRoutingFirst);
				}
			}
		}
	}
	
//	private void checkSameGraph(UndirectedSparseGraph<Node, Edge> compGraph,
//			UndirectedSparseGraph<Node, Edge> copyGraph) {
//		if (compGraph.getVertexCount() != copyGraph.getVertexCount()) {
//			throw new RuntimeException("compGraph vertex = "+compGraph.getVertexCount()
//					+", copyGraph vertex = "+copyGraph.getVertexCount());
//		}
//		if (compGraph.getEdgeCount() != copyGraph.getEdgeCount()) {
//			throw new RuntimeException("compGraph edge = "+compGraph.getEdgeCount()
//					+", copyGraph edge = "+copyGraph.getEdgeCount());
//		}
//		for (Node node : compGraph.getVertices()) {
//			if (!copyGraph.containsVertex(node)) {
//				throw new RuntimeException("comp graph has node "+node+", but copy graph does not");
//			}
//		}
//		for (Edge edge : compGraph.getEdges()) {
//			Edge edge2 = copyGraph.findEdge(edge.getSmallerNode(), edge.getBiggerNode());
//			if (edge2 == null) {
//				throw new RuntimeException("comp graph has edge "+edge.getSmallerNode()+"->"+edge.getBiggerNode()+", but copy graph does not");
//			}
//			if (edge.getLinkQuality() != edge2.getLinkQuality()) {
//				throw new RuntimeException("edge "+edge.getSmallerNode()+"->"+edge.getBiggerNode()+" link quality different");
//			}
//			
//		}
//		
//	}
	/**
	 * Experiments should that METRIC_TOTAL_HOPS and METRIC_EFFECTIVE_DEADLINE give the same feasibility given the same 
	 * number of channels.
	 * For the feasible problems (by any number of channels), the reliability and hops are the same.
	 * 
	 * For for all routable flows, the METRIC_TOTAL_HOPS gives slightly higher reliability (0.7751 vs 0.7750)
	 * and slightly smaller hop count (15.2618 vs 15.2685).
	 */
	
	public static final int METRIC_TOTAL_HOPS = 0;
	
	public static final int METRIC_EFFECTIVE_DEADLINE = 1;
	
	private static int importantMetric = METRIC_TOTAL_HOPS;
	
	public static void setImportantMetric(int metric) {
		importantMetric = metric;
	}
	
	public static int getImportantMetric() {
		return importantMetric;
	}
	
	
	private boolean hasBetterMetricThanCurrent(List<Subpath> uppaths, List<Subpath> downpaths) {
		if (importantMetric == METRIC_TOTAL_HOPS) {
			return getAllPathHops(uppaths, downpaths) < this.getAllPathHops();
		} else {
			return getMinDeadlineLength(uppaths, downpaths) < this.getMinDeadlineLength();
		}	
		
	}

	public void createVertexDisjointRouting(int nPaths, int routingMethod) {
		boolean lengthBound = (routingMethod == NetworkModel.ROUTING_DISJOINT_PAIR_MOST_RELIABLE_LC || routingMethod == NetworkModel.ROUTING_DISJOINT_BF_MOST_RELIABLE);
		//boolean auditUtil = this.network.getMaxNodeUtil() != 0.0;
		boolean auditUtil = false;
		boolean checkDeadline = false;
		if (lengthBound) {
			this.createVertexDisjointRoutingLCPart(nPaths, routingMethod, true, auditUtil);
		} else {
			this.createVertexDisjointRoutingNLCPart(nPaths, routingMethod, true, auditUtil, checkDeadline);
			if (!auditUtil || this.nodesRemoved == 0) {
				//both routings are done on the biggest possible graph. we can quit early.
				if (this.upflows.size() == nPaths && this.downflows.size() == nPaths && auditUtil) {
					this.network.nodeUtils = this.copiedNodeUtils;
				}
				return;
			} 
		}
		List<Subpath> bufUpflows = new ArrayList<Subpath>(this.upflows);
		List<Subpath> bufDownflows = new ArrayList<Subpath>(this.downflows);
		Map<Node, Double> bufCopiedNodeUtils = this.copiedNodeUtils;
		if (lengthBound) {
			this.createVertexDisjointRoutingLCPart(nPaths, routingMethod, false, auditUtil);
		} else {
			this.createVertexDisjointRoutingNLCPart(nPaths, routingMethod, false, auditUtil, checkDeadline);
		}
		
		if (bufUpflows.size() == nPaths && bufDownflows.size() == nPaths) {
			if (this.upflows.size() != nPaths || this.downflows.size() != nPaths 
					|| hasBetterMetricThanCurrent(bufUpflows, bufDownflows)) {
//				System.out.printf("routing2 upflow %d downflow %d, routing1 hops %d routing2 hops %d\n",
//						this.upflows.size(), this.downflows.size(), getAllPathHops(bufUpflows, bufDownflows), this.getAllPathHops());
//				System.out.println("replace down/up routing by up/down routing "+this);
				this.upflows.clear();
				this.downflows.clear();
				
				for (Subpath path : bufUpflows) {
					this.addSubpath(path, true);
				}
				
				for (Subpath path : bufDownflows) {
					this.addSubpath(path, false);
				}
				if (auditUtil) {
					this.network.nodeUtils = bufCopiedNodeUtils;
				}
				return;
			} 
		} 
		
		if (this.upflows.size() == nPaths && this.downflows.size() == nPaths && auditUtil) {
			this.network.nodeUtils = this.copiedNodeUtils;
		}
		
	
	}
	
	
	/**
	 *
	 * nodeUtils is the remaining utilization of each node. 
	 * 
	 * 
	 */
	public void createVertexDisjointRoutingNLCPart(int nPaths, int routingMethod, boolean upRoutingFirst,
			boolean auditNodeUtil, boolean checkDeadline)  {
		this.upflows.clear();
		this.downflows.clear();
		
		this.network.copyGraph = this.network.copyGraph();
		//pre-process graph
		copiedNodeUtils = null;
		List<Edge> edgesOnEnd = null;
		Node end = null;
		boolean removeEnd = false;
		
		this.network.copyGraph.addVertex(virtualGW);
		//add 100% link quality links betweeen virtual GW and real GWs.
		for (Node gw : this.network.getGateways()) {
			this.network.addEdgeToCopyGraph(gw, virtualGW, 1.0);
		}
		
		//if not piggyback (opportunistic aggregation) and period is already determined.
		if (auditNodeUtil) {
			//copy the remaining utilization of all nodes.
			copiedNodeUtils = new HashMap<Node, Double>(network.nodeUtils);		
			
			end = (upRoutingFirst ? this.destination : this.source);
			removeEnd =  (copiedNodeUtils.get(end) < 2 * (1.0/this.period));
			if (removeEnd) {
				edgesOnEnd = new ArrayList<Edge>(this.network.copyGraph.getIncidentEdges(end));
			}
			
			if (!organizeCopyGraphBeforeRouting(nPaths, upRoutingFirst)) {
				return;
			}
		}
		
		List<Subpath> paths1 = null;
		BhandariAlgorithm router = null;
		switch (routingMethod) {
		case NetworkModel.ROUTING_DISJOINT_MOST_RELIABLE:
			paths1 = this.getMostReliableVertexDisjointSubpaths(upRoutingFirst, nPaths);
			break;
		case NetworkModel.ROUTING_DISJOINT_SHORTEST_PATH:
			paths1 = this.getShortestVertexDisjointSubpaths(upRoutingFirst, nPaths);
			break;
		case NetworkModel.ROUTING_DISJOINT_SHORTEST_PATHS_WITH_DINIC:
			paths1 = this.getShortestVertexDisjointSubpathsDinic(upRoutingFirst, nPaths);
			break;
		case NetworkModel.ROUTING_DISJOINT_BHANDARI_MIN_SUM_LOSS_RATE:
			router = new BhandariAlgorithm(this.network.copyGraph, this.lossRateTransformer);
			break;
		case NetworkModel.ROUTING_DISJOINT_BHANDARI_MAX_PROD_REC_RATE:
			router = new BhandariAlgorithm(this.network.copyGraph, this.negativeLogReceptionRate);
			break;	
		case NetworkModel.ROUTING_DISJOINT_BHANDARI_MIN_ETX:
			router = new BhandariAlgorithm(this.network.copyGraph, this.etxTransformer);
			break;
		case NetworkModel.ROUTING_DISJOINT_BHANDARI_MIN_TOTAL_HOPS:
			router = new BhandariAlgorithm(this.network.copyGraph, this.totalHopsTransformer);
			break;
		default:
			throw new RuntimeException("routing method "+routingMethod+" unknown!");
		}
		if (router != null) {
			if (upRoutingFirst) {
				paths1 = this.transformToSubPaths(router.getPathsOfSmallestSumCost(this.source, virtualGW, true, nPaths), true);
			} else {
				paths1 = this.transformToSubPaths(router.getPathsOfSmallestSumCost(virtualGW, this.destination, true, nPaths), false);
			}
			
		}
	
		if (paths1.size() == nPaths) {
			//found n uppaths
			//modified node utilities.
			if (auditNodeUtil) {
				updateUtilizationAfterRouting(nPaths, paths1, upRoutingFirst);
				
				//added by pdv possibly restore end.
				if (removeEnd) {
					this.network.copyGraph.addVertex(end);
					for (Edge edge : edgesOnEnd) {
						if (this.network.copyGraph.containsVertex(edge.getTheOtherNode(end))) {
							this.network.copyGraph.addEdge(edge, edge.getSmallerNode(), edge.getBiggerNode());
						}
					}
				}
				
//				this.network.copyGraph = this.network.copyGraph();
//				this.network.copyGraph.addVertex(virtualGW);
//				//add 100% link quality links betweeen virtual GW and real GWs.
//				for (Node gw : this.network.getGateways()) {
//					this.network.addEdgeToCopyGraph(gw, virtualGW, 1.0);
//				}
				if (!organizeCopyGraphBeforeRouting(nPaths, !upRoutingFirst)) {
					return;
				}	
			
			}
			
			
			List<Subpath> paths2 = null;
			switch (routingMethod) {
			case NetworkModel.ROUTING_DISJOINT_MOST_RELIABLE:
				paths2 = this.getMostReliableVertexDisjointSubpaths(!upRoutingFirst, nPaths);
				break;
			case NetworkModel.ROUTING_DISJOINT_SHORTEST_PATH:
				paths2 = this.getShortestVertexDisjointSubpaths(!upRoutingFirst, nPaths);
				break;
			case NetworkModel.ROUTING_DISJOINT_SHORTEST_PATHS_WITH_DINIC:
				paths2 = this.getShortestVertexDisjointSubpathsDinic(!upRoutingFirst, nPaths);
				break;
			case NetworkModel.ROUTING_DISJOINT_BHANDARI_MIN_SUM_LOSS_RATE:
			case NetworkModel.ROUTING_DISJOINT_BHANDARI_MAX_PROD_REC_RATE:	
			case NetworkModel.ROUTING_DISJOINT_BHANDARI_MIN_ETX:
			case NetworkModel.ROUTING_DISJOINT_BHANDARI_MIN_TOTAL_HOPS:
				if (upRoutingFirst) {
					paths2 = this.transformToSubPaths(
							router.getPathsOfSmallestSumCost(virtualGW, this.destination, true, nPaths), false);
				} else {
					paths2 = this.transformToSubPaths(
							router.getPathsOfSmallestSumCost(this.source, virtualGW, true, nPaths), true);
				}
				break;
			default:
				throw new RuntimeException("routing method "+routingMethod+" unknown!");
			}
			
			if (paths2.size() == nPaths) {
				if (auditNodeUtil) {
					//compute the utilization of all nodes.
					//destination node
					updateUtilizationAfterRouting(nPaths, paths2, !upRoutingFirst);
					
				}
				
				if (checkDeadline) {
					int maxFlow1Hops = this.getMaxHopsAmongPaths(paths1);
					int maxFlow2Hops = this.getMaxHopsAmongPaths(paths2);
					
					if (maxFlow1Hops + maxFlow2Hops > this.deadline) {
						//System.out.println("routing paths too long for period");
						return;
					}
				}
				
				
				for (Subpath path : paths1) {
					this.addSubpath(path, upRoutingFirst);
				}
				
				for (Subpath path : paths2) {
					this.addSubpath(path, !upRoutingFirst);
				}
			}
		}
	}

	/**
	 * 
	 * update utilization after routing of upflow or downflow;
	 * 
	 * @param afterUpRouting
	 */
	private void updateUtilizationAfterRouting(int nPaths,
			List<Subpath> paths, boolean afterUpRouting) {
		double util = 1.0/period;
		
		if (afterUpRouting) {
//			Node node = this.source;
//			double nodeU = copiedNodeUtils.get(node) - util * nPaths;
//			if (nodeU < 0) {
//				//should not happen!
//				throw new RuntimeException("source node does not have enough util!");
//			}
//			copiedNodeUtils.put(node, nodeU);
			for (Subpath path: paths) {
				Node node;
				double nodeU;
				List<Node> senderNodes = path.getSenderNodes();
				//intermediate nodes.
				for (int i=1; i<senderNodes.size(); i++) {
					node = senderNodes.get(i);
					nodeU = copiedNodeUtils.get(node) - util*2;
					if (nodeU < 0) {
						//this should not happen!
						throw new RuntimeException("intermediate node does not have enough util!");
					}
					copiedNodeUtils.put(node, nodeU);
				}
				
				//gw nodes
				node = path.getLastNode();
				nodeU = copiedNodeUtils.get(node) - util;
				if (nodeU < 0) {
					//should not happen!
					throw new RuntimeException("destination node does not have enough util!");
				}
				copiedNodeUtils.put(node, nodeU);
			}
		} else {
//			Node node = this.destination;
//			double nodeU = copiedNodeUtils.get(node) - util * nPaths;
//			if (nodeU < 0) {
//				//should not happen!
//				throw new RuntimeException("destination node does not have enough util!");
//			}
//			copiedNodeUtils.put(node, nodeU);
			for (Subpath path : paths) {
				Node node;
				double nodeU;
				List<Node> senderNodes = path.getSenderNodes();
				//gateway node, node0 is virtual GW.
				node = senderNodes.get(0);
				nodeU = copiedNodeUtils.get(node) - util;
				if (nodeU < 0) {
					//should not happen!
					throw new RuntimeException("gateway node does not have enough util!");
				}
				copiedNodeUtils.put(node, nodeU);
				//intermediate node.
				for (int i=1; i<senderNodes.size(); i++) {
					node = senderNodes.get(i);
					nodeU = copiedNodeUtils.get(node) - util*2;
					if (nodeU < 0) {
						//this should not happen!
						throw new RuntimeException("intermediate node "+node+" does not have enough util!");
					}
					copiedNodeUtils.put(node, nodeU);
				}
			}
		}
		
	}

	
	
	/**
	 * Organize the copy graph before doing the downflow routing.
	 * remove nodes without enough utilization left.
	 * 
	 * @return false if the routing can not be successful.
	 * nodesRemoved: number of nodes removed except source or destination. 
	 * 
	 * @param nPaths
	 *
	 */
	private int nodesRemoved;
	
	private List<Node> nodesToRemove = new ArrayList<Node>();
	
	private boolean organizeCopyGraphBeforeRouting(int nPaths, boolean beforeUpflowRouting) {
		double util = 1.0/period;
		int numValidGWs = 0;
		nodesRemoved = 0;
		nodesToRemove.clear();
		if (beforeUpflowRouting) {
			for (Node node : this.network.copyGraph.getVertices()) {
				double nodeU;
				if (node == virtualGW) {
					//do nothing.
				} else if (node == this.source) {
					//the source and destination (two different nodes) should have at least 
					//remaining utilization of nPaths*util
//					nodeU = copiedNodeUtils.get(node);
//					if (nodeU < util * nPaths) {
//						//definitely unschedulable
//						//System.out.println("the source does not have enough utilization left!");
//						return false;
//					}
				} else if (node.isGateway()) {
					//if a node is a gateway, it should have at least remaining utilization of util
					nodeU = copiedNodeUtils.get(node);
					if (nodeU < util) {
						nodesToRemove.add(node);
						nodesRemoved++;
					} else {
						numValidGWs++;
					}
				} else {
					//other nodes should have at least remaining utilization of util*2;
					nodeU = copiedNodeUtils.get(node);
					if (nodeU < util*2) {
						nodesToRemove.add(node);
						if (node != this.destination) {
							nodesRemoved++;
						}
					}
				}
			}
		} else {
			for (Node node : this.network.copyGraph.getVertices()) {
				double nodeU;
				if (node == virtualGW) {
					//do nothing.
				} else if (node == this.destination) {
					//the source and destination (two different nodes) should have at least 
					//remaining utilization of nPaths*util
//					nodeU = copiedNodeUtils.get(node);
//					if (nodeU < util * nPaths) {
//						//definitely unschedulable
//						//System.out.println("the destination does not have enough utilization left!");
//						return false;
//					}
				} else if (node.isGateway()) {
					//if a node is a gateway, it should have at least remaining utilization of util
					nodeU = copiedNodeUtils.get(node);
					if (nodeU < util) {
						nodesToRemove.add(node);
						nodesRemoved++;
					} else {
						numValidGWs++;
					}
				} else {
					//other nodes should have at least remaining utilization of util*2;
					nodeU = copiedNodeUtils.get(node);
					if (nodeU < util*2) {
						nodesToRemove.add(node);
						if (node != this.source) {
							nodesRemoved++;
						}
					}
				}
			}
		}
		if (numValidGWs < nPaths) {
			//System.out.println("not enough GWs available!");
			return false;
		}
		
		for (Node n : nodesToRemove) {
			this.network.copyGraph.removeVertex(n);
		}
		return true;
	}
	
	private int getMaxHopsAmongPaths(List<Subpath> paths) {
		int max = 0;
		for (Subpath path : paths) {
			int len = path.getEdges().size();
			if (len > max) {
				max = len;
			}
		}
		return max;
	}
	
	
	
	private UndirectedSparseGraph<Node, Edge> copyGraph() {
		UndirectedSparseGraph<Node, Edge> originalGraph = this.network.copyGraph;
		UndirectedSparseGraph<Node, Edge> graph = new UndirectedSparseGraph<Node, Edge>();
		for (Node node : originalGraph.getVertices()) {
			graph.addVertex(node);
		}
		for (Edge edge : originalGraph.getEdges()) {
			Pair<Node> ends = originalGraph.getEndpoints(edge);
			graph.addEdge(edge, ends);
		}
		return graph;
	}
	 
	/**
	 * Here we use the Dinic's algorithm to find the max number of vertex/edge
	 * disjoint shortest paths (in number of hops). 
	 * 
	 * Find K paths by running Dinic's algorithm any number of times until all paths 
	 * are collected or no more path is available. For paths of the same hops, more
	 * reliable ones are preferred. 
	 * 
	 */
	private List<Subpath> getShortestVertexDisjointSubpathsDinic(boolean upflow, int nPaths) {
		UndirectedSparseGraph<Node, Edge> graph = this.copyGraph();
		int pathsLeft = nPaths;
		List<Subpath> ret = new ArrayList<Subpath>();
		while (pathsLeft > 0) {
			DinicAlgorithm algo = new DinicAlgorithm(graph);
			List<List<Node>> pathsNodes;
			if (upflow)
				pathsNodes = algo.getShortestPaths(this.source, this.virtualGW, true);
			else
				pathsNodes = algo.getShortestPaths(this.virtualGW, this.destination, true);
			//order the subpaths by reliability (decreasingly)
			List<Subpath> paths = convertToSubpathsAndOrderByReliability(pathsNodes, upflow);
			if (paths.size() == 0)
				break;
			int pathsAdded = Math.min(pathsLeft, paths.size());
			for (int i=0; i<pathsAdded; i++) {
				Subpath subpath = paths.get(i); 
				ret.add(subpath);
				List<Node> senders = subpath.getSenderNodes(); 
				//remove vertices
				if (upflow) {
					for (Node node : senders.subList(1, senders.size())) {
						graph.removeVertex(node);
					}
					graph.removeVertex(subpath.getLastNode());
				} else {
					for (Node node : senders) {
						graph.removeVertex(node);
					}
				}
			}
			pathsLeft -= pathsAdded;
		}
		return ret;
	}
	
	
	
	private Comparator<Subpath> decSubpathReliability = new Comparator<Subpath>() {

		@Override
		public int compare(Subpath p1, Subpath p2) {
			if (p1.getReliability() > p2.getReliability())
				return -1;
			if (p1.getReliability() < p2.getReliability())
				return 1;
			return 0;
		}
		
	};
	
	private List<Subpath> convertToSubpathsAndOrderByReliability(List<List<Node>> pathsNodes, boolean upflow) {
		List<Subpath> ret = new ArrayList<Subpath>();
		if (pathsNodes != null) {
			for (List<Node> path : pathsNodes) {
				ret.add(this.transformToSubpath(path, upflow));
			}
			if (ret.size() >= 2) {
				Collections.sort(ret, decSubpathReliability);
			}
		}
		return ret;
	}

	
	public List<Subpath> getUpflowPaths() {
		return this.upflows;
	}
	
	public List<Subpath> getDownflowPaths() {
		return this.downflows;
	}
	/**
	 * Set the period of a flow.
	 */
	public void setPeriod(int period) {
		this.period = period;
	}
	
	/**
	 * Get the period.
	 */
	public int getPeriod() {
		return this.period;
	}
	
	/**
	 * Random a period in the range.
	 * @param lowIndex lower bound inclusive. The value is index of 2.
	 * @param highIndex upper bound inclusive. The value is index of 2.
	 */
	public void randomPeriod(int lowIndex, int highIndex) {
		int lower = 1 << lowIndex;
		int upper = 1 << highIndex;
		this.period = Global.randomGen.nextInt(upper-lower+1) + lower;
	}
	/**
	 * Set the deadline of a flow.
	 */
	public void setDeadline(int deadline) {
		this.deadline = deadline;
	}
	/**
	 * Get the deadline of a flow.
	 */
	public int getDeadline() {
		return this.deadline;
	}

	
	/**
	 * Return the minimum slots to let the flow be possible schedulable.
	 * 
	 * @return as the network uses wait-at-GWs model, then it is the same as the longest path.
	 * 
	 */
	public int getMinDeadlineLength() {
		return this.getMaxUpstreamHops() + this.getMaxDownstreamHops();
	}
	
	public static int getMinDeadlineLength(List<Subpath> uppaths, List<Subpath> downpaths) {
		return getMaxPathHops(uppaths) + getMaxPathHops(downpaths);
	}
	
	/**
	 * Random a deadline uniformly distributed between longest path to period-1 (both inclusive).
	 */
	public void randomDeadline() {
		int minDeadline = this.getMinDeadlineLength();
		deadline = Global.randomGen.nextInt(this.period - minDeadline) + minDeadline;
	}
	
	/**
	 * Set the deadline to be the same as the period.
	 */
	public void implicitDeadline() {
		deadline = this.period;
	}
	
	/**
	 * Check whether there is at least 1 path.
	 * @return
	 */
	public boolean hasPath() {
		return (this.upflows.size() > 0 && this.downflows.size() > 0);
	}
	
	public static int getAllPathHops(List<Subpath> uppaths, List<Subpath> downpaths) {
		int sum = 0;
		for (Subpath path : uppaths) {
			sum += path.getEdges().size();
		}
		for (Subpath path : downpaths) {
			sum += path.getEdges().size();
		}
		return sum;
	}
	
	/**
	 * @return the sum hops of all paths.
	 */
	public int getAllPathHops() {
		return getAllPathHops(this.upflows, this.downflows);
	}
	
	/**
	 * 
	 * @return the maximum utilization (upper bound) of a flow.
	 */
	public double getMaximumUtilization() {
		int minDeadline = getMinDeadlineLength();		
		int hops = this.getAllPathHops();
		return ((double)hops) / minDeadline;
	}
	
	/**
	 * 
	 * @return the maximum length of the upstream paths.
	 */
	public int getMaxUpstreamHops() {
		return getMaxPathHops(this.upflows);
	}
	
	public static int getMaxPathHops(List<Subpath> paths) {
		int len = 0;
		for (Subpath path : paths) {
			int hops = path.getEdges().size();
			if (hops > len) {
				len = hops;
			}
		}
		return len;
	}
	/**
	 * 
	 * @return the maximum length of the downstream paths.
	 */
	public int getMaxDownstreamHops() {
		return getMaxPathHops(this.downflows);
	}
	
	/**
	 * 
	 * @return the effective deadline of the upstream.
	 * 
	 * It is equal to the deadline - max_downstream_hop
	 */
	public int getUpflowEffectiveDeadline() {
		int val = this.deadline - this.getMaxDownstreamHops();
		if (val < this.getMaxUpstreamHops()) {
			throw new IllegalStateException("the deadline is not enough for finishing upstream and downstream transmissions");
		}
		return val;
	}
	
	/**
	 * 
	 * @return the effective deadline of the downstream.
	 */
	public int getDownflowEffectiveDeadline() {
		return this.deadline;
	}
	
	public int getSubflowEffectiveDeadline(boolean upflow) {
		if (upflow) {
			return this.getUpflowEffectiveDeadline();
		}
		return this.getDownflowEffectiveDeadline();
	}
	
	public int getMaxSubflowDeadline(boolean upFlow) {
		if (upFlow) {
			return this.deadline - this.getMaxDownstreamHops();
		}
		return this.deadline - this.getMaxUpstreamHops();
	}

	public int getSubpathLength(boolean upFlow, int subpathId) {
		return getSubpath(upFlow, subpathId).getEdges().size();
	}

	public Subpath getSubpath(boolean upFlow, int subpathId) {
		List<Subpath> subpaths;
		if (upFlow) {
			subpaths = this.upflows;
		} else {
			subpaths = this.downflows;
		}
		return subpaths.get(subpathId);
		
	}
	
	public List<Subpath> getSubpaths(boolean upflow) {
		if (upflow)
			return this.upflows;
		return this.downflows;
	}
		
	/**
	 * if max(upflow)+max(downflow) > deadline, return true.
	 * else return false.
	 * @return
	 */
	public boolean isDeadlineTooSmall() {
		if (this.getMaxUpstreamHops() + this.getMaxDownstreamHops() > this.deadline) {
			//System.err.printf("%d + %d > %d\n", this.getMaxUpstreamHops(), this.getMaxDownstreamHops(), this.deadline);
			return true;
		}
		return false;
	}

	public double getUtil() {
		int hops = 0;
		for (Subpath subpath : this.getUpflowPaths()) {
			hops += subpath.getEdges().size();
		}
		for (Subpath subpath : this.getDownflowPaths()) {
			hops += subpath.getEdges().size();
		}
		return (double)hops / this.period;
	}
	/**
	 * Calculate the reliablity of flow under the 1P model.
	 * @return
	 */
	public double getReliability1P() {
		if (this.upflows.size() != this.downflows.size()) {
			throw new RuntimeException("the upflow and downflow should have the same number of paths");
		}
		double r = 1.0;
		for (Subpath upPath : this.upflows) {
			Subpath matchedDownPath = null;
			for (Subpath downPath: this.downflows) {
				if (upPath.getLastNode() == downPath.getFirstNode()) {
					matchedDownPath = downPath;
					break;
				}
			}
			r *= (1.0 - upPath.getReliability() * matchedDownPath.getReliability());
		}
		return 1.0 - r;
		
	}
	
	/**
	 * Calculate the reliability of the flow under the 2P model.
	 */
	public double getReliability2P() {
		double r1 = 1.0;
		for (Subpath path : this.upflows) {
			r1 *= (1.0 - path.getReliability());
		}
		r1 = 1.0 - r1;
		
		double r2 = 1.0;
		for (Subpath path : this.downflows) {
			r2 *= (1.0 - path.getReliability());
		}
		r2 = 1.0 - r2;
		return r1*r2;
	}

	/**
	 * Set flow id in a global array.
	 */
	public void setFlowId(int flowId) {
		this.flowId = flowId;
	}
	
	public int getFlowId() {
		return this.flowId;
	}
}
