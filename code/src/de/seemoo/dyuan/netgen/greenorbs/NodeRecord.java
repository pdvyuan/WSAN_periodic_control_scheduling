package de.seemoo.dyuan.netgen.greenorbs;

import java.util.ArrayList;
import java.util.List;

import de.seemoo.dyuan.netgen.structure.Node;

public class NodeRecord {
	
	
	private List<Neighbour> neighbours;
	
	private int parentId;
	
	private double pathEtx = Double.NaN;
	
	private Node node;
	
	public NodeRecord(Node node) {
		this.node = node;
		this.neighbours = new ArrayList<Neighbour>();
	}
	
	public void setParentId(int parentId) {
		this.parentId = parentId;
	}
	
	public int getParentId() {
		return this.parentId;
	}
	
	public void addNeighbour(Neighbour nbr) {
		this.neighbours.add(nbr);
	}
	
	public void clearNeighbours() {
		this.neighbours.clear();
	}
	

	public Node getNode() {
		return this.node;
	}
	
	public double getPathEtx() {
		return this.pathEtx;
	}
	
	public void setPathEtx(double pathEtx) {
		this.pathEtx = pathEtx;
	}
	
}
