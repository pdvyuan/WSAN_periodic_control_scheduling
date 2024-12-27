package de.seemoo.dyuan.scheduler.dynamic_priority.edf;

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
 * One of the dynamic priority scheduling algorithm. Earliest deadline first.
 * 
 * A task is the upflow or downflow transmissions of a subpath.
 * 
 * The priorities of all tasks are set according to the absolute deadlines. 
 * 
 * the deadline is the absolute deadline of the current upflow/downflow subpath transmissions.
 * 
 * This works for both shortSchedule mode and not.
 * 
 * @author dyuan
 *
 */
public class EDFScheduler extends HeuristicScheduler {

	public EDFScheduler(NetworkModel network, boolean piggyback, boolean shortSchedule, int nChannels) {
		super(network, piggyback, shortSchedule, nChannels);
	}
	
	public static class EDFActiveTransmission extends ActiveTransmission {
		public int deadline;
	}
	
	private Comparator<EDFActiveTransmission> transmissionsComparator = new Comparator<EDFActiveTransmission>() {

		@Override
		public int compare(EDFActiveTransmission t1, EDFActiveTransmission t2) {
			return t1.deadline - t2.deadline;
		}
	};

	@Override
	protected final List<? extends ActiveTransmission> collectAndOrderPotentialTransmissions(int slot) {
		List<EDFActiveTransmission> potentialTransmissions = collectPotentialTransmissions();
		Collections.sort(potentialTransmissions, this.transmissionsComparator);
		return potentialTransmissions;
	}
	
	/**
	 * Works for both shortSchedule and not.
	 */
	protected List<EDFActiveTransmission> collectPotentialTransmissions() {
		List<EDFActiveTransmission> potentialTransmissions = new ArrayList<EDFActiveTransmission>();
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
					int absDeadline = period * flow.getPeriod() + deadline - 1;
					EDFActiveTransmission at = new EDFActiveTransmission();
					this.initActiveTransmission(flowState, subpath, hop, at);
					at.deadline = absDeadline;
					potentialTransmissions.add(at);
				}
			}
		}
		return potentialTransmissions;
	}

}
