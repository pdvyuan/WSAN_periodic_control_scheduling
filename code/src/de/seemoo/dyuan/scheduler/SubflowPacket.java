package de.seemoo.dyuan.scheduler;

import java.util.ArrayList;
import java.util.List;

import de.seemoo.dyuan.netgen.Flow;
import de.seemoo.dyuan.netgen.Subpath;
import de.seemoo.dyuan.netgen.structure.Edge;
import de.seemoo.dyuan.netgen.structure.Node;

/**
 * A packet is a certain packet (period index) of a flow.
 * It is used to compute the transmissions of the packet that are 
 * fallen into a certain time slot range.
 * 
 * A packet can be released or unreleased.
 * 
 * @author dyuan
 *
 */
public class SubflowPacket {
	
	private final int periodId;
	
	private final boolean upflow;
	
	/**
	 * The absolute release time of the flow.
	 */
	private final int absoluteFlowReleaseTime;

	/**
	 * The absolute effective release time of the packet of this subpath.
	 */
	private final int effectiveSubflowReleaseTime;

	/**
	 * the deadline of the packet of this subpath.
	 */
	private final int effectiveSubFlowDeadline;
	
	private final FlowTransmissionState flowState;
	
	/**
	 * 
	 * Constructor. Suppose all packets are generated for the first time at slot 0.
	 * 
	 * @param path the path which the packet belongs to.
	 * @param periodNum the period number of the packet.
	 */
	public SubflowPacket(FlowTransmissionState flowState, boolean upflow, int periodNum) {
		this.periodId = periodNum;
		this.flowState = flowState;
		this.upflow = upflow;
		Flow flow = flowState.getFlow();
		this.absoluteFlowReleaseTime = periodNum * flow.getPeriod();
		if (upflow) {
			this.effectiveSubflowReleaseTime = this.absoluteFlowReleaseTime;
		} else {
			this.effectiveSubflowReleaseTime = this.absoluteFlowReleaseTime + flow.getMaxUpstreamHops();
		}
		this.effectiveSubFlowDeadline = this.absoluteFlowReleaseTime + flow.getSubflowEffectiveDeadline(upflow) - 1; 
	}
	
	private static final List<Transmission> empty_transmissions = new ArrayList<Transmission>();
	
	public static class IntersectTransmissions {
		public List<Transmission> transmissions;
		public boolean intersectNextPacket;
	}
	
	/**
	 * Compute the intersection of this subflow transmissions.
	 * 
	 * if intersectNextPacket == false, we should not check the later packets of this flow.
	 * 
	 * CHECKED.
	 */
	public IntersectTransmissions getIntersectOfSubflowTransmissions(int currentSlot, int deadline, Node conflictNode) {
		IntersectTransmissions ret = new IntersectTransmissions();
		ret.transmissions = new ArrayList<Transmission>();
		ret.intersectNextPacket = true;
		Flow flow = flowState.getFlow();
		List<Subpath> subpaths = flow.getSubpaths(this.upflow); 
		//unreleased.
		if (currentSlot < this.absoluteFlowReleaseTime) {
			for (int i=0; i<subpaths.size(); i++) {
				IntersectTransmissions trans = this.getIntersectOfUnreleasedSubpathTransmissions(i, currentSlot, deadline, conflictNode);
				ret.transmissions.addAll(trans.transmissions);
				if (!trans.intersectNextPacket) {
					ret.intersectNextPacket = false;
				}
			}
		} else {
			for (int i=0; i<subpaths.size(); i++) {
				Subpath subpath = subpaths.get(i);
				if ( (flowState.isInUpflow() == subpath.isUpflow() 
						&& flowState.getPendingTransmissions()[i] < subpath.getEdges().size())
						|| (flowState.isInUpflow() != subpath.isUpflow()) ) {
					IntersectTransmissions trans = this.getIntersectOfReleasedSubpathTransmissions(i, currentSlot, deadline, conflictNode);
					ret.transmissions.addAll(trans.transmissions);
					if (!trans.intersectNextPacket) {
						ret.intersectNextPacket = false;
					}
				}
				
			}
		}
		
		return ret;
	}
	
