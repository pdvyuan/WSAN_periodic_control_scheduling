package de.seemoo.dyuan.netgen.structure;

import java.util.ArrayList;
import java.util.List;

/**
 * A node in the graph.
 * @author dyuan
 *
 */
public class Node {
	
	private static long global_index = 0;
	
	private long index;
	
	protected String id;
	
	private int numPackets;
	
	public static final int UNKNOWN = 0;
	public static final int GATEWAY = 1;
	public static final int NORMAL = 2;
	
	private int type;
	
	private double distanceToDest;
	
	private List<Double> bfDistances;
	private List<Node> bfPredecessors;
	
	public static void clearIndex() {
		global_index = 0;
	}
	
	public Node(String id) {
		this.id = id;
		this.index = global_index;
		global_index++;
		this.type = UNKNOWN;
	}
	
	public String getId() {
		return this.id;
	}
	
	
	public long getIndex() {
		return this.index;
	}
	
	public String toString() {
		String s;
		if (this.type == UNKNOWN)
			s = "u";
		else if (this.type == GATEWAY) 
			s = "g";
		else
			s = "n";
		return s+this.id;
	}
	
	public boolean equals(Object o) {
		return (o instanceof Node && ((Node)o).index == index);
	}
	
	public int hashCode() {
		return (int) this.index;
	}
	
	public int getType() {
		return this.type;
	}
	
	public void setType(int type) {
		this.type = type;
	}
	
	public boolean isGateway() {
		return this.type == GATEWAY;
	}
	
	private boolean sourceDest = false;
	
	public void setSourceDest() {
		this.sourceDest = true;
	}
	
	public void unsetSourceDest() {
		this.sourceDest = false;
	}
	
	public boolean isSourceOrDest() {
		return this.sourceDest;
	}
	
	public int getNumPackets() {
		return this.numPackets;
	}
	
	public void setNumPackets(int n) {
		this.numPackets = n;
	}
	
	public void incNumPackets() {
		this.numPackets++;
		if (this.numPackets > maxBuffer) {
			maxBuffer = this.numPackets;
		}
	}
	
	public void decNumPackets() {
		this.numPackets--;
	}
	
	private static int maxBuffer = 0;
	
	public static int getMaxBuffer() {
		return maxBuffer;
	}
	
	public static void clearMaxBuffer() {
		maxBuffer = 0;
	}
	
	/**
	 * The distance is set to positive infinity if there is no path to destination.
	 * 
	 */
	public void setDistanceToDest(double dist) {
		this.distanceToDest = dist;
	}
	
	public double getDistanceToDest() {
		return this.distanceToDest;
	}
	
	private boolean removed = false;
	
	public void setRemoved(boolean removed) {
		this.removed = removed;
	}
	
	public boolean isRemoved() {
		return this.removed;
	}

	public void initBF(double value) {
		this.bfDistances = new ArrayList<Double>();
		this.bfDistances.add(value);
		this.bfPredecessors = new ArrayList<Node>();
		this.bfPredecessors.add(null);
	}
	
	public double getBFDistance(int hop) {
		return this.bfDistances.get(hop);
	}
	
	
	public Node getBFPredecessor(int hop) {
		return this.bfPredecessors.get(hop);
	}
	
	public void setBFDistanceAndPredecessor(int hop, double distance, Node pred) {
		this.bfDistances.set(hop, distance);
		this.bfPredecessors.set(hop, pred);
	}
	
	public void copyBFHop() {
		int size = this.bfDistances.size();
		double cost = this.bfDistances.get(size-1);
		this.bfDistances.add(size, cost);
		Node pred = this.bfPredecessors.get(size-1);
		this.bfPredecessors.add(size, pred);
	}
	
	public int getBFLastHop() {
		return this.bfDistances.size()-1;
	}
}
