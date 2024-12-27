package de.seemoo.dyuan.netgen.algo;

import java.util.List;

import de.seemoo.dyuan.netgen.structure.Node;

public class DisjointPairPath {
	
	private List<Node> path1;
	
	private List<Node> path2;
	
	private double reliability;
	
	public DisjointPairPath(List<Node> path1, List<Node> path2, double reliability) {
		this.path1 = path1;
		this.path2 = path2;
		this.reliability = reliability;
	}
	
	public List<Node> getPath1() {
		return this.path1;
	}
	
	public List<Node> getPath2() {
		return this.path2;
	}
	
	public double getReliability() {
		return this.reliability;
	}

}
