package de.seemoo.dyuan.netgen;

import java.util.ArrayList;
import java.util.List;

import de.seemoo.dyuan.netgen.structure.Edge;
import de.seemoo.dyuan.netgen.structure.Node;

/**
 * Generator a line topology. The link quality is perfect (1). 
 * 
 * Suppose there are n nodes in total (including sensor nodes and gateway), which are numbered
 * 0, 1, ... n-1
 * We add M gateways.
 * Then gateway k (k = 0, 1, ...) is at node floor((n-1)*(k+0.5)/M). 
 * 
 * @author dyuan
 *
 */
public class LineGen extends NetworkGenerator {
	
	public LineGen(NetworkModel model) {
		super(model);
	}
	
	/**
	 * Add number of sensor nodes.
	 *
	 */
	public void addSensors(int nNodes) {
		for (int i=1; i<=nNodes; i++) {
			Node node = new Node(""+i);
			network.addNormalNode(node);
		}
	}
	
	/**
	 * Build edges forming a line-topology. Set LQ to 1;
	 * 
	 */
	public void buildEdgesAndSetLQ() {
		for (int i=0; i<this.network.normalNodes.size()-1; i++) {
			Node node1 = this.network.normalNodes.get(i);
			Node node2 = this.network.normalNodes.get(i+1);
			network.addEdge(node1, node2, 1);
		}
	}
	
	/**
	 * add n gateways
	 * 
	 */
	public void addGateways(int nGateway) {
		List<Node> allNodes = new ArrayList<Node>();
		int nextCopy = 0;
		for (int i=0; i<nGateway; i++) {
			int nodeId = (int) Math.ceil((network.normalNodes.size()-1)*(i+0.5)/nGateway);
			Node gateway = new Node((i+1)+"");
			network.addGateway(gateway);
			for (int j=nextCopy; j < network.normalNodes.size() && j < nodeId; j++) {
				allNodes.add(network.normalNodes.get(j));
			}
			allNodes.add(gateway);
			nextCopy = nodeId;
		}
		for (int j=nextCopy; j<network.normalNodes.size(); j++) {
			allNodes.add(network.normalNodes.get(j));
		}
		//clear all edges
		for (int i=0; i<network.normalNodes.size(); i++) {
			Node node = network.normalNodes.get(i);
			for (Edge edge : network.graph.getIncidentEdges(node)) {
				network.graph.removeEdge(edge);
			}
		}
		
		for (int i=0; i<allNodes.size()-1; i++) {
			Node node1 = allNodes.get(i);
			Node node2 = allNodes.get(i+1);
			network.addEdge(node1, node2, 1);
		}
	}
	

}
