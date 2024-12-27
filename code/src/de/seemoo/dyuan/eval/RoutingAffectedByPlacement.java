package de.seemoo.dyuan.eval;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.seemoo.dyuan.netgen.Flow;
import de.seemoo.dyuan.netgen.NetworkModel;
import de.seemoo.dyuan.netgen.UniformGen;
import de.seemoo.dyuan.utils.Global;

/**
 * The routing protocol finds the 2 most reliable upflow and downflow paths.
 * 
 * How do different GW placement schemes affect the routing? 
 *
 */
public class RoutingAffectedByPlacement {
	
	private final int numPathsPerFlow = 2;
	
	private final int numSensors = 100;
	private final int numTopologies = 100;
	
	private final boolean physical = true;
	
	private boolean gwReliable;
	
	private NetworkModel network;
	
	private static Map<Integer, String> gwPlaceIDName;
	private static Map<String, Integer> gwPlaceNameID;
	
	
	private static void init() {
		gwPlaceIDName = new HashMap<Integer, String>();
		gwPlaceNameID = new HashMap<String, Integer>();
		int typeId = 0;
		String name = "sector-center";
		gwPlaceIDName.put(typeId, name);
		gwPlaceNameID.put(name, typeId);
		
		typeId = 1;
		name = "min-sum-dist";
		gwPlaceIDName.put(typeId, name);
		gwPlaceNameID.put(name, typeId);
		
		typeId = 2;
		name = "cluster-min-sum-dist";
		gwPlaceIDName.put(typeId, name);
		gwPlaceNameID.put(name, typeId);
		
		typeId = 3;
		name = "min-sum-square-dist";
		gwPlaceIDName.put(typeId, name);
		gwPlaceNameID.put(name, typeId);
		
		typeId = 4;
		name = "cluster-min-sum-square-dist";
		gwPlaceIDName.put(typeId, name);
		gwPlaceNameID.put(name, typeId);
		
		typeId = 5;
		name = "center";
		gwPlaceIDName.put(typeId, name);
		gwPlaceNameID.put(name, typeId);
		
		typeId = 6;
		name = "spectral-partition";
		gwPlaceIDName.put(typeId, name);
		gwPlaceNameID.put(name, typeId);
	}
	
	public static String getTypeNameFromId(int id) {
		if (gwPlaceIDName == null)
			init();
		return gwPlaceIDName.get(id);
	}
	
	public static int getTypeIdFromName(String name) {
		if (gwPlaceNameID == null)
			init();
		return gwPlaceNameID.get(name);
	}
	
	
	public void doExperiment() throws Exception {
		System.out.println("physical "+this.physical);
		for (int i=0; i<numTopologies; i++) {
			System.out.println("topo "+i);
			UniformGen gen;
			network = new NetworkModel();
			if (this.physical) {
				gen = new UniformGen(network, Settings.getLinkQualityLB(), Settings.getLinkQualityUB());
			} else {
				gen = new UniformGen(network, Settings.getCommunicationRange(false), Settings.getLinkQualityLB(), Settings.getLinkQualityUB());
			}
			gen.addSensors(numSensors, Settings.getSpaceSide(this.physical, false));
			gen.buildEdgeAndRandomLQ();
			
			for (int times = 0; times < 10; times++) {
				double endFraction = Global.randomGen.nextDouble();
							
				//generate n flows.
				gen.randomFlows(endFraction);
				List<Flow> flows = network.getFlows();
				if (flows.size() == 0)
					continue;
				
				for (Flow flow : flows) {
					//one slot is 10ms, the period is in the range of 100ms to 1000s
					//random period in [10, 100000] ?
					int period = Global.randomGen.nextInt(99991)+10;
					flow.setPeriod(period);
				}
				for (int distanceComputingType : new int[] {UniformGen.DISTANCE_COMPUTING_ALL_NODES, 
						UniformGen.DISTANCE_COMPUTING_END_NODES, UniformGen.DISTANCE_COMPUTING_WEIGHTED_END_NODES} ) {
					System.out.println("distance_computing_type "+distanceComputingType);
					for (int numGWs : new int[]{2, 4, 6}) {
						System.out.println("GWs="+numGWs);
						int totalTypes = 7;
						for (int type = 0; type < totalTypes; type++) {
							if (type == 0) {
								if (distanceComputingType == UniformGen.DISTANCE_COMPUTING_ALL_NODES)
									gen.addSectorCenterGateways(numGWs, false);
								else
									continue;
							} else if (type == 1) {
								gen.addSameGatewaysMinSumDistToGWs(numGWs, false, distanceComputingType);
							} else if (type == 2) {
								gen.addGatewaysMinSumDistToNearestGW(numGWs, false, distanceComputingType);
							} else if (type == 3) {
								gen.addSameGatewaysMinSumSquareDistToGWs(numGWs, false, distanceComputingType);
							} else if (type == 4) {
								gen.addGatewaysMinSumSquareDistToNearestGW(numGWs, false, distanceComputingType);
							} else if (type == 5) {
								if (distanceComputingType == UniformGen.DISTANCE_COMPUTING_ALL_NODES)
									gen.addGatewaysInTheCenter(numGWs, false);
								else
									continue;
							} else {
								if (numGWs == 2 && distanceComputingType == UniformGen.DISTANCE_COMPUTING_ALL_NODES)
									gen.addGatewaysSpectralPartitioning(false);
								else
									continue;
							}
							String placement = getTypeNameFromId(type);
							System.out.println("GWType="+placement);
							gwReliable = false;
							System.out.println("gwReliable="+gwReliable);
							
							network.doVertexDisjointRouting(numPathsPerFlow, NetworkModel.ROUTING_DISJOINT_BHANDARI_MIN_ETX);
							
							System.out.printf("flows %d with-paths %d mean-reliability %.3f mean-weighted-reliability %.3f mean-hops %.3f mean-weighted-hops %.3f\n", 
								flows.size(), network.getNonEmptyFlows().size(), network.computeMeanFlowReliability(), 
								network.computeMeanWeightedFlowReliability(), network.computeMeanFlowHops(), network.computeMeanWeightedFlowHops());
						}
					}
					
				}
			}
		}
		
	}

	public static void main(String[] args) throws Exception {
		RoutingAffectedByPlacement exp = new RoutingAffectedByPlacement();
		exp.doExperiment();
	}

}
