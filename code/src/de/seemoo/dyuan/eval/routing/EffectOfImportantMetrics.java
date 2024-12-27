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
public class EffectOfImportantMetrics {
	
	
	private final int numSensors = 100;
	private final int numTopologies = 50;
	private final int maxFlows = numSensors/2;
	
	private NetworkModel network;
	
	private boolean realistic = false;
	
	
	public void doExperiment() {
		System.out.println("physical model: "+realistic);
		UniformGenSetting settings;
		if (realistic) {
			settings = Settings.REAL_SMALL_NET_HIGH_CONN;
		} else {
			settings = Settings.DISK_SMALL_NET_HIGH_CONN;
		}
		for (int i=0; i<numTopologies; i++) {
			System.err.println("topo "+i);
			System.out.println("topo "+i);
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
			for (int times = 0; times < 10; times++) {
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
				
				int[] numGWs_to_evaluate = {2, 4};
				for (int numGWs : numGWs_to_evaluate ) {
					//add GWs to grid center.
					gen.addSectorCenterGateways(numGWs, false);
					System.out.println("GWs="+numGWs);

					//generate 2 disjoint paths.
					int numPaths = 2;
					System.out.println("numPaths "+numPaths);

					int[] routingTypes_to_evaluate = {NetworkModel.ROUTING_DISJOINT_BHANDARI_MIN_ETX, NetworkModel.ROUTING_DISJOINT_BF_MOST_RELIABLE};
					for (int routingType : routingTypes_to_evaluate) {
						for (int metric : new int[] {Flow.METRIC_TOTAL_HOPS, Flow.METRIC_EFFECTIVE_DEADLINE}) {
							System.out.println("metric "+metric);
							Flow.setImportantMetric(metric);
							long diff = System.nanoTime();
							network.doVertexDisjointRouting(numPaths, routingType);
							diff = System.nanoTime() - diff;
							System.out.printf("routing-type %d: flows %d with-paths %d mean-reliability %.3f mean-hops %.3f time %d\n", 
									routingType, flows.size(), network.getNonEmptyFlows().size(), network.computeMeanWeightedFlowReliability(), 
									network.computeMeanWeightedFlowHops(), diff);
							
							if (network.getNonEmptyFlows().size() == flows.size()) {
								schedule(network);
							} else {
								System.out.printf("routing-failure\n");
							}
						}
						
					}
				}
			}
		}
		
	}

	private void schedule(NetworkModel network)  {
		for (int ch = 1; ch <= 16; ch++) {
			LST_MCFScheduler scheduler = new LST_MCFScheduler(network, false, true, ch);
			System.out.println("channel count "+ch);
			ScheduleResult ret = scheduler.schedule();
			ret.printSimple();
		}		
	}

	public static void main(String[] args) throws Exception {
		EffectOfImportantMetrics exp = new EffectOfImportantMetrics();
		exp.doExperiment();
	}

}
