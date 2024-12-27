package de.seemoo.dyuan.scheduler.tasa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.seemoo.dyuan.netgen.Flow;
import de.seemoo.dyuan.netgen.NetworkModel;
import de.seemoo.dyuan.netgen.Subpath;
import de.seemoo.dyuan.netgen.structure.Node;
import de.seemoo.dyuan.scheduler.ActiveTransmission;
import de.seemoo.dyuan.scheduler.FlowTransmissionState;
import de.seemoo.dyuan.scheduler.HeuristicScheduler;

/**
 * TASA scheduler which extends the idea of TASA to real-time multi-flow scheduling.
 * see Traffic Aware Scheduling Algorithm for Reliable Low-Power Multi-Hop IEEE 802.15.4e Networks
 * 
 * @author dyuan
 *
 */
public class TasaScheduler extends HeuristicScheduler {
	
	private Map<Node, Integer> queueSize;

	public TasaScheduler(NetworkModel network, boolean piggyback, boolean shortSchedule, int nChannels) {
		super(network, piggyback, shortSchedule, nChannels);
	}

	@Override
	protected List<? extends ActiveTransmission> collectAndOrderPotentialTransmissions(int slot) {
		List<ActiveTransmission> potentialTransmissions = collectPotentialTransmissions();
		Collections.sort(potentialTransmissions, transmissionsComparator);
		return potentialTransmissions;
	}
	
	private Comparator<ActiveTransmission> transmissionsComparator = new Comparator<ActiveTransmission>() {

		@Override
		public int compare(ActiveTransmission t1, ActiveTransmission t2) {
			return queueSize.get(t2.sender) - queueSize.get(t1.sender); 
		}
	};

	private List<ActiveTransmission> collectPotentialTransmissions() {
		List<ActiveTransmission> potentialTransmissions = new ArrayList<ActiveTransmission>();
		this.queueSize = new HashMap<Node, Integer>();
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
					ActiveTransmission at = new ActiveTransmission();
					this.initActiveTransmission(flowState, subpath, hop, at);
					Integer size;
					if ((size = queueSize.get(at.sender)) == null) {
						size = 1;
					} else {
						size++;
					}
					this.queueSize.put(at.sender, size);
					potentialTransmissions.add(at);
				}
			}
		}
		return potentialTransmissions;
	}

}
