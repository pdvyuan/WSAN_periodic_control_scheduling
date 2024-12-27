package de.seemoo.dyuan.netgen.algo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.seemoo.dyuan.netgen.structure.Edge;
import de.seemoo.dyuan.netgen.structure.Node;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

/**
 * The heuristic aims at finding a maximum number of vertex disjoint bounded paths between 
 * two points in a graph.
 * 
 * The heuristic is from the paper "heuristics for finding a maximum number of disjoint bounded paths"
 * by D. Ronen and Y. Perl. Networks, Vol. 14 (1984) 531-544
 * 
 * @author dyuan
 *
 */
public class VertexDisjointBoundedPathHeuristic {
	
	/**
	 * Nodes are of two types:
	 * 1) free nodes. The nodes do not belong to any path, belongPath = null
	 * distance is the shortest distance from the source node till now.
	 * distanceToDestination doesn't make sense in this case.
	 * 
	 * 2) nonfree nodes. The nodes belong to a path in the solution, belongPath != null
	 * distance is the shortest distance from the source node till now.
	 * distanceToDestination is set accordingly.
	 *
	 */
	public static class NodeInfo {
		//the shortest distance from source node till now.
		public int distance;
		public List<Node> belongPath;
		//When the node is a nonfree node, the field contains the distance to the destination.
		public int distanceToDestination;
		public Set<MatchInfo> forbiddenList;
		
		public boolean forbiddenListContains(MatchInfo mi) {
			if (this.forbiddenList != null && this.forbiddenList.contains(mi))
				return true;
			return false;
		}
		
		public void addToForbiddenList(MatchInfo mi) {
			if (this.forbiddenList == null)
				this.forbiddenList = new HashSet<MatchInfo>();
			this.forbiddenList.add(mi);
		}
	}
	
	public static class MatchInfo {
		Node node;
		int distanceFromSource;
		Node exitFromSource;
		
		public boolean equals(Object o) {
			if (!(o instanceof MatchInfo))
				return false;
			MatchInfo mo = (MatchInfo) o;
			return (mo.node == this.node && mo.distanceFromSource == this.distanceFromSource
					&& mo.exitFromSource == this.exitFromSource);
		}
		
		public int hashCode() {
			return this.node.hashCode()*31*31 +this.distanceFromSource*31+this.exitFromSource.hashCode();
		}
		
		public String toString() {
			return node+", "+distanceFromSource+", "+exitFromSource;
		}
	}
	
	public static class StackData {
		int pathId;
		List<Node> replacedPath;
		Node node;
	}
	
	private LinkedList<StackData> stack;

	private UndirectedSparseGraph<Node, Edge> originalGraph;
	
	private int bound;
	
	private List<List<Node>> paths;

	private Node source;
	
	private Node destination;
	
	private Map<Node, NodeInfo> nodesTable;
	
	private List<Node> currentSearchPath;
	
	

	public VertexDisjointBoundedPathHeuristic(UndirectedSparseGraph<Node, Edge> graph) {
		this.originalGraph = graph;
	}
	
	/**
	 * 
	 * @param source the source node.
	 * @param destination the destination node.
	 * @param bound the paths should be no larger than bound.
	 * @return the maximum number of vertex disjoint paths from the source to the destination
	 * with bounded path length.
	 */
	public List<List<Node>> getMaxBoundedPaths(Node source, Node destination, int bound) {
		this.source = source;
		this.destination = destination;
		this.bound = bound;
		DinicAlgorithm finder = new DinicAlgorithm(this.originalGraph);
		this.paths = finder.getVertexDisjointPaths(this.source, this.destination, bound);
		//the first condition is that there is no connection between the two nodes.
		//the second condition is that the shortest distance between the two nodes are larger than bound.
		//paths contains all possible paths we found.
		if (paths.size() == 0) {
			return paths;
		}
		//int oldv = paths.size();
		while (!oneIteration())
			;		
		for (List<Node> path : paths) {
			System.out.println(path);
		}
//		int newv = paths.size();
//		if (newv > oldv)
//			System.err.println("improve");
		return this.paths;
	}