	/**
	 * Find the future transmissions of subpath transmissions whose releaseTime is in [currentSlot, deadline] and
	 * which involve node conflictNode. The transmissions of the subpath are not released.
	 * 
	 * Condition the packet is not released. currentSlot < this.absoluteFlowReleaseTime.
	 * 
	 * if intersectNext == false, it means that the later subflow packets do not need to be checked.
	 * 
	 * CHECKED.
	 *
	 */
	private IntersectTransmissions getIntersectOfUnreleasedSubpathTransmissions(int subpathId, int currentSlot, int deadline, Node conflictNode) {
		if (currentSlot >= this.absoluteFlowReleaseTime) {
			throw new IllegalArgumentException("the flow transmissions should not be already released.");
		}
		
		//currentSlot > this.effectiveSubpathDeadline is impossible
		
		//effectiveSubpathReleaseTime is the earliest possible release time of the subpath transmissions.
		if (deadline < this.effectiveSubflowReleaseTime) {
			IntersectTransmissions ret = new IntersectTransmissions();
			ret.transmissions = empty_transmissions;
			ret.intersectNextPacket = false;
			return ret;
		}
		
		IntersectTransmissions ret = new IntersectTransmissions();
		ret.transmissions = new ArrayList<Transmission>();	
		ret.intersectNextPacket = true;
		
		Subpath subpath = this.flowState.getFlow().getSubpath(this.upflow, subpathId);
		
		for (int i=0; i<subpath.getEdges().size(); i++) {
			int ri = this.effectiveSubflowReleaseTime + i;
			int di = this.effectiveSubFlowDeadline - (subpath.getEdges().size() - 1 - i);
			
			Edge edge = subpath.getEdges().get(i);
			if (ri >= currentSlot && ri <= deadline && edge.hasNode(conflictNode)) {
				Transmission trans = new Transmission();
				trans.anticipatedReleaseTime = ri;
				trans.deadline = di;
				//trans.pathId is not properly set.
				trans.currEdge = edge;
				ret.transmissions.add(trans);
			}
			//optimize a bit.
			else if (ri > deadline) {
				ret.intersectNextPacket = false;
				break;
			}
		}
		return ret;
	}
	
	/**
	 * 
	 * Find the future transmissions of subpath transmissions whose releaseTime is in [currentSlot, deadline] and
	 * which involve node conflictNode. 
	 * 
	 * The transmissions of the subpath are released and there is unfinished transmissions in the subpath.
	 * 
	 * Condition: currentSlot >= this.absoluteFlowReleaseTime.
	 * For the current period and current subflow, only look at the current subpath with State STATE_TRANSMISSION_RELEASED
	 * 
	 * If the current subflow is upflow, we also look at the downflow subpaths.
	 * 
	 * if intersectNext == false, it means that the later subflow packets do not need to be checked.
	 * 
	 * CHECKED.
	 */
	private IntersectTransmissions getIntersectOfReleasedSubpathTransmissions(int subpathId, int currentSlot, int deadline, Node conflictNode) {
		if (currentSlot < this.absoluteFlowReleaseTime) {
			throw new IllegalArgumentException("the flow transmissions should not be unreleased.");
		}
		if (this.periodId != flowState.getPeriodId()) {
			throw new IllegalArgumentException("The subflow periodId should be the same as the current periodId");
		}
		
		Subpath subpath = this.flowState.getFlow().getSubpath(this.upflow, subpathId);
		if (flowState.isInUpflow() == subpath.isUpflow()) {
			//the current subpath transmissions, there must be transmissions left on the subpath.
			int nextTransmissionHop = flowState.getPendingTransmissions()[subpathId];
			if (nextTransmissionHop == subpath.getEdges().size()) {
				throw new IllegalArgumentException("should not happen.");
			}
			if (this.effectiveSubFlowDeadline - currentSlot + 1 < subpath.getEdges().size() - nextTransmissionHop) {
				System.err.printf("err %d < %d\n", this.effectiveSubFlowDeadline - currentSlot + 1, subpath.getEdges().size() - nextTransmissionHop);
				throw new RuntimeException(new SchedulerException(SchedulerException.NOT_ENOUGH_SLOTS_FOR_FUTURE_TRANSMISSION));
			}
			
			IntersectTransmissions ret = new IntersectTransmissions();
			ret.transmissions = new ArrayList<Transmission>();	
			ret.intersectNextPacket = true;
			
			for (int i=nextTransmissionHop; i<subpath.getEdges().size(); i++) {
				int ri = currentSlot + (i - nextTransmissionHop);
				int di = this.effectiveSubFlowDeadline - (subpath.getEdges().size()-1-i);
				
				Edge edge = subpath.getEdges().get(i);
				if (ri >= currentSlot && ri <= deadline && edge.hasNode(conflictNode)) {
					Transmission trans = new Transmission();
					trans.anticipatedReleaseTime = ri;
					trans.deadline = di;
					//trans.pathId is not properly set.
					trans.currEdge = edge;
					ret.transmissions.add(trans);
				}
				//optimize a bit.
				else if (ri > deadline) {
					ret.intersectNextPacket = false;
					break;
				}
			}
			return ret;
			
		} else {
			int remainingInThisSubflow = flowState.getTransmissionsLeftInThisSubflow();
			//for the same period, the current transmissions are in the upflow, we look at the downflow.
			if (subpath.isUpflow() || !flowState.isInUpflow()) {
				throw new IllegalArgumentException("the current transmissions must be in the upflow and the subpath must be the downflow.");
			}
			if (this.effectiveSubFlowDeadline - currentSlot + 1 
					< remainingInThisSubflow + subpath.getEdges().size()) {
				System.err.printf(" err %d < %d\n", this.effectiveSubFlowDeadline - currentSlot + 1, 
						remainingInThisSubflow + subpath.getEdges().size());
				throw new RuntimeException(new SchedulerException(SchedulerException.NOT_ENOUGH_SLOTS_FOR_FUTURE_TRANSMISSION));
				
			}			
			
			//effective subpath release time may be different from flow release time.
			if (deadline < currentSlot + remainingInThisSubflow) {
				IntersectTransmissions ret = new IntersectTransmissions();
				ret.transmissions = empty_transmissions;
				ret.intersectNextPacket = false;
				return ret;
			}
			
			
			IntersectTransmissions ret = new IntersectTransmissions();
			ret.transmissions = new ArrayList<Transmission>();	
			ret.intersectNextPacket = true;
			
			for (int i=0; i<subpath.getEdges().size(); i++) {
				int ri = currentSlot + remainingInThisSubflow + i;
				int di = this.effectiveSubFlowDeadline - (subpath.getEdges().size() - 1 - i);
				Edge edge = subpath.getEdges().get(i);
				if (ri >= currentSlot && ri <= deadline && edge.hasNode(conflictNode)) {
					Transmission trans = new Transmission();
					trans.anticipatedReleaseTime = ri;
					trans.deadline = di;
					//trans.pathId is not properly set.
					trans.currEdge = edge;
					ret.transmissions.add(trans);
				}
				//optimize a bit.
				else if (ri > deadline) {
					ret.intersectNextPacket = false;
					break;
				}
			}
			return ret;
			
		}
	}
	
