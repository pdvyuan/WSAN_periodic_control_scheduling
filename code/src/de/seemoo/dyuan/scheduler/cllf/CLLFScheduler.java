package de.seemoo.dyuan.scheduler.cllf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.seemoo.dyuan.netgen.Flow;
import de.seemoo.dyuan.netgen.NetworkModel;
import de.seemoo.dyuan.netgen.Subpath;
import de.seemoo.dyuan.netgen.structure.Node;
import de.seemoo.dyuan.scheduler.ActiveTransmission;
import de.seemoo.dyuan.scheduler.FlowTransmissionState;
import de.seemoo.dyuan.scheduler.HeuristicScheduler;
import de.seemoo.dyuan.scheduler.SchedulerException;
import de.seemoo.dyuan.scheduler.SubflowPacket;
import de.seemoo.dyuan.scheduler.SubflowPacket.IntersectTransmissions;
import de.seemoo.dyuan.scheduler.Transmission;

/**
 * The C-LLF scheduler for the same scheduling problem as
 * OptimalBBScheduler. also see the same paper.
 * 
 * 
 * the deadline is the absolute deadline of the upstream/downstream transmissions.
 * 
 * This only works in the non-shortSchedule mode.
 * 
 * 
 * @author dyuan
 *
 */
public class CLLFScheduler extends HeuristicScheduler {

	/**
	 * Constructor
	 */
	public CLLFScheduler(NetworkModel network, boolean piggyback, boolean shortSchedule, int nChannels) {
		super(network, piggyback, shortSchedule, nChannels);
	}
	
	
	public static class CLLFActiveTransmission extends ActiveTransmission {
		public int laxity;
		public int deadline;
	}
	
	//smaller conflict-aware laxity is prioritized.
	private Comparator<CLLFActiveTransmission> comparatorInLaxity = new Comparator<CLLFActiveTransmission>() {
		@Override
		public int compare(CLLFActiveTransmission t1, CLLFActiveTransmission t2) {
			if (t1.laxity != t2.laxity) {
				return t1.laxity - t2.laxity;
			} else {
				return t1.deadline - t2.deadline;
			}
			
		}
		
	};

	@Override
	protected List<? extends ActiveTransmission> collectAndOrderPotentialTransmissions(int slot) {
		List<CLLFActiveTransmission> potentialTransmissions = new ArrayList<CLLFActiveTransmission>();
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
					CLLFActiveTransmission at = new CLLFActiveTransmission();
					this.initActiveTransmission(flowState, subpath, hop, at);
					int toTrans = subpath.getEdges().size() - hop;
					int transDT = period * flow.getPeriod() + deadline - toTrans;
					int laxity = this.computeMinLaxityOfTransmission(flowId, slot, transDT, subpath.getSenderNodes().get(hop)); 
					at.deadline = transDT;
					at.laxity = laxity;
					potentialTransmissions.add(at);
				}
			}
		}
		Collections.sort(potentialTransmissions, comparatorInLaxity);
//		System.out.println(slot+":");
		for (CLLFActiveTransmission trans : potentialTransmissions) {
//			System.out.printf("f%d, p%d, l=%d, d=%d\n", trans.flowState.getFlow().getFlowId(), 
//					trans.subpath.getSubpathId(), trans.laxity, trans.deadline);
		}
		return potentialTransmissions;		
	}
	

	private Comparator<Transmission> comparatorInDeadline = new Comparator<Transmission>() {
		@Override
		public int compare(Transmission t1, Transmission t2) {
			return t1.deadline - t2.deadline;
		}
		
	};
	
	/**
	 * Compute the minimum laxity of an active transmission.
	 * Precondition: the transDT >= currentSlot.
	 * @param pathId the pathId of the active transmission.
	 * @param currentSlot the current slot.
	 * @param transDT the deadline of the active transmission.
	 * @param conflictNode the node of interest of the active transmission. node u in the paper.
	 * @throws SchedulerException
	 */
	public int computeMinLaxityOfTransmission(int flowId, int currentSlot, int transDT, Node conflictNode) {
		List<Transmission> transmissionsInvolved = new ArrayList<Transmission>();
		//find the intersect between the current flow and this transmission.
		FlowTransmissionState flowState = this.flowStates.get(flowId);
		SubflowPacket packet = new SubflowPacket(flowState, flowState.isInUpflow(), flowState.getPeriodId());
		IntersectTransmissions trans = packet.getIntersectOfSubflowTransmissions(currentSlot, transDT, conflictNode);
		transmissionsInvolved.addAll(trans.transmissions);
		
		if (flowState.isInUpflow() && trans.intersectNextPacket) {
			packet = new SubflowPacket(flowState, false, flowState.getPeriodId());
			trans = packet.getIntersectOfSubflowTransmissions(currentSlot, transDT, conflictNode);
			transmissionsInvolved.addAll(trans.transmissions);
		}
		
		for (int i=0; i<this.flowStates.size()-1; i++) {
			flowId++;
			flowId = flowId % this.flowStates.size();
			flowState = this.flowStates.get(flowId);
			if (flowState.isFinished())
				continue;
			for (int pd = flowState.getPeriodId(); pd < this.getHyperPeriod() / flowState.getFlow().getPeriod(); pd++) {
				if (pd == flowState.getPeriodId()) {
					packet = new SubflowPacket(flowState, flowState.isInUpflow(), pd);
					trans = packet.getIntersectOfSubflowTransmissions(currentSlot, transDT, conflictNode);
					transmissionsInvolved.addAll(trans.transmissions);
					if (!trans.intersectNextPacket) {
						break;
					}
					if (flowState.isInUpflow()) {
						packet = new SubflowPacket(flowState, false, pd);
						trans = packet.getIntersectOfSubflowTransmissions(currentSlot, transDT, conflictNode);
						transmissionsInvolved.addAll(trans.transmissions);
						if (!trans.intersectNextPacket) {
							break;	
						}
					}
				} else {
					packet = new SubflowPacket(flowState, true, pd);
					trans = packet.getIntersectOfSubflowTransmissions(currentSlot, transDT, conflictNode);
					transmissionsInvolved.addAll(trans.transmissions);
					if (!trans.intersectNextPacket) {
						break;
					}
					packet = new SubflowPacket(flowState, false, pd);
					trans = packet.getIntersectOfSubflowTransmissions(currentSlot, transDT, conflictNode);
					transmissionsInvolved.addAll(trans.transmissions);
					if (!trans.intersectNextPacket) {
						break;
					}
				}
			}
		}
		Collections.sort(transmissionsInvolved, comparatorInDeadline);
		int laxity = Integer.MAX_VALUE;
		
		for (int i=0; i<transmissionsInvolved.size(); i++) {
			Transmission t = transmissionsInvolved.get(i);
			int la = t.deadline-currentSlot-i;
//			if (la < 0) {
//				throw new SchedulerException(SchedulerException.NOT_ENOUGH_SLOTS_FOR_FUTURE_TRANSMISSION);
//			}
			if (la < laxity) {
				laxity = la;
			}
//			System.out.printf("curr=%d, deadline=%d, id=%d, laxity=%d\n", currentSlot, t.deadline, i, la);
			
		}
		return laxity;
		
//		for (Transmission t : transmissionInvolved) {
//			System.out.println(t.currEdge.description()+" "+t.deadline);
//		}
//		System.out.println();
	}

}
