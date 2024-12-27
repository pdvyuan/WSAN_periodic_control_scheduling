package de.seemoo.dyuan.netgen.algo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import de.seemoo.dyuan.netgen.structure.Node;
import edu.uci.ics.jung.graph.Graph;

public class BellmanFordAlgorithm<E> {
	
	public BellmanFordAlgorithm() {
		
	}
	
	
	public List<Node> findShortestPathOfLengthConstraints(Set<Node> excludedNodes, 
			Graph<Node, E> graph, Node source, Node destination, int maxLength,
			EdgeCostTransformer<E> trans) {
		//remove nodes to get disjoint path
		for (Node node : excludedNodes) {
			node.setRemoved(true);
		}
		
		List<Node> nodesUnremoved = new ArrayList<Node>();
		for (Node node : graph.getVertices()) {
			if (!excludedNodes.contains(node)) {
				nodesUnremoved.add(node);
				if (node != source) {
					node.initBF(Double.MAX_VALUE);
				} else {
					node.initBF(0);
				}
			}			
		}
		destination.initBF(Double.MAX_VALUE);
		
		int iterations = Math.min(maxLength, graph.getVertexCount()-1);
		for (int i=0; i<iterations; i++) {
			boolean changed = false;
			for (Node node : nodesUnremoved) {
				node.copyBFHop();
				Collection<E> inEdges = graph.getInEdges(node);				
				for (E edge : inEdges) {
					Node n1 = graph.getOpposite(node, edge);
					if (!n1.isRemoved() && n1.getBFDistance(i) != Double.MAX_VALUE) {
						double dist = n1.getBFDistance(i) + trans.getCost(edge);
						if (node.getBFDistance(i+1) > dist) {
							node.setBFDistanceAndPredecessor(i+1, dist, n1);
							changed = true;
						}
					}
				}
			}
			if (!changed) {
				break;
			}
		}
		
		
		List<Node> path;
		int lastHop = destination.getBFLastHop();
		if (destination.getBFDistance(lastHop) == Double.MAX_VALUE) {
			path = null;
		} else {
			path = new ArrayList<Node>();
			Node node = destination;
			do {
				path.add(0, node);
				node = node.getBFPredecessor(lastHop--);
			} while (node != source);
			path.add(0, source);
		}
		
		for (Node node : excludedNodes) {
			node.setRemoved(false);
		}
		
		return path;
	}

}