	/**
	 * Set nodes 
	 * @param path
	 */
	private void setPathNodeToNonFree(List<Node> path) {
		for (int i=1; i<path.size()-1; i++) {
			Node node = path.get(i);
			NodeInfo ni = this.nodesTable.get(node);
			ni.belongPath = path;
			ni.distanceToDestination = path.size()-1-i;
		}
	}
	
	private void setPathNodeToNonFree(List<Node> path, int leftInc, int rightInc) {
		for (int i=Math.max(1, leftInc); i<=Math.min(rightInc, path.size()-2); i++) {
			Node node = path.get(i);
			NodeInfo ni = this.nodesTable.get(node);
			ni.belongPath = path;
			ni.distanceToDestination = path.size()-1-i;
		}
	}
	
	private void setPathNodeToFreeAndDistanceFromSource(List<Node> path, int leftInc, int rightInc) {
		for (int i=Math.max(1, leftInc); i<=Math.min(rightInc, path.size()-2); i++) {
			Node node = path.get(i);
			NodeInfo ni = this.nodesTable.get(node);
			ni.distance = i;
			ni.belongPath = null;
		}
	}
	
	public Node getCurrentNode() {
		return this.currentSearchPath.get(this.currentSearchPath.size()-1);
	}
	
	
	/**
	 * In one iteration, the algorithm tries to add one bounded path.
	 * @return true. the algorithm ends.
	 */
	private boolean oneIteration() {
		this.nodesTable = new HashMap<Node, NodeInfo>();
		for (Node node : this.originalGraph.getVertices()) {
			NodeInfo ni = new NodeInfo();
			//source and destination nodes are always free.
			//dist(source) = 0, dist(others) = bound+1.
			ni.belongPath = null;
			if (node == source) {
				ni.distance = 0;
			} else {
				ni.distance = this.bound+1;
			}
			this.nodesTable.put(node, ni);
		}
		//destination node is always free. 
		for (int pathId = 0; pathId < this.paths.size(); pathId++) {
			List<Node> path = this.paths.get(pathId);
			setPathNodeToNonFree(path);
		}	
		
		this.stack = new LinkedList<StackData>();		
		this.currentSearchPath = new ArrayList<Node>();
		this.currentSearchPath.add(this.source);
		
		while (true) {
			int currentDist; 
			if ((currentDist = this.nodesTable.get(getCurrentNode()).distance) < this.bound) {
				boolean hasFreeNode = false;	
				
				boolean isNextHopDestination = this.originalGraph.getNeighbors(getCurrentNode()).contains(destination);
				if (isNextHopDestination) {
					//check for single hop path. if there is a single hop path, it is already found.
					if (this.currentSearchPath.size() != 1) {
						//we have found a new path.
						this.currentSearchPath.add(destination);
						this.paths.add(this.currentSearchPath);
						//printPaths();
						//this.setPathNodeToNonFree(this.currentSearchPath);
						return false;
					}
				} 
				for (Node node : this.originalGraph.getNeighbors(getCurrentNode())) {
					if (node != destination) {
						NodeInfo ni = this.nodesTable.get(node);
						if (ni.belongPath == null && ni.distance > currentDist + 1) {
							hasFreeNode = true;
							this.currentSearchPath.add(node);
							ni.distance = currentDist + 1;
							hasFreeNode = true;
							break;
						}
					}
				}
				
				if (!hasFreeNode) {
					//try to match, we do not match at the source node.
					if (getCurrentNode() == this.source || !doMatch()) {
						boolean ended = backTrack();
						if (ended)
							return true;
					}
					// if matched continue DFS.
				}
			} else {
				boolean ended = backTrack();
				if (ended)
					return true;
			}
		}
		
		
		
	}
	
	private void printPaths() {
		System.err.println("----------------------------");
		for (List<Node> path : this.paths) {
			System.err.println(path);
		}
		System.err.println("----------------------------");
	}

