package de.seemoo.dyuan.eval.routing;

import java.util.List;

import de.seemoo.dyuan.eval.Settings;
import de.seemoo.dyuan.netgen.Flow;
import de.seemoo.dyuan.netgen.NetworkModel;
import de.seemoo.dyuan.netgen.UniformGen;
import de.seemoo.dyuan.netgen.UniformGenSetting;
import de.seemoo.dyuan.scheduler.ScheduleResult;
import de.seemoo.dyuan.scheduler.dynamic_priority.busy_first.LST_MCFScheduler;
import de.seemoo.dyuan.utils.Global;

/**
 * Compare different routing methods. Link model is the random link model.
 *
 */
public class RoutingComparison {
	
	
	private final int numSensors = 100;
	private final int maxFlows = numSensors/2;
	private final int numPaths = 2;
	
	private int topoId;
	
	private int feasibleProblems;
	
	private NetworkModel network;
	
	private boolean realistic = false;
	
	private static int randomFlowsForEachTopo = 10;

	private static int[] numGWs_to_evaluate = {2, 4};

	
	private static final int FEASIBLE_PROBLEM_COUNT = 1000;
	
	
	public void doExperiment() {
		topoId = 0;
		feasibleProblems = 0;
		System.out.println("physical model: "+realistic);
		UniformGenSetting settings;
		if (realistic) {
			settings = Settings.REAL_SMALL_NET_HIGH_CONN;
		} else {
			settings = Settings.DISK_SMALL_NET_HIGH_CONN;
		}
		for (;;) {
			System.err.println("topo "+topoId);
			System.out.println("topo "+topoId);
			topoId++;
			network = new NetworkModel();
			UniformGen gen;
			if (realistic) {
				gen = new UniformGen(network, Settings.getLinkQualityLB(), Settings.getLinkQualityUB());
			} else {
				gen = new UniformGen(network, settings.commRange, Settings.getLinkQualityLB(), Settings.getLinkQualityUB());
			}
			// add 100 nodes.
			gen.addSensors(numSensors, settings.side);
		
			//generate random link qualities
			gen.buildEdgeAndRandomLQ();
			
			//generate 10 random flows.
			for (int times = 0; times < randomFlowsForEachTopo; times++) {
				int nflows = Global.randomGen.nextInt(this.maxFlows)+1;
				gen.randomNFlows(nflows);
				List<Flow> flows = network.getFlows();
			
				//harmonious period. implicit deadline
				for	(Flow flow : flows) {
					//period at least 8, at most 8192
					int minIndex = 2;
					int maxIndex = NetworkModel.harmonious_periods.length;
					int index = Global.randomGen.nextInt(maxIndex - minIndex) + minIndex;
					int period = NetworkModel.harmonious_periods[index];
					flow.setPeriod(period);
					flow.implicitDeadline();
				}
				
				for (int numGWs : numGWs_to_evaluate) {
					//add GWs to grid center.
					gen.addSectorCenterGateways(numGWs, false);
					System.out.println("GWs="+numGWs);
					
					int checkFeasible = network.checkFeasibility(numPaths, numGWs, 16); 
					if (checkFeasible != NetworkModel.FEASIBLE) {
						System.out.println("network infeasible "+checkFeasible);
						continue;
					}

					//generate 2 disjoint paths.
					System.out.println("numPaths "+numPaths);

					int[] routingTypes_to_evaluate = {1, 2, 3, 4, 5, 6, 7, 8, 9};
					
					for (int routingType : routingTypes_to_evaluate) {
						for (double maxNodeUtil : new double[] {1.0, 0.5}) {
							System.out.println("max-util "+maxNodeUtil);
							network.setMaxNodeUtil(maxNodeUtil);
							long diff = System.nanoTime();
							network.doVertexDisjointRouting(numPaths, routingType);
							diff = System.nanoTime() - diff;
							System.out.printf("routing-type %d: flows %d with-paths %d mean-reliability %.3f mean-hops %.3f time %d util %.3f\n", 
									routingType, flows.size(), network.getNonEmptyFlows().size(), network.computeMeanWeightedFlowReliability(), 
									network.computeMeanWeightedFlowHops(), diff, network.getTotalUtilization());
							if (network.getNonEmptyFlows().size() == flows.size()) {
								if (schedule(network)) {
									System.out.println("node util ub "+maxNodeUtil+" feasible");
								}
							} else {
								System.out.printf("routing-failure\n");
							}
						}
					}
					
					feasibleProblems++;
					if (feasibleProblems == FEASIBLE_PROBLEM_COUNT) {
						return;
					}					
				}			
			}			
		}
		
	}

	/**
	 * 
	 * @param network
	 * @return true if feasible, false if not.
	 */
	private boolean schedule(NetworkModel network)  {
		boolean feasible = false;
		for (int ch = this.network.getMinChannelsForNonpiggybackScheduling(); ch <= 16; ch++) {
			LST_MCFScheduler scheduler = new LST_MCFScheduler(network, false, true, ch);
			System.out.println("channel count "+ch);
			ScheduleResult ret = scheduler.schedule();
			if (ret.getStatus() == ScheduleResult.FEASIBLE) {
				feasible = true;
			}
			ret.printSimple();
		}		
		return feasible;
	}

	public static void main(String[] args) throws Exception {
		RoutingComparison exp = new RoutingComparison();
		exp.doExperiment();
	}

}
