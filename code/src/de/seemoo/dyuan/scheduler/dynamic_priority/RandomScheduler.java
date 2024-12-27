package de.seemoo.dyuan.scheduler.dynamic_priority;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.seemoo.dyuan.netgen.Flow;
import de.seemoo.dyuan.netgen.NetworkModel;
import de.seemoo.dyuan.netgen.Subpath;
import de.seemoo.dyuan.scheduler.ActiveTransmission;
import de.seemoo.dyuan.scheduler.FlowTransmissionState;
import de.seemoo.dyuan.scheduler.HeuristicScheduler;
import de.seemoo.dyuan.scheduler.dynamic_priority.busy_first.LST_MCFScheduler.BusySenderActiveTransmission;
import de.seemoo.dyuan.scheduler.dynamic_priority.edf.EDFScheduler.EDFActiveTransmission;

/**
 * 
 * Randomly select links to be scheduled per slot. 
 * 
 * @author dyuan
 *
 */
public class RandomScheduler extends HeuristicScheduler {
	
	public static class RandomActiveTransmission extends ActiveTransmission {
	}

	public RandomScheduler(NetworkModel network, boolean piggyback, boolean shortSchedule, int nChannels) {
		super(network, piggyback, shortSchedule, nChannels);
	}

	@Override
	protected List<? extends ActiveTransmission> collectAndOrderPotentialTransmissions(int slot) {
		List<RandomActiveTransmission> potentialTransmissions = collectPotentialTransmissions();
		Collections.shuffle(potentialTransmissions);
		return potentialTransmissions;
		
	}

	private List<RandomActiveTransmission> collectPotentialTransmissions() {
		List<RandomActiveTransmission> potentialTransmissions = new ArrayList<RandomActiveTransmission>();
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
					
					RandomActiveTransmission at = new RandomActiveTransmission();
					this.initActiveTransmission(flowState, subpath, hop, at);
					potentialTransmissions.add(at);
				}
			}
		}
		return potentialTransmissions;
	}

}
