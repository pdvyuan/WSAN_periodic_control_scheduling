package de.seemoo.dyuan.netgen.algo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.seemoo.dyuan.netgen.structure.Edge;
import de.seemoo.dyuan.netgen.structure.Node;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

public class MostReliablePairDisjointPath extends KDAlgorithm {
	
	private BellmanFordAlgorithm<KDEdge> bfAlgo;

	public MostReliablePairDisjointPath(UndirectedSparseGraph<Node, Edge> graph, Node source, Node destination, int bound) {
		//Integer.MAX_VALUE
		super(graph, source, destination, bound, 100);
		this.bfAlgo = new BellmanFordAlgorithm<KDEdge>();
	}
	
	public double computeReliability(List<Node> path1, List<Node> path2) {
		double r = Math.exp(-this.computePathCost(path1));
		r += (1-r)*Math.exp(-this.computePathCost(path2));
		return r;
	}

	
	/**
	 * The algorithm will quit anyhow when the computation time is over 100 sec.
	 * 
	 */
	public DisjointPairPath findMostReliablePair() {
		
		this.prepareSteps();
		if (graph.getOutEdges(this.source).size() < 2 || graph.getInEdges(this.destination).size() < 2) {
			//System.out.println("impossible to find 2 disjoint paths.");
			return null;
		}
		
		List<Node> nextShortestPath;
		DisjointPairPath res = null;
		OUTER:
		while ((nextShortestPath = this.getNextLooplessShortestPath()) != null) {
			List<Node> disjointPath = bellmanFordDisjointPath(nextShortestPath);
			if (disjointPath == null) {
				continue;
			}
			double reliability = this.computeReliability(nextShortestPath, disjointPath);
			res = new DisjointPairPath(nextShortestPath, disjointPath, reliability);
			
			while ((nextShortestPath = this.getNextLooplessShortestPath()) != null) {
				double r = this.computeReliability(nextShortestPath, nextShortestPath);
				if (res.getReliability() >= r) {
					break OUTER;
				}
				disjointPath = bellmanFordDisjointPath(nextShortestPath);
				if (disjointPath == null) {
					continue;
				} 
				r = this.computeReliability(nextShortestPath, disjointPath);
				if (r > res.getReliability()) {
					res = new DisjointPairPath(nextShortestPath, disjointPath, r);
				} 
			}
			break;			
			
		}
//		if (this.k == this.pathCount) {
//			if (res != null) {
//				System.out.println("PAIR PATH FOUND1");
//			}
//			System.out.println("PATH COUNT REACHED!XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
//		}
		return res;
	}


	private List<Node> bellmanFordDisjointPath(List<Node> path1) {
		//remove nodes to get disjoint path
		Set<Node> excludedNodes = new HashSet<Node>(path1.subList(1, path1.size()-1));
		return this.bfAlgo.findShortestPathOfLengthConstraints(excludedNodes, 
				graph, source, destination, this.bound, new EdgeCostTransformer<KDEdge>() {
					@Override
					public double getCost(KDEdge e) {
						return e.getReliabilityCost();
					}
		});
	}

}
