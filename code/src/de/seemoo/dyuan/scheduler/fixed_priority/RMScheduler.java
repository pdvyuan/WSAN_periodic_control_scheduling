package de.seemoo.dyuan.scheduler.fixed_priority;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import de.seemoo.dyuan.netgen.Flow;
import de.seemoo.dyuan.netgen.NetworkModel;
import de.seemoo.dyuan.netgen.Subpath;
import de.seemoo.dyuan.scheduler.FlowTransmissionState;

/**
 * 
 * The classical rate-monotonic scheduling algorithm.
 * Rate-monotonic scheduling algorithm is a fixed-priority real-time
 * scheduling algorithm. 
 * 1. The algorithm assigns priority based on the task period, the shorter the 
 * period, the higher the priority. 
 * 2. For the same period, the priority is assigned arbitrarily.
 * 
 * A flow has a fixed rate (period).
 * 
 * Work for both shortSchedule or not.
 * 
 * @author dyuan
 *
 */
public class RMScheduler extends FixedPriorityScheduler {

	public RMScheduler(NetworkModel network, boolean piggyback, boolean shortSchedule, int nChannels) {
		super(network, piggyback, shortSchedule, nChannels);
	}

	
	/**
	 * Assign priorities to paths. The priority of each task is distinct.
	 * The smaller the period, the higher the priority.
	 * For the same period, the priority is assigned arbitrarily.
	 */
	@Override
	protected void assignPriorities() {
		List<Flow> orderedFlows = new ArrayList<Flow>();
		orderedFlows.addAll(this.getNonEmptyFlows());
		
		Comparator<Flow> flowComparator = new Comparator<Flow>() {
			@Override
			public int compare(Flow f1, Flow f2) {
				return f1.getPeriod() - f2.getPeriod();
			}
		};
		Collections.sort(orderedFlows, flowComparator);
		for (int i=0; i<orderedFlows.size(); i++) {
			this.priorities.put(orderedFlows.get(i), i);
		}
		
	}

	@Override
	protected FPActiveTransmission newTransmission(FlowTransmissionState flowState, Subpath subpath, int hop) {
		FPActiveTransmission at = new FPActiveTransmission();
		this.initActiveTransmission(flowState, subpath, hop, at);
		at.route = subpath.getParentFlow();
		return at;
	}

}
