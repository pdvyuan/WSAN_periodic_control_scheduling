package de.seemoo.dyuan.netgen.algo;

import java.util.List;

import de.seemoo.dyuan.netgen.structure.Node;

public interface KDAlgoCallback {
	
	public void callback(List<Node> path, double pathReliability);
	
}
