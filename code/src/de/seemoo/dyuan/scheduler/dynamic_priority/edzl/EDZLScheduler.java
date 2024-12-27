package de.seemoo.dyuan.scheduler.dynamic_priority.edzl;

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
 * EDZL multiprocessor scheduling algorithm. Earliest Deadline Zero Laxity Algorithm.
 * c.f. "On-line multiprocessor scheduling algorithms for real-time tasks" by Suk Kyoon Lee. 
 * EDZL results in the same schedule as EDF until a situation is reached
	when a task will miss its deadline unless it executes for
	all of the remaining time up to its deadline (zero laxity),
	EDZL gives such a task the highest priority.
	
	The task model
 * 		the upstream/downstream transmissions of a subpath is defined as a task.
 * 
 * works for both shortSchedule or not.
 * 	
 * @author dyuan
 *
 */
public class EDZLScheduler extends HeuristicScheduler {

	public EDZLScheduler(NetworkModel network, boolean piggyback, boolean shortSchedule, int nChannels) {
		super(network, piggyback, shortSchedule, nChannels);
	}
	
	public static class EDZLActiveTransmission extends ActiveTransmission {
		public int deadline;
		public int laxity;
	}
	
	private Comparator<EDZLActiveTransmission> transmissionsComparator = new Comparator<EDZLActiveTransmission>() {
		
		/**
		 * If both laxities are non-zero, prioritize smaller deadline, if deadlines are the same, prioritize the smaller laxity.
		 * 
		 * If one of the laxity is zero, prioritize the smaller laxity.
		 */
		@Override
		public int compare(EDZLActiveTransmission t1, EDZLActiveTransmission t2) {
			//earliest deadline
			if (t1.laxity > 0 && t2.laxity > 0) {
				if (t1.deadline != t2.deadline) {
					return t1.deadline - t2.deadline;
				}
			}
			//at least one of the laxity <= 0 or deadlines are the same.
			return t1.laxity - t2.laxity;			
		}
		
	};
	
	@Override
	protected List<? extends ActiveTransmission> collectAndOrderPotentialTransmissions(
			int slot) {
		List<EDZLActiveTransmission> potentialTransmissions = collectPotentialTransmissions(slot);
		Collections.sort(potentialTransmissions, this.transmissionsComparator);
//		System.out.println(slot+":");
//		for (EDZLActiveTransmission trans : potentialTransmissions) {
//			System.out.printf("f%d p%d l=%d d=%d\n", trans.flowId, trans.subpathId, trans.laxity, trans.deadline);
//		}
		return potentialTransmissions;
	}
	
	/**
	 * Works for both shortSchedule or not. 
	 * 
	 */
	protected List<EDZLActiveTransmission> collectPotentialTransmissions(int slot) {
		
		List<EDZLActiveTransmission> potentialTransmissions = new ArrayList<EDZLActiveTransmission>();
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
					int period = flowState.getPeriodId();
					EDZLActiveTransmission at = new EDZLActiveTransmission();
					this.initActiveTransmission(flowState, subpath, hop, at);
					int packetRT = period * flow.getPeriod();
					int toTrans = subpath.getEdges().size() - flowState.getPendingTransmissions()[subpathId];
					int absDeadline = packetRT + deadline -1;
					int transDT = absDeadline + 1 - toTrans;
					int slack = transDT - slot;
					at.deadline = absDeadline;
					at.laxity = slack;
					potentialTransmissions.add(at);
				}
			}
		}
		return potentialTransmissions;
	}

}
