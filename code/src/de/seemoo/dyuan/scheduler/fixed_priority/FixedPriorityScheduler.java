package de.seemoo.dyuan.scheduler.fixed_priority;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.seemoo.dyuan.netgen.Flow;
import de.seemoo.dyuan.netgen.NetworkModel;
import de.seemoo.dyuan.netgen.Route;
import de.seemoo.dyuan.netgen.Subpath;
import de.seemoo.dyuan.scheduler.ActiveTransmission;
import de.seemoo.dyuan.scheduler.FlowTransmissionState;
import de.seemoo.dyuan.scheduler.HeuristicScheduler;

/**
 * Fixed priority scheduler. The base of all fixed priority scheduler,
 * e.g. DM, RM, etc.
 * 
 * The priority of a flow or a subpath is fixed.
 * 
 * 
 * Works for both shortSchedule mode or not.
 * 
 * @author dyuan
 *
 */
public abstract class FixedPriorityScheduler extends HeuristicScheduler {
	
	protected Map<Route, Integer> priorities;
	
	
	private Comparator<FPActiveTransmission> transmissionsComparator = new Comparator<FPActiveTransmission>() {

		@Override
		public int compare(FPActiveTransmission at1, FPActiveTransmission at2) {
			return getPriority(at1.route) - getPriority(at2.route);
		}
		
	};
	
	public FixedPriorityScheduler(NetworkModel network, boolean piggyback, boolean shortSchedule, int nChannels) {
		super(network, piggyback, shortSchedule, nChannels);
	}
	
	public static class FPActiveTransmission extends ActiveTransmission {
		public Route route;
	}
	
	/**
	 * Work for both shortSchedule or not.
	 */
	@Override
	protected List<? extends ActiveTransmission> collectAndOrderPotentialTransmissions(int slot) {
		List<FPActiveTransmission> potentialTransmissions = new ArrayList<FPActiveTransmission>();
		for (int flowId = 0; flowId < this.flowStates.size(); flowId++) {
			FlowTransmissionState flowState = this.flowStates.get(flowId);
			if (flowState.isFinished())
				continue;
			Flow flow = flowState.getFlow();
			for (int subpathId=0; subpathId<flowState.getPendingTransmissions().length; subpathId++) {
				if (flowState.getPendingTransmissionStates()[subpathId] == FlowTransmissionState.STATE_TRANSMISSION_RELEASED) {
					int hop = flowState.getPendingTransmissions()[subpathId];
					Subpath subpath = flow.getSubpath(flowState.isInUpflow(), subpathId); 
					FPActiveTransmission at = newTransmission(flowState, subpath, hop);
					potentialTransmissions.add(at);
				}
			}
		}
		
		Collections.sort(potentialTransmissions, this.transmissionsComparator);
		return potentialTransmissions;
	}


	protected abstract FPActiveTransmission newTransmission(FlowTransmissionState flowState, Subpath subpath, int hop);
	

	protected abstract void assignPriorities();
	
	public int getPriority(Route route) {
		Integer val = this.priorities.get(route);
		if (val == null)
			throw new IllegalArgumentException("the priority of the route is not set.");
		return val.intValue();
	}
	
	@Override
	public final void initScheduler() {
		this.priorities = new HashMap<Route, Integer>();
		this.assignPriorities();
	}
	
}
