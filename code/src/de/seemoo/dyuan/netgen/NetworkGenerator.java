package de.seemoo.dyuan.netgen;

import java.util.ArrayList;
import java.util.List;

import de.seemoo.dyuan.netgen.structure.Edge;
import de.seemoo.dyuan.netgen.structure.Node;
import de.seemoo.dyuan.utils.Global;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

public class NetworkGenerator {
	
	protected final NetworkModel network;
	
	public NetworkGenerator(NetworkModel model) {
		this.network = model;
		model.setNetworkGenerator(this);
		//node id starts from 0 and so on.
		Node.clearIndex();
	}

	/**
	 * Create random flows.
	 * @param endFrac a number in [0, 1]. source and destination fraction. Both nodes of a pair of source and destination nodes
	 * are normal nodes. endFrac of nodes are either source or destination. Both the source and destination nodes
	 * amount to half of the nodes.
	 * 
	 */
	public void randomFlows(double endFrac) {
		network.clearFlows();
		int pairs = (int) (endFrac * network.getNormalNodes().size() / 2);
		List<Node> tmpNormalNodes = new ArrayList<Node>(network.getNormalNodes()); 
		for (int i=0; i<pairs; i++) {
			int index = Global.randomGen.nextInt(tmpNormalNodes.size());
			Node src = tmpNormalNodes.get(index);
			tmpNormalNodes.remove(index);
			
			index = Global.randomGen.nextInt(tmpNormalNodes.size());
			Node dest = tmpNormalNodes.get(index);
			tmpNormalNodes.remove(index);
			network.newFlow(src, dest);		
			src.setSourceDest();
			dest.setSourceDest();
		}
	}
	
	public void randomNFlows(int num) {
		network.clearFlows();
		List<Node> tmpNormalNodes = new ArrayList<Node>(network.getNormalNodes()); 
		for (int i=0; i<num; i++) {
			int index = Global.randomGen.nextInt(tmpNormalNodes.size());
			Node src = tmpNormalNodes.get(index);
			tmpNormalNodes.remove(index);
			
			index = Global.randomGen.nextInt(tmpNormalNodes.size());
			Node dest = tmpNormalNodes.get(index);
			tmpNormalNodes.remove(index);
			network.newFlow(src, dest);					
			src.setSourceDest();
			dest.setSourceDest();
		}
	}
	
}