	public static class Status {
		public boolean packetReleasedLaterThanTimeRange;
	}
//
//	
//	/**
//	 * compute the transmissions that fall into the slots between [lowerSlot, upperSlot]
//	 * @param currentSlot We are at the beginning of slot currentSlot.
//	 * @param lowerSlot The lower bound of the range, inclusive.
//	 * @param upperSlot The upper bound of the range, inclusive.
//	 * @param nextTransmissionHop What is the next transmission hop on this path for this packet. If the packet has
//	 * been released, nextTransmissionHop is the next transmission to happen. IF the packet has not yet been released,
//	 * nextTransmissionHop should be 0.
//	 * @param status, if upperSlot < absoluteReleaseTime, set to true, otherwise false.
//	 * @return the transmissions of the path in the range
//	 * @throws SchedulerException If there is not enough slots available for finishing transmitting this packet.
//	 */
//	public List<Edge> getTransmissionsInTimeRange(int currentSlot, int lowerSlot, int upperSlot, int nextTransmissionHop,
//			Status status) throws SchedulerException {
//		if (lowerSlot < currentSlot)
//			throw new SchedulerException(SchedulerException.UNEXPECTED_EXCEPTION);
//		if (currentSlot > this.absoluteReleaseTime) {
//			//the packet has been released.
//			//do not have enough slots for transmission.
//			if (this.absoluteDeadline - currentSlot +1 < path.getAllEdges().size() - nextTransmissionHop) {
//				throw new SchedulerException(SchedulerException.NOT_ENOUGH_SLOTS_FOR_FUTURE_TRANSMISSION);
//			}
//		} else {
//			//the packet is not yet released. No transmission is possible.
//			nextTransmissionHop = 0;
//		}
//		
//		
//		if (upperSlot < this.absoluteReleaseTime || lowerSlot > this.absoluteDeadline) {
//			//no intersection.
//			if (upperSlot < this.absoluteReleaseTime)
//				status.packetReleasedLaterThanTimeRange = true;
//			return empty_edges;
//		}
//		status.packetReleasedLaterThanTimeRange = false;
//		
//		List<Edge> edges = new ArrayList<Edge>();
//		//compute the intersection.
//		int low = Math.max(lowerSlot, this.absoluteReleaseTime);
//		int up = Math.min(upperSlot, this.absoluteDeadline);
//		
//		int nextTransmissionReleaseTime = Math.max(currentSlot, this.absoluteReleaseTime);
//		for (int i=nextTransmissionHop+(low - nextTransmissionReleaseTime); i<path.getAllEdges().size()-(this.absoluteDeadline - up);
//				i++) {
//			Edge edge = path.getAllEdges().get(i);
//			edges.add(edge);
//		}
//		
////		//correct!
////		List<Edge> edges2 = new ArrayList<Edge>();
////		for (int i=nextTransmissionHop; i<path.getEdges().size(); i++) {
////			int ri;
////			if (currentSlot > this.absoluteReleaseTime) {
////				//released
////				ri = currentSlot + i - nextTransmissionHop;
////			} else {
////				//unreleased
////				ri = this.absoluteReleaseTime + i;
////			}
////			int di = this.absoluteDeadline - (path.getEdges().size()-1-i);
////			if (ri >= low && di <= up) {
////				Edge edge = path.getEdges().get(i);
////				edges2.add(edge);
////			}
////			//optimize a bit.
////			else if (di > up)
////				break;
////		}
////		if (edges.size() != edges2.size()) {
////			throw new SchedulerException("check failed");
////		}
////		for (int i=0; i<edges.size(); i++) {
////			if (!edges.get(i).equals(edges2.get(i)))
////				throw new SchedulerException("check failed");
////		}
//		return edges;
//	}
}
