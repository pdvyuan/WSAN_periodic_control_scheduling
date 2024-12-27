package de.seemoo.dyuan.eval;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.seemoo.dyuan.netgen.Flow;
import de.seemoo.dyuan.netgen.NetworkModel;
import de.seemoo.dyuan.netgen.UniformGen;
import de.seemoo.dyuan.utils.Global;

public class EffectOfPlacement {
	
	private final int side = 500;
	private final int numPathsPerFlow = 2;
	private final int[] all_channels = {1, 2, 4, 8, 16};
	private int channels;
	private final double commRange = 100;
	private final boolean implicitDeadline = true;
	
	private final int numSensors = 100;
	private final int numGWs = 4;
	private final int numTopologies = 100;
	
	private boolean gwReliable;
	
	
	private NetworkModel network;
	
	private static Map<Integer, String> gwPlaceIDName;
	private static Map<String, Integer> gwPlaceNameID;
	
	private static boolean debug = false;
	
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
		for (int i=0; i<numTopologies; i++) {
			System.out.println("topo "+i);
			UniformGen gen;
			while (true) {
				network = new NetworkModel();
				gen = new UniformGen(network, commRange, Settings.getLinkQualityLB(), Settings.getLinkQualityUB());
				gen.addSensors(numSensors, side);
				gen.buildEdgeAndRandomLQ();
				if (network.check2VertexConnected())
					break;
			}
			OUTER:
			for (int times = 0; times < 10; times++) {
				double endFraction = Global.randomGen.nextDouble();
				System.out.println("GWs="+numGWs);
				System.out.println("GWType=sector-center");
				gen.addSectorCenterGateways(numGWs, false);		
				//generate n flows.
				gen.randomFlows(endFraction);
				List<Flow> flows = network.getFlows();
				if (flows.size() == 0)
					continue;
				gwReliable = false;
				System.out.println("gwReliable="+gwReliable);
				network.doVertexDisjointRouting(numPathsPerFlow, NetworkModel.ROUTING_DISJOINT_MOST_RELIABLE);
				//for each flow try to generate two upflow paths and downflow paths with the naive-n-paths method.
				for (int j=0; j<flows.size(); j++) {
					Flow flow = flows.get(j);
					if (!flow.hasPath()) {
						System.out.println("for the baseline GW placement, 2 disjoint paths cannot be found for a flow");
						continue OUTER;
					}
				}
				System.out.printf("e=%.1f, flows=%d\n", endFraction, network.getNonEmptyFlows().size());
				double util = Global.randomGen.nextDouble();
				//distribute total expected utilization among flows. 
				boolean notValidUtil = network.uUniFastSettingPeriods(16 * util, this.implicitDeadline, false);
				if (notValidUtil) {
					System.out.printf("no valid period setting for util %.3f flow %d\n", 16*util, network.getNonEmptyFlows().size());
					continue;
				}
				//the periods are set. 
				List<Flow> nonEmptyFlows = network.getNonEmptyFlows();
				double actualUtil = network.getTotalUtilization();
				if (this.implicitDeadline) {
					network.implicitDeadlines();
				} else {
					network.randomDeadlines();
				}
				System.out.printf("u=%.3f, au=%.3f\n", util * 16, actualUtil);
				//set different channel counts and schedule.
				System.out.printf("reliability=%.3f\n", network.getReliability());
				for (int j=0; j<all_channels.length; j++) {
					this.channels = all_channels[j];
					System.out.printf("channels=%d\n", channels);
					runOnce();
				}
					//sector-center 0, 
					//min-sum-dist 1, 
					//cluster-min-sum-dist 2,
					//min-sum-square-dist 3, 
					//cluster-min-sum-square-dist 4,
					//center
				int totalTypes;
				if (this.numGWs == 2) {
					totalTypes = 7;
				} else {
					totalTypes = 6;
				}
				int distanceComputingType = UniformGen.DISTANCE_COMPUTING_ALL_NODES;
				for (int type = 0; type < totalTypes; type++) {
					if (type == 0) {
						
					} else if (type == 1) {
						gen.addSameGatewaysMinSumDistToGWs(numGWs, false, distanceComputingType);
					} else if (type == 2) {
						gen.addGatewaysMinSumDistToNearestGW(numGWs, false, distanceComputingType);
					} else if (type == 3) {
						gen.addSameGatewaysMinSumSquareDistToGWs(numGWs, false, distanceComputingType);
					} else if (type == 4) {
						gen.addGatewaysMinSumSquareDistToNearestGW(numGWs, false, distanceComputingType);
					} else if (type == 5) {
						gen.addGatewaysInTheCenter(numGWs, false);
					} else {
						if (numGWs != 2)
							throw new RuntimeException("numGWs must be 2");
						gen.addGatewaysSpectralPartitioning(false);
					}
					String placement = getTypeNameFromId(type);
					System.out.println("GWType="+placement);
					for (boolean reliable : new boolean[] {false}) {
						gwReliable = reliable;
						if (!reliable && type == 0)
							continue;
						System.out.println("gwReliable="+gwReliable);
						
						network.doVertexDisjointRouting(numPathsPerFlow, NetworkModel.ROUTING_DISJOINT_MOST_RELIABLE);
						
						int flowsWithRouting = network.getNonEmptyFlows().size(); 
						boolean routingFailed = false;
						if (flowsWithRouting != nonEmptyFlows.size()) {
							routingFailed = true;
							System.out.printf("expected route generation for placement %s, GW-reliable=%b, %d flows, actual %d\n", 
									placement, reliable, nonEmptyFlows.size(), flowsWithRouting);
						}
						actualUtil = network.getTotalUtilization();
						//the expected utilization is not correct.
						System.out.printf("u=%.3f, au=%.3f\n", util * 16, actualUtil);
						//set different channel counts and schedule.
						System.out.printf("reliability=%.3f\n", network.getReliability());
						for (int j=0; j<all_channels.length; j++) {
							this.channels = all_channels[j];
							System.out.printf("channels=%d\n", channels);
							if (routingFailed) {
								System.out.print("EDZL: unschedulable\n");
							} else {
								runOnce();
							}
						}
					}
				}
					
			}
		}
		
	}
	
	private void runOnce() throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException  {
//		Class clazz = Schedulers.best_scheduler_class;
//		Constructor constructor = clazz.getConstructor(NetworkModel.class, int.class);
//		SchedulerBase scheduler = (SchedulerBase) constructor.newInstance(network, channels);
//		ScheduleResult ret = scheduler.schedule();
//		if (debug)
//			ret.validate();
//		boolean feasible = (ret.getStatus() == ScheduleResult.FEASIBLE);
//		System.out.print(scheduler.getName()+": ");
//		if (feasible) {
//			System.out.println("feasible");
//		} else {
//			System.out.println("unschedulable");
//		}
	}

	public static void main(String[] args) throws Exception {
		EffectOfPlacement exp = new EffectOfPlacement();
		exp.doExperiment();
	}

}
