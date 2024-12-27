package de.seemoo.dyuan.eval;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import de.seemoo.dyuan.netgen.NetworkModel;
import de.seemoo.dyuan.netgen.UniformGen;
import de.seemoo.dyuan.scheduler.HeuristicScheduler;
import de.seemoo.dyuan.scheduler.ScheduleResult;
import de.seemoo.dyuan.scheduler.ScheduleResult.MemStats;
import de.seemoo.dyuan.scheduler.Schedulers;
import de.seemoo.dyuan.utils.Global;

/**
 * Evaluate the effect of opportunistic aggregate.
 * @author dyuan
 *
 */
public class OpportunisticAggregate {

	private final int numPathsPerFlow = 2;
	private final int[] all_channels = {1, 2, 4, 8, 16};
	
	private final Type type = Type.PhysicalModel;
	private final boolean implicitDeadline = false;
	
	private final int numSensors = 100;
	private final int numGWs = 2;
	private final int numTopologies = 100;
	private final int numFlowsToTry = 5;
	private final int numUtilsToTry = 10;
	private final int maxFlows = numSensors / 2;
	private final int maxUtil = 25;
	
	private int channels;
	
	
	public static enum Type {
		RandomModel, PhysicalModel;
	}
	
	private NetworkModel network;
	
	
	public void doExperiment() throws Exception {
		System.out.println("model: "+this.type);
		System.out.println("implicit deadline: "+this.implicitDeadline);
		
		for (int i=0; i<numTopologies; i++) {
			System.err.println("topo "+i);
			System.out.println("topo "+i);
			network = new NetworkModel();
			UniformGen gen;
			if (type.equals(Type.RandomModel)) {
				gen = new UniformGen(network, Settings.getCommunicationRange(true), Settings.getLinkQualityLB(), Settings.getLinkQualityUB());
			} else {
				gen = new UniformGen(network, Settings.getLinkQualityLB(), Settings.getLinkQualityUB());
			}
			
			gen.addSensors(numSensors, Settings.getSpaceSide(type.equals(Type.PhysicalModel), true));
			gen.buildEdgeAndRandomLQ();
			gen.addSectorCenterGateways(numGWs, false);

			for (int a = 0; a < numFlowsToTry; a++) {
				int nflows = Global.randomGen.nextInt(this.maxFlows)+1;
				//generate n flows.
				gen.randomNFlows(nflows);
				
				network.doVertexDisjointRouting(numPathsPerFlow, NetworkModel.ROUTING_DISJOINT_MOST_RELIABLE);
				
				System.out.printf("e=%.1f, flows=%d\n", nflows*2.0/network.getNormalNodes().size(), network.getNonEmptyFlows().size());
				for (int b = 0; b < numUtilsToTry; b++) {
					double util = Global.randomGen.nextDouble()*maxUtil;
					//distribute total expected utilization among flows. 
					boolean notValidUtil = network.uUniFastSettingPeriods(util, this.implicitDeadline, false);
					if (notValidUtil) {
						System.out.printf("no valid period setting for util %.3f flow %d\n", util, network.getNonEmptyFlows().size());
						continue;
					}
						
					double actualUtil = network.getTotalUtilization();
					if (this.implicitDeadline) {
						network.implicitDeadlines();
					} else {
						network.randomDeadlines();
					}
					System.out.printf("u=%.3f, au=%.3f\n", util, actualUtil);
					//set different channel counts and schedule.
					for (int j=0; j<all_channels.length; j++) {
						this.channels = all_channels[j];
						System.out.printf("channels=%d\n", channels);
						runOnce();
					}
					
				}
				
			}
					
		}
		
	}
	
	
	private void runOnce() throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Class clazz = Schedulers.best_scheduler_class;
		Constructor constructor = clazz.getConstructor(NetworkModel.class, boolean.class, boolean.class, int.class);
		boolean[] piggyback_types = {false, true};
		for (boolean piggyback : piggyback_types) {
			System.out.println("piggyback "+piggyback);
			HeuristicScheduler scheduler = (HeuristicScheduler) constructor.newInstance(network, piggyback, false, channels);
			long diff = System.currentTimeMillis();
			ScheduleResult ret = scheduler.schedule();
			diff = System.currentTimeMillis() - diff;
			//ret.validate();
			System.out.print(scheduler.getName()+": ");
			System.out.println(ret.getStatusStr());
			if (ret.getStatus() == ScheduleResult.FEASIBLE) {
				MemStats stats = ret.getMemStats();
				System.out.printf("time %d max-buf %d buf-time-prod %d hyper %d\n", 
						diff, stats.maxBuffer, stats.bufferTimeProduct, scheduler.getHyperPeriod());
				if (piggyback)
					System.out.printf("oa stats: oa-percent %.3f%% oa-max %d\n", 
						scheduler.getPiggybacks() * 100/(double)scheduler.getTransmittedPackets(), scheduler.getMaxPiggybackOnSender());
			}
			System.out.println();
		}
		
	}

	public static void main(String[] args) throws Exception {
		OpportunisticAggregate exp = new OpportunisticAggregate();
		exp.doExperiment();
	}

}
