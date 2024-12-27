package de.seemoo.dyuan.scheduler.dynamic_priority.busy_first;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.seemoo.dyuan.netgen.Flow;
import de.seemoo.dyuan.netgen.NetworkModel;
import de.seemoo.dyuan.netgen.Subpath;
import de.seemoo.dyuan.netgen.structure.Edge;
import de.seemoo.dyuan.netgen.structure.Node;
import de.seemoo.dyuan.scheduler.ActiveTransmission;
import de.seemoo.dyuan.scheduler.FlowTransmissionState;
import de.seemoo.dyuan.scheduler.HeuristicScheduler;
import de.seemoo.dyuan.scheduler.SchedulerBase.FlowsOfPeriod;

/**
 * 
 * if slacks of two transmissions are not equal, LST
 * draw with busy sender first (max conflict trans left) 
 * conflict include all links in primary conflict with a link.
 * 
 * random, implicit
 *   LST 6864 BusySenderFirst 6924
 * random, restricted:
 *   LST 4420 BusySenderFirst 4515
 *   
 * Work for both shortSchedule or not.
 * 
 * 
 * 	
 * @author dyuan
 *
 */
public class LST_MCFScheduler extends HeuristicScheduler {
	

	public LST_MCFScheduler(NetworkModel network, boolean piggyback, boolean shortSchedule, int nChannels) {
		super(network, piggyback, shortSchedule, nChannels);
	}
	
	@Override
	public void initScheduler() {
		for (Edge edge : this.network.getGraph().getEdges()) {
			edge.buildNeighbors(this.network);
		}
	}
	
	@Override
	protected void initFlowsOfPeriod(FlowsOfPeriod fp) {
		super.initFlowsOfPeriod(fp);
		for (Edge edge : this.network.getGraph().getEdges()) {
			edge.setTransmissionsLeft(0);
		}
		for (Flow flow : fp.getFlows()) {
			int trans = this.getHyperPeriod() / flow.getPeriod();
			for (boolean upflow : new boolean[] {true, false}) {
				List<Subpath> paths;
				if (upflow) {
					paths = flow.getUpflowPaths();
				} else {
					paths = flow.getDownflowPaths();
				}
				for (Subpath path : paths) {
					for (Edge edge : path.getEdges()) {
						edge.setTransmissionsLeft(edge.getTransmissionsLeft() + trans);
					}
				}
			}
		}
	}
	
	@Override
	public void initPendingPackets() {
		super.initPendingPackets();
		for (Edge edge : this.network.getGraph().getEdges()) {
			edge.setTransmissionsLeft(0);
		}
		for (Flow flow : this.nonEmptyFlows) {
			int trans = this.getHyperPeriod() / flow.getPeriod();
			for (boolean upflow : new boolean[] {true, false}) {
				List<Subpath> paths;
				if (upflow) {
					paths = flow.getUpflowPaths();
				} else {
					paths = flow.getDownflowPaths();
				}
				for (Subpath path : paths) {
					for (Edge edge : path.getEdges()) {
						edge.setTransmissionsLeft(edge.getTransmissionsLeft() + trans);
					}
				}
			}
		}
	}
	
	public static class BusySenderActiveTransmission extends ActiveTransmission {
		public int slack;
		public int conflictTransLeft;
	}
	
	private Comparator<BusySenderActiveTransmission> transmissionsComparator = new Comparator<BusySenderActiveTransmission>() {

		@Override
		public int compare(BusySenderActiveTransmission t1, BusySenderActiveTransmission t2) {
			if (t1.slack != t2.slack) {
				return t1.slack - t2.slack;
			}
			return t2.conflictTransLeft - t1.conflictTransLeft;			
		}
		
	};

	@Override
	protected List<? extends ActiveTransmission> collectAndOrderPotentialTransmissions(int slot) {
		List<BusySenderActiveTransmission> potentialTransmissions = collectPotentialTransmissions(slot);
		Collections.sort(potentialTransmissions, this.transmissionsComparator);
//		System.out.println(slot+":");
//		for (BusySenderActiveTransmission trans : potentialTransmissions) {
//			System.out.printf("f%d p%d l=%d c=%d\n", trans.flowId, trans.subpathId, trans.slack, trans.conflictTransLeft);
//		}
		return potentialTransmissions;
	}

	/**
	 * This works for both shortSchedule or not.
	 */
	private List<BusySenderActiveTransmission> collectPotentialTransmissions(int slot) {
		List<BusySenderActiveTransmission> potentialTransmissions = new ArrayList<BusySenderActiveTransmission>();
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
					
					BusySenderActiveTransmission at = new BusySenderActiveTransmission();
					this.initActiveTransmission(flowState, subpath, hop, at);
					at.slack = slack;
					at.conflictTransLeft = getConflictTransmissionsLeft(at.edge);
					potentialTransmissions.add(at);
				}
			}
		}
		return potentialTransmissions;
	}
	
	/**
	 * return the number of transmissions in all conflicting links and self.
	 * 
	 */
	private int getConflictTransmissionsLeft(Edge edge) {
		int conflicts = 0;
		for (Edge e : edge.getNeighbors()) {
			conflicts += e.getTransmissionsLeft();
		}
		conflicts += edge.getTransmissionsLeft();
		return conflicts;
	}
	

	/**
	 * In the current slot, transmit the next transmission on a certain path.
	 * 
	 */
	@Override
	protected void transmitNext(FlowTransmissionState flowState, OneTransmission trans, Node sender, Node receiver) {
		super.transmitNext(flowState, trans, sender, receiver);
		Edge edge = trans.edge;
		edge.decTransmissionsLeft();
	}
	
	private final static boolean debug = false;
	
	protected void doBeforeSuccess() {
		if (debug && !this.shortSchedule) {
			for (Edge edge : this.network.getGraph().getEdges()) {
				if (edge.getTransmissionsLeft() != 0) {
					throw new RuntimeException("should not happen!");
				}
			}
		}
	}

}

