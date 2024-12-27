package de.seemoo.dyuan.netgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.seemoo.dyuan.netgen.structure.Edge;
import de.seemoo.dyuan.netgen.structure.Node;
import de.seemoo.dyuan.scheduler.DeadlineType;

/**
 * The class of subpath. A path is composed of two subpaths --
 * the upstream subpath and the downstream subpath.
 * @author dyuan
 *
 */
public class Subpath implements Route {
	
	private List<Edge> path;
	
	private List<Node> senderNodes;
	
	private Flow parent;
	
	private boolean isUpflow;
	
	private final Node firstNode;
	
	private final Node lastNode;
	
	private int subpathId;
	
	public Subpath(List<Edge> path, Node firstNode) {
		if (path == null)
			this.path = new ArrayList<Edge>();
		else
			this.path = path;
		this.firstNode = firstNode;
		this.senderNodes = new ArrayList<Node>();
		Node sender;
		sender = firstNode;
		senderNodes.add(sender);
		for (int i=0; i<this.path.size(); i++) {
			Edge edge = path.get(i);
			if (i != 0) {
				senderNodes.add(sender);
			} 
			sender = edge.getTheOtherNode(sender);			
		}
		this.lastNode = sender;
	}
	
	public Node getFirstNode() {
		return this.firstNode;
	}
	
	public Node getLastNode() {
		return this.lastNode;
	}
	
	public List<Node> getSenderNodes() {
		return this.senderNodes;
	}
	
	public List<Edge> getEdges() {
		return this.path;
	}
	
	/**
	 * Compute the reliability of a path.
	 * 
	 */
	public double getReliability() {
		return getSubPathReliability(this.path);
	}
	
	public static double getSubPathReliability(List<Edge> path) {
		if (path == null || path.size() == 0)
			return 0;
		double reliability = 1.0;
		for (Edge edge : path) {
			reliability *= edge.getLinkQuality();
		}
		return reliability;
	}
	
	public static double getSubPathReliability(List<Edge> path, Set<Node> excludedNodes) {
		if (path == null || path.size() == 0)
			return 0.0;
		double reliability = 1;
		for (Edge edge : path) {
			if (excludedNodes.contains(edge.getBiggerNode()) || excludedNodes.contains(edge.getSmallerNode()))
				return 0.0;
			reliability *= edge.getLinkQuality();
		}
		return reliability;
	}
	
	public static int getSubpathLength(List<Edge> path, Set<Node> excludedNodes) {
		if (path == null || path.size() == 0)
			return Integer.MAX_VALUE;
		for (Edge edge : path) {
			if (excludedNodes.contains(edge.getBiggerNode()) || excludedNodes.contains(edge.getSmallerNode())) {
				return Integer.MAX_VALUE;
			}
		}
		return path.size();
	}
	
	public void setParentFlow(Flow parent) {
		this.parent = parent;
	}
	
	public Flow getParentFlow() {
		return this.parent;
	}
	
	public void setUpflow(boolean isUpflow) {
		this.isUpflow = isUpflow;
	}
	
	public boolean isUpflow() {
		return this.isUpflow;
	}
	
	/**
	 * The different kinds of deadlines are chosen according to the phd thesis
	 * of Jun Sun "Fixed-priority end-to-end scheduling in distributed real-time systems".
	 **/
	

	public int getDeadlineOfType(DeadlineType type) {
		if (type == DeadlineType.SD && this.isUpflow) {
			return this.parent.getUpflowEffectiveDeadline();
		}
		return this.parent.getDeadline();
	}
	
	public int getEffectiveDeadline() {
		if (this.isUpflow) {
			return this.parent.getUpflowEffectiveDeadline();
		}
		return this.parent.getDownflowEffectiveDeadline();
	}
	
//	public double getDeadlineOfType(DeadlineType type) {
//		if (type == DeadlineType.GD) {
//			return this.getGlobalDeadline();
//		} 
//		if (type == DeadlineType.ED) {
//			return this.getEffectiveDeadline();
//		}
//		return this.getProportionalDeadline();
//	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		for (Node sender : this.senderNodes) {
			buf.append(sender.toString()+"-");
		}
		buf.append(this.lastNode.toString());
		return buf.toString();
	}
	
	public void setSubpathId(int id) {
		this.subpathId = id;
	}
	
	public int getSubpathId() {
		return this.subpathId;
	}
}
