package de.seemoo.dyuan.scheduler.dynamic_priority.epd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.seemoo.dyuan.netgen.Flow;
import de.seemoo.dyuan.netgen.NetworkModel;
import de.seemoo.dyuan.netgen.Subpath;
import de.seemoo.dyuan.scheduler.ActiveTransmission;
import de.seemoo.dyuan.scheduler.FlowTransmissionState;
import de.seemoo.dyuan.scheduler.HeuristicScheduler;

/**
 * 
 * A dynamic priority scheduling.
 * 
 * Earliest proportional deadline schedules a transmission based on
 * its packet's absolute sub-deadline defined, at every slot, as its remaining 
 * time divided by the remaining number of transmissions.
 * 
 * 
 * Two task models:
 * 		
 * 	the upstream/downstream transmissions of a subpath is defined as a task.	
 * 
 * This works for both shortSchedule or not.
 *   
 * 
 * @author dyuan
 *
 */
public class EPDScheduler extends HeuristicScheduler {

	public EPDScheduler(NetworkModel network, boolean piggyback, boolean shortSchedule, int nChannels) {
		super(network, piggyback, shortSchedule, nChannels);
	}
	
	public static class EPDActiveTransmission extends ActiveTransmission {
		public double subDeadline;
	}
	
	private Comparator<EPDActiveTransmission> transmissionsComparator = new Comparator<EPDActiveTransmission>() {

		@Override
		public int compare(EPDActiveTransmission t1, EPDActiveTransmission t2) {
			if (t1.subDeadline < t2.subDeadline)
				return -1;
			if (t1.subDeadline > t2.subDeadline)
				return 1;
			return 0;
		}
		
	};

	@Override
	protected List<? extends ActiveTransmission> collectAndOrderPotentialTransmissions(
			int slot) {
		List<EPDActiveTransmission> potentialTransmissions = collectPotentialTransmissions(slot);
		Collections.sort(potentialTransmissions, this.transmissionsComparator);
//		System.out.println(slot+":");
//		for (EPDActiveTransmission trans : potentialTransmissions) {
//			System.out.printf("f%d p%d s=%.3f\n", trans.flowId, trans.subpathId, trans.subDeadline);
//		}
		return potentialTransmissions;
	}
	
	/**
	 * Works for both shortSchedule or not.
	 */
	protected List<EPDActiveTransmission> collectPotentialTransmissions(int slot) {
		
		List<EPDActiveTransmission> potentialTransmissions = new ArrayList<EPDActiveTransmission>();
		for (int flowId = 0; flowId < this.flowStates.size(); flowId++) {
			FlowTransmissionState flowState = this.flowStates.get(flowId);
			if (flowState.isFinished())
				continue;
			Flow flow = flowState.getFlow();
			int numSubpaths = flowState.getPendingTransmissionStates().length;
			for (int subpathId = 0; subpathId < numSubpaths; subpathId++) {
				if (flowState.getPendingTransmissionStates()[subpathId] == FlowTransmissionState.STATE_TRANSMISSION_RELEASED) {
					Subpath subpath = flow.getSubpath(flowState.isInUpflow(), subpathId); 	
					int hop = flowState.getPendingTransmissions()[subpathId];
					EPDActiveTransmission at = new EPDActiveTransmission();
					this.initActiveTransmission(flowState, subpath, hop, at);
					
					int deadline = flow.getSubflowEffectiveDeadline(flowState.isInUpflow());
					int period = flowState.getPeriodId();
					int packetRT = period * flow.getPeriod();
					int toTrans = subpath.getEdges().size() - flowState.getPendingTransmissions()[subpathId];
					
					double subDeadline = (packetRT + deadline - slot) / (double)toTrans;
					at.subDeadline = subDeadline;					
					potentialTransmissions.add(at);
				}
			}
		}
		return potentialTransmissions;
	}

}
