package de.seemoo.dyuan.eval;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import de.seemoo.dyuan.netgen.Flow;
import de.seemoo.dyuan.netgen.NetworkModel;
import de.seemoo.dyuan.netgen.UniformGen;
import de.seemoo.dyuan.scheduler.ScheduleResult;
import de.seemoo.dyuan.scheduler.SchedulerBase;
import de.seemoo.dyuan.scheduler.Schedulers;
import de.seemoo.dyuan.utils.Global;

public class EffectOfNumGWs {
	
	private final int side = 500;
	private final int numPathsPerFlow = 2;
	private final int[] all_channels = {1, 2, 4, 8, 16};
	private int channels;
	private final double commRange = 100;
	private final boolean implicitDeadline = false;
	
	private final int numSensors = 100;
	private final int[] other_GWs = {4, 6};
	private int numGWs = 2;
	private final int numTopologies = 100;
	
	
	private NetworkModel network;
	
	public void doExperiment() throws Exception {
		for (int i=0; i<numTopologies; i++) {
			System.out.println("topo "+i);
			network = new NetworkModel();
			UniformGen gen;
			gen = new UniformGen(network, commRange, Settings.getLinkQualityLB(), Settings.getLinkQualityUB());
			gen.addSensors(numSensors, side);
			gen.buildEdgeAndRandomLQ();
			
			for (int times = 0; times < 10; times++) {
				double endFraction = Global.randomGen.nextDouble();
				numGWs = 2;
				System.out.println("GWs="+numGWs);
				gen.addSectorCenterGateways(numGWs, false);		
				//generate n flows.
				gen.randomFlows(endFraction);
				List<Flow> flows = network.getFlows();
				if (flows.size() == 0)
					continue;
				//for each flow try to generate two upflow paths and downflow paths with the naive-n-paths method.
				network.doVertexDisjointRouting(numPathsPerFlow, NetworkModel.ROUTING_DISJOINT_MOST_RELIABLE);
				
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
				for (int j=0; j<all_channels.length; j++) {
					this.channels = all_channels[j];
					System.out.printf("channels=%d\n", channels);
					runOnce();
				}
				
				for (int gw : other_GWs) {
					numGWs = gw;
					System.out.println("GWs="+numGWs);
					((UniformGen)gen).addSectorCenterGateways(gw, false);
					network.doVertexDisjointRouting(numPathsPerFlow, NetworkModel.ROUTING_DISJOINT_MOST_RELIABLE);
					
					int flowsWithRouting = network.getNonEmptyFlows().size(); 
					boolean routingFailed = false;
					if (flowsWithRouting != nonEmptyFlows.size()) {
						routingFailed = true;
						System.out.printf("expected route generation for %d GWs, %d flows, actual %d\n", gw, nonEmptyFlows.size(), flowsWithRouting);
					}
					actualUtil = network.getTotalUtilization();
					//the expected utilization is not correct.
					System.out.printf("u=%.3f, au=%.3f\n", util * 16, actualUtil);
					//set different channel counts and schedule.
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
	
	private void runOnce() throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException  {
		Class clazz = Schedulers.best_scheduler_class;
		Constructor constructor = clazz.getConstructor(NetworkModel.class, boolean.class, boolean.class, int.class);
		SchedulerBase scheduler = (SchedulerBase) constructor.newInstance(network, false, false, channels);
		ScheduleResult ret = scheduler.schedule();
		ret.validate();
		System.out.print(scheduler.getName()+": ");
		if (ret.getStatus() == ScheduleResult.FEASIBLE) {
			System.out.println("feasible");
		} else {
			System.out.println("unschedulable");
		}
	}

	public static void main(String[] args) throws Exception {
		EffectOfNumGWs exp = new EffectOfNumGWs();
		exp.doExperiment();
	}

}
