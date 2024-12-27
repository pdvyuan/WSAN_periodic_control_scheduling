package de.seemoo.dyuan.scheduler.dynamic_priority.lst;

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
 * One of the dynamic priority scheduling algorithm. Least slack time scheduling.
 * 
 * The priorities of all active packets are set according to the slack time, which is the 
 * remaining time till deadline minus the remaining computation time. 
 * 
 * Two task models:
 * The transmissions of a subpath is defined as a task.
 * 
 * This works for both shortSchedule or not.
 * 
 * @author dyuan
 *
 */
public class LSTScheduler extends HeuristicScheduler {

	public LSTScheduler(NetworkModel network, boolean piggyback, boolean shortSchedule, int nChannels) {
		super(network, piggyback, shortSchedule, nChannels);
	}
	
	public static class LSTActiveTransmission extends ActiveTransmission {
		public int slack;
	}
	
	private Comparator<LSTActiveTransmission> transmissionsComparator = new Comparator<LSTActiveTransmission>() {

		@Override
		public int compare(LSTActiveTransmission t1, LSTActiveTransmission t2) {
			return t1.slack - t2.slack;
		}
		
	};

	@Override
	protected List<? extends ActiveTransmission> collectAndOrderPotentialTransmissions(int slot) {
		List<LSTActiveTransmission> potentialTransmissions = collectPotentialTransmissions(slot);
		Collections.sort(potentialTransmissions, this.transmissionsComparator);
		return potentialTransmissions;
	}

	/**
	 * 
	 * This works for both shortSchedule or not.
	 * 
	 */
	protected List<LSTActiveTransmission> collectPotentialTransmissions(int slot) {
		List<LSTActiveTransmission> potentialTransmissions = new ArrayList<LSTActiveTransmission>();
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
					int deadline = flow.getSubflowEffectiveDeadline(flowState.isInUpflow());
					int toTrans = subpath.getEdges().size() - flowState.getPendingTransmissions()[subpathId];
					int packetRT = flowState.getPeriodId() * flow.getPeriod();	
					int transDT = packetRT + deadline - toTrans;
					int slack = transDT - slot;
					
					LSTActiveTransmission at = new LSTActiveTransmission();
					this.initActiveTransmission(flowState, subpath, hop, at);
					at.slack = slack;
					potentialTransmissions.add(at);
				}
			}
		}
		return potentialTransmissions;
	}	

}