	/**
	 * @return true. if algorithm ended, else, go to DFS.
	 */
	private boolean backTrack() {
		if (getCurrentNode() == this.source) {
			//the algorithm has backtracked to source.
			if (stack.size() > 0) {
				StackData data = stack.removeLast();
				//node w.
				Node commonNode = data.node;
				//exchange path.
				int pathId = data.pathId;
				List<Node> oldPath = this.paths.get(pathId);
				List<Node> newPath = data.replacedPath;
				//the second part.
				int commonPartLen = this.nodesTable.get(commonNode).distanceToDestination;
				//set the first part of old path to free.
				this.setPathNodeToFreeAndDistanceFromSource(oldPath, 0, oldPath.size()-1-commonPartLen-1);
				//set the first part of the new path to nonfree.
				this.setPathNodeToNonFree(newPath);
				this.paths.set(pathId, newPath);
				this.currentSearchPath = oldPath;
				for (int i=0; i<commonPartLen+1; i++) {
					this.currentSearchPath.remove(this.currentSearchPath.size()-1);
				}
			} else {
				return true;
			}
		} else {
			//the current node is not source, backtrace.
			this.currentSearchPath.remove(this.currentSearchPath.size()-1);
		}
		return false;
	}

	/**
	 * 
	 * @return true can do a match at the currentNode.
	 */
	private boolean doMatch() {
		//triplet(v, dist(v), path-id(v))
		MatchInfo mi = new MatchInfo();
		mi.distanceFromSource = this.currentSearchPath.size()-1;
		mi.node = getCurrentNode();
		mi.exitFromSource = this.currentSearchPath.get(1);
		
		for (Node node : this.originalGraph.getNeighbors(getCurrentNode())) {
			NodeInfo ni = this.nodesTable.get(node);
			/**
			 * node is a nonfree node, the path "s - currentNode - node- t" is of bounded length
			 * and currentNode is not in the forbidden list of node "node".
			 */
			if (ni.belongPath != null && this.currentSearchPath.size()-1+ni.distanceToDestination < this.bound
					&& !ni.forbiddenListContains(mi)) {
				List<Node> oldPath = ni.belongPath;
				//assertInPaths(oldPath);
				//triplet(z, dist(z), path-id(z))
				MatchInfo replacedPath = new MatchInfo();
				replacedPath.exitFromSource = oldPath.get(1);
				replacedPath.distanceFromSource = oldPath.size()-1-ni.distanceToDestination-1;
				replacedPath.node = oldPath.get(replacedPath.distanceFromSource);

				for (int i=replacedPath.distanceFromSource+1; i<oldPath.size(); i++) {
					//the new path going to substitute the old. extend the new path.
					this.currentSearchPath.add(oldPath.get(i));
				}
				//set new path to nonfree
				this.setPathNodeToNonFree(this.currentSearchPath);
				
				int pathId = replacePath(oldPath, this.currentSearchPath);
				StackData data = new StackData();
				data.pathId = pathId;
				//node w.
				data.node = node;
				data.replacedPath = oldPath;
				stack.addLast(data);
				//set old path to free.
				this.setPathNodeToFreeAndDistanceFromSource(oldPath, 0, replacedPath.distanceFromSource);
				
				ni.addToForbiddenList(mi);
				ni.addToForbiddenList(replacedPath);
				
				//build a new search path, which is the first part of the replaced path.
				this.currentSearchPath = new ArrayList<Node>();
				for (int i=0; i<=replacedPath.distanceFromSource; i++) {
					this.currentSearchPath.add(oldPath.get(i));
				}
				return true;
			} 
		}
		return false;
	}

	private void assertInPaths(List<Node> belongPath) {
		for (List<Node> path : this.paths) {
			if (belongPath == path)
				return;
		}
		throw new IllegalStateException("should do not happen.");
	}

	/**
	 * replace the old path in all paths with the new one.
	 * @return path index.
	 */
	private int replacePath(List<Node> oldPath,
			List<Node> newPath) {
		if (oldPath == newPath)
			throw new IllegalArgumentException("two paths should not be the same.");
//		System.out.println("old "+oldPath.hashCode()+":"+oldPath);
//		System.out.println("new "+newPath.hashCode()+":"+newPath);
		for (int i=0; i<this.paths.size(); i++) {
			if(paths.get(i) == oldPath) {
				paths.set(i, newPath);
				return i;
			}
		}
		throw new IllegalArgumentException("should not happen!");
	}

}
