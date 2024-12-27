package de.seemoo.dyuan.eval;

import de.seemoo.dyuan.netgen.Flow;
import de.seemoo.dyuan.netgen.NetworkModel;
import de.seemoo.dyuan.netgen.UniformGen;
import de.seemoo.dyuan.utils.Global;

public class CompareRoutingReliability {

	private final int numPathsPerFlow = 2;
	
	private final Type type = Type.RandomModel;
	
	private final int numSensors = 100;
	private final int numGWs = 2;
	private final int numTopologies = 100;
	private final int numFlowsToTry = 1;
	private int maxFlows = numSensors / 2;
	
	public static enum Type {
		RandomModel, PhysicalModel;
	}
	
	private NetworkModel network;
	
	
	public void doExperiment(int lqLB, int lqUB) throws Exception {
		System.err.println("model: "+this.type);
		System.out.println("1P,2P");
		
		for (int i=0; i<numTopologies; i++) {
			System.err.println("topo "+i);
			System.err.println("topo "+i);
			network = new NetworkModel();
			UniformGen gen;
			if (type.equals(Type.RandomModel)) {
				gen = new UniformGen(network, Settings.getCommunicationRange(true), lqLB, lqUB);
			} else {
				gen = new UniformGen(network, lqLB, lqUB);
			}
			
			gen.addSensors(numSensors, Settings.getSpaceSide(type.equals(Type.PhysicalModel), true));
			gen.buildEdgeAndRandomLQ();
			gen.addSectorCenterGateways(numGWs, false);
			
			for (int a=0; a<this.numFlowsToTry; a++) {
				int nflows = Global.randomGen.nextInt(this.maxFlows)+1;
				//generate n flows.
				gen.randomNFlows(nflows);
				//network.doVertexDisjointRouting(numPathsPerFlow, NetworkModel.ROUTING_DISJOINT_SHORTEST_PATH);
				network.doVertexDisjointRouting(numPathsPerFlow, NetworkModel.ROUTING_DISJOINT_MOST_RELIABLE);
				System.err.printf("e=%.1f, flows=%d\n", nflows*2.0/network.getNormalNodes().size(), 
						network.getNonEmptyFlows().size());
				for (int f = 0; f < network.getNonEmptyFlows().size(); f++) {
					Flow flow = network.getNonEmptyFlows().get(f);
					double r1 = flow.getReliability1P();
					double r2 = flow.getReliability2P();
					System.err.printf("flow %d: r(1P) = %f, r(2P) = %f \n", f, r1, r2);
					System.out.printf("%f,%f\n", r1, r2);
					
				}
			}
			
					
		}
		
	}
	

	public static void main(String[] args) throws Exception {
		CompareRoutingReliability exp = new CompareRoutingReliability();
		int lqLB = Integer.parseInt(args[0]);
		int lqUB = Integer.parseInt(args[1]);
		exp.doExperiment(lqLB, lqUB);
	}

}
