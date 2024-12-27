package de.seemoo.dyuan.netgen.structure;

import java.util.Formatter;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import de.seemoo.dyuan.netgen.NetworkModel;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

/**
 * An edge is represented by two nodes. 
 * Always the smaller is the first and the bigger is the second.
 * 
 * @author dyuan
 *
 */
public class Edge implements ReliabilityCost {
	
	private static long global_index = 0;
	
	private double linkQuality;
	
	
	private Set<Edge> neighbors;
	
	private Node node1;
	private Node node2;
	
	private final long index;
	
	private final double reliabilityCost;
	
	public Edge(Node n1, Node n2, double lq) {		
		if (n1.getIndex() < n2.getIndex()) {
			this.node1 = n1;
			this.node2 = n2;
		} else {
			this.node1 = n2;
			this.node2 = n1;
		}
		this.linkQuality = lq;
		this.index = global_index;
		global_index++;
		this.reliabilityCost = -Math.log(this.linkQuality);
		
	}
	
	public double getLinkQuality() {
		return this.linkQuality;
	}
	
	/**
	 * toString prints the link quality.
	 */
	public String toString() {
		Formatter formatter = new Formatter();
		formatter.format(Locale.US, "%.2f", this.linkQuality);
		return formatter.toString();
	}
	/**
	 * 
	 * @return the node with the smaller index.
	 */
	public Node getSmallerNode() {
		return this.node1;
	}
	
	/**
	 * 
	 * @return the node with the larger index.
	 */
	public Node getBiggerNode() {
		return this.node2;
	}
	
	/**
	 * Check whether an edge has two ends of n1 and n2.
	 * The sequence of the two nodes are arbitrary. 
	 */
	public boolean hasEnds(Node n1, Node n2) {
		return (this.node1.equals(n1) && this.node2.equals(n2)) || 
				(this.node1.equals(n2) && this.node2.equals(n1));
	}
	
	/**
	 * description in the format of n1.id-n2.id.
	 */
	public String description() {
		return this.node1.toString()+"-"+this.node2.toString();
	}
	
	long getIndex() {
		return this.index;
	}
	/**
	 * Given a node of an edge, give the other node.
	 * 
	 */
	public Node getTheOtherNode(Node node) {
		if (this.node1.equals(node))
			return this.node2;
		if (this.node2.equals(node))
			return this.node1;
		throw new RuntimeException("edge "+this.description()+" doesn't have node "+node);
	}
	
	
	public boolean equals(Object o) {
		return (o instanceof Edge && ((Edge)o).index == this.index);
	}
	
	public int hashCode() {
		return (int) this.index;
	}
	
	public boolean isConflict(Edge o) {
		return (o.getSmallerNode().equals(this.getSmallerNode()) || o.getBiggerNode().equals(this.getSmallerNode())
				|| o.getSmallerNode().equals(this.getBiggerNode()) || o.getBiggerNode().equals(this.getBiggerNode()));
	}
	

	public boolean hasNode(Node n) {
		return this.node1.equals(n) || this.node2.equals(n);
	}
	
	private int transmissionsLeft;
	
	public void setTransmissionsLeft(int times) {
		this.transmissionsLeft = times;
	}
	
	public int getTransmissionsLeft() {
		return this.transmissionsLeft;
	}
	
	public void decTransmissionsLeft() {
		this.transmissionsLeft--;
	}
	
	public void incTransmissionsLeft() {
		this.transmissionsLeft++;
	}
	
	public void buildNeighbors(NetworkModel network) {
		UndirectedSparseGraph<Node, Edge> graph = network.getGraph();
		this.neighbors = new HashSet<Edge>();
		neighbors.addAll(graph.getIncidentEdges(this.getBiggerNode()));
		neighbors.addAll(graph.getIncidentEdges(this.getSmallerNode()));
		neighbors.remove(this);
	}
	
	public Set<Edge> getNeighbors() {
		return this.neighbors;
	}
	
	public double getReliabilityCost() {
		return this.reliabilityCost;
	}
	
}
