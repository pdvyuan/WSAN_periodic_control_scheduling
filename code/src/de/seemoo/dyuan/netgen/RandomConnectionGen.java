package de.seemoo.dyuan.netgen;

import de.seemoo.dyuan.netgen.structure.Node;
import de.seemoo.dyuan.utils.Global;

/**
 * Generate n nodes, m gateways. A node is randomly connected with other nodes.
 * The link qualities are assigned randomly. 
 * 
 * 
 * @author dyuan
 *
 */
public class RandomConnectionGen extends NetworkGenerator {
	
	private double edgeDensity;
	
	private int linkQualityLB;
	
	private int linkQualityUB;
	
	public RandomConnectionGen(NetworkModel network) {
		super(network);
	}
	
	/**
	 * 
	 * Random edges according to an edge density and assign random link qualities to the edges.
	 * @param edgeDensity edge density, a number in [0, 1]
	 * @param lbLQ Link quality lower bound, a number in [0, 100]. the bound is inclusive.
	 * @param ubLQ Link quality upper bound, a number in [0, 100]. the bound is inclusive.
	 * 
	 */
	public void buildEdgeAndRandomLQ(double edgeDensity, int lbLQ, int ubLQ) {
		this.edgeDensity = edgeDensity;
		this.linkQualityLB = lbLQ;
		this.linkQualityUB = ubLQ;
		int delta = ubLQ - lbLQ;
		//outgoing link;
		for (int i=0; i<this.network.normalNodes.size(); i++) {
			Node node1 = this.network.normalNodes.get(i);
			for (int j=i+1; j<this.network.normalNodes.size(); j++) {
				Node node2 = this.network.normalNodes.get(j);
				if (Global.randomGen.nextDouble() <= edgeDensity) {
					double lq = (lbLQ + Global.randomGen.nextInt(delta+1)) / (100f);
					network.addEdge(node1, node2, lq);
				}
			}
		}
	}
	
	
	/**
	 * Set the number of normal sensor nodes
	 * @param nNodes
	 */
	public void addSensors(int nNodes) {
		for (int i=1; i<=nNodes; i++) {
			Node node = new Node(""+i);
			network.addNormalNode(node);
		}
	}
	
	/**
	 * add n gateways, gateways are randomly put.
	 * 
	 * Their connectivity with the normal nodes is set in the same way for any normal node.
	 * 
	 * @param nGateway
	 * 
	 */
	public void addGateways(int nGateway) {
		
		for (int i=1; i<=nGateway; i++) {
			Node gateway = new Node(i+"");
			network.addGateway(gateway);
		}
		
		int delta = this.linkQualityUB - this.linkQualityLB;
		for (Node gateway : network.gateways) {
			for (int i=0; i<this.network.normalNodes.size(); i++) {
				Node node = this.network.normalNodes.get(i);
				if (Global.randomGen.nextDouble() <= edgeDensity) {
					double lq = (this.linkQualityLB + Global.randomGen.nextInt(delta+1)) / (100f);
					network.addEdge(gateway, node, lq);
				}
			}
		}
	}

}
