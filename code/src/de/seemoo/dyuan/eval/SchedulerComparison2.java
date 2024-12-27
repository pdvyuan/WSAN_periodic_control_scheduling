package de.seemoo.dyuan.eval;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import de.seemoo.dyuan.netgen.Flow;
import de.seemoo.dyuan.netgen.NetworkModel;
import de.seemoo.dyuan.netgen.UniformGen;
import de.seemoo.dyuan.scheduler.HeuristicScheduler;
import de.seemoo.dyuan.scheduler.ScheduleResult;
import de.seemoo.dyuan.scheduler.ScheduleResult.MemStats;
import de.seemoo.dyuan.scheduler.Schedulers;
import de.seemoo.dyuan.utils.Global;

public class SchedulerComparison2 {

	private final int numPathsPerFlow = 2;
	private final int[] all_channels = {1, 2, 4, 8, 16};
	
	private final Type type = Type.PhysicalModel;
	private final boolean implicitDeadline = false;
	
	private final int numSensors = 100;
	private final int numGWs = 2;
	private final int numTopologies = 100;
	private final int numFlowsToTry = 5;
	private final int numUtilsToTry = 10;
	private int maxFlows = numSensors / 2;
	
	private int channels;
	
	public static enum Type {
		RandomModel, PhysicalModel;
	}
	
	private NetworkModel network;
	
	
	public void doExperiment() throws Exception {
		System.out.println("model: "+this.type);
		System.out.println("implicit: "+this.implicitDeadline);
		
		for (int i=0; i<numTopologies; i++) {
			System.out.println("topo "+i);
			System.err.println("topo "+i);
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
			
			for (int a=0; a<this.numFlowsToTry; a++) {
				int nflows = Global.randomGen.nextInt(this.maxFlows)+1;
				//generate n flows.
				gen.randomNFlows(nflows);
				network.doVertexDisjointRouting(numPathsPerFlow, NetworkModel.ROUTING_DISJOINT_MOST_RELIABLE);
				System.out.printf("e=%.1f, flows=%d\n", nflows*2.0/network.getNormalNodes().size(), 
						network.getNonEmptyFlows().size());
				for (int b=0; b<this.numUtilsToTry; b++) {
					double util = Global.randomGen.nextDouble();
					//distribute total expected utilization among flows. 
					boolean notValidUtil = network.uUniFastSettingPeriods(16 * util, this.implicitDeadline, false);
					if (notValidUtil) {
						System.out.printf("no valid period setting for util %.3f flow %d\n", 16*util, network.getNonEmptyFlows().size());
						continue;
					}
						
					double actualUtil = network.getTotalUtilization();
					if (this.implicitDeadline) {
						network.implicitDeadlines();
					} else {
						network.randomDeadlines();
					}
					System.out.printf("u=%.3f, au=%.3f\n", util * 16, actualUtil);
					//set different channel counts and schedule.
					for (int j=0; j<all_channels.length; j++) {
						channels = all_channels[j];
						System.out.printf("channels=%d\n", channels);
						runOnce();
					}
					
				}
				
			}
					
		}
		
	}
	
	private void runOnce() throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Class[] schedulers = Schedulers.schedulers_classes;
		for (int i = 0; i < schedulers.length; i++) {
			Class clazz = schedulers[i];
			Constructor constructor = clazz.getConstructor(NetworkModel.class, boolean.class, boolean.class, int.class);
			HeuristicScheduler scheduler = (HeuristicScheduler) constructor.newInstance(network, false, false, channels);
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
			} 
			System.out.println();
		}
	}

	public static void main(String[] args) throws Exception {
		SchedulerComparison2 exp = new SchedulerComparison2();
		exp.doExperiment();
	}

}
