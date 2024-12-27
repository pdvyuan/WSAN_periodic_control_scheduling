package de.seemoo.dyuan.scheduler;

import de.seemoo.dyuan.netgen.Flow;
import de.seemoo.dyuan.netgen.Subpath;

public class FlowTransmissionState {
	
	public static final int STATE_TRANSMISSION_RELEASED = 0;
	
	public static final int STATE_TRANSMISSION_UNRELEASED = 1;
	
	public static final int STATE_SCHEDULING_FINISHED = 2;
	
	private final HeuristicScheduler scheduler;
	
	private final Flow flow;
	
	private int periodId;
	
	private boolean finished;
	
	private boolean upFlow;
	private int[] pendingTransmissions;
	private int[] upflowTransmissions;
	private int[] downflowTransmissions;
	private int[] upflowTransmissionStates;
	private int[] downflowTransmissionStates;
	private int[] pendingTransmissionStates;
	
	
	public FlowTransmissionState(HeuristicScheduler scheduler, Flow flow) {
		this.scheduler = scheduler;
		this.flow = flow;
		this.upflowTransmissions = new int[flow.getUpflowPaths().size()];
		this.upflowTransmissionStates = new int[flow.getUpflowPaths().size()];
		
		this.downflowTransmissions = new int[flow.getDownflowPaths().size()];
		this.downflowTransmissionStates = new int[flow.getDownflowPaths().size()];
		
		this.pendingTransmissions = this.upflowTransmissions;
		this.pendingTransmissionStates = this.upflowTransmissionStates;
		
		this.upFlow = true;
		for (int i=0; i<this.upflowTransmissions.length; i++) {
			this.upflowTransmissions[i] = 0;
			this.upflowTransmissionStates[i] = STATE_TRANSMISSION_RELEASED;
		}
		for (int i=0; i<this.downflowTransmissions.length; i++) {
			this.downflowTransmissions[i] = 0;
			this.downflowTransmissionStates[i] = STATE_TRANSMISSION_RELEASED;
		}
		this.periodId = 0;
		this.finished = false;
	}
	
	public Flow getFlow() {
		return this.flow;
	}
	
	public boolean isInUpflow() {
		return this.upFlow;
	}
	
	public int[] getPendingTransmissions() {
		return this.pendingTransmissions;
	}
	
	public int getPeriodId() {
		return this.periodId;
	}
	
	public int[] getPendingTransmissionStates() {
		return this.pendingTransmissionStates;
	}

	public void transmitNext(int subpathId, boolean upFlow) {
		if (this.upFlow != upFlow) {
			throw new IllegalArgumentException("the current scheduling subflow doesn't match the expected.");
		}
		this.pendingTransmissions[subpathId] += 1;
	}

	public boolean isFinished() {
		return this.finished;
	}
	
	public int getTransmissionsLeftInThisSubflow() {
		int max = 0;
		for (int i=0; i<this.pendingTransmissions.length; i++) {
			Subpath subpath = this.flow.getSubpath(this.upFlow, i);
			int nextHop = this.pendingTransmissions[i];
			int toTrans = subpath.getEdges().size() - nextHop;
			if (toTrans > max) {
				max = toTrans;
			}
		}
		return max;
	}
	
	/**
	 * @param the slot id of the next slot.
	 * This works with non-shortSchedule.
	 * This also works with shortSchedule.
	 * 
	 */
	public int auditSchedulingState(int nextSlot, boolean shortSchedule) {
		//the scheduling of this flow is already finished.
		if (this.finished)
			return HeuristicScheduler.SCHEDULING_FINISHED;
		//shift data struct when subflowFinished.
		boolean subflowFinished = true;
		boolean lastPeriod;
		if (shortSchedule) {
			lastPeriod = (this.periodId == 0);
		} else {
			lastPeriod = (this.periodId == scheduler.getHyperPeriod() / flow.getPeriod() - 1);
		}
		
		for (int i=0; i<this.pendingTransmissions.length; i++) {
			int nextHop = this.pendingTransmissions[i];
			if (nextHop == 0 && this.upFlow) {
				//nextTransmission is the 1st hop of the upflow.
				boolean currentPacketReleased = (this.periodId * flow.getPeriod() <= nextSlot);
				
				if (currentPacketReleased && checkDeadlineMiss(i, nextSlot))
					return HeuristicScheduler.SCHEDULING_DEADLINE_MISS; 
				
				this.pendingTransmissionStates[i] = (currentPacketReleased ? STATE_TRANSMISSION_RELEASED : STATE_TRANSMISSION_UNRELEASED);
				subflowFinished = false;
				
			} else if (nextHop == flow.getSubpathLength(this.upFlow, i)) {
				//all transmissions in the subpath have been finished.
				if (lastPeriod && !this.upFlow) {
					this.pendingTransmissionStates[i] = STATE_SCHEDULING_FINISHED;
				} else {
					//the upflow subpath finished or the downflow subpath finished but the period has not ended.
					this.pendingTransmissionStates[i] = STATE_TRANSMISSION_UNRELEASED;
				}
			} else {
				if (checkDeadlineMiss(i, nextSlot))
					return HeuristicScheduler.SCHEDULING_DEADLINE_MISS; 
				this.pendingTransmissionStates[i] = STATE_TRANSMISSION_RELEASED;
				subflowFinished = false;
			}
		}
		if (subflowFinished) {
			//toggle from upflow to downflow.
			if (this.upFlow) {
				this.upFlow = false;
				this.pendingTransmissions = this.downflowTransmissions;
				this.pendingTransmissionStates = this.downflowTransmissionStates;
				
				//check deadline miss.
				int deadline = flow.getDeadline();
				int remaining = flow.getMaxDownstreamHops();
				int transDT = this.periodId * flow.getPeriod() + deadline - remaining;
				if (transDT < nextSlot)
					return HeuristicScheduler.SCHEDULING_DEADLINE_MISS;
				
				for (int i=0; i<this.pendingTransmissions.length; i++) {
					this.pendingTransmissions[i] = 0;
					this.pendingTransmissionStates[i] = STATE_TRANSMISSION_RELEASED;
				}
			} else {
				//toggle from downflow to upflow
				this.periodId++;
				if (lastPeriod) {
					//the whole transmission finished.
					this.finished = true;
					return HeuristicScheduler.SCHEDULING_FINISHED;
				} else {
					this.upFlow = true;
					this.pendingTransmissions = this.upflowTransmissions;
					this.pendingTransmissionStates = this.upflowTransmissionStates;
					//check whether the new packet is released.
					int packetRT = periodId * this.flow.getPeriod();
					boolean released = (packetRT <= nextSlot);
					
					if (released) {
						//check deadline miss.
						int deadline = flow.getUpflowEffectiveDeadline();
						int remaining = flow.getMaxUpstreamHops();
						int transDT = this.periodId * flow.getPeriod() + deadline - remaining;
						if (transDT < nextSlot)
							return HeuristicScheduler.SCHEDULING_DEADLINE_MISS;
					}
					
					for (int i=0; i<this.pendingTransmissions.length; i++) {
						this.pendingTransmissions[i] = 0;
						if (released) {
							this.pendingTransmissionStates[i] = STATE_TRANSMISSION_RELEASED;
						} else
							this.pendingTransmissionStates[i] = STATE_TRANSMISSION_UNRELEASED;
					}
				}
			}
		}
		return HeuristicScheduler.SCHEDULING_RUNNING;		
	}
	
	/**
	 * 
	 * @param subpathId subpath Id
	 * @return true. if deadline is missed.
	 * Works with both shortSchedule or not.
	 */
	private boolean checkDeadlineMiss(int subpathId, int nextSlot) {
		int deadline;
		int remaining;
		if (this.upFlow) {
			deadline = flow.getUpflowEffectiveDeadline();
			remaining = flow.getUpflowPaths().get(subpathId).getEdges().size() - this.pendingTransmissions[subpathId];
		} else {
			deadline = flow.getDeadline();
			remaining = flow.getDownflowPaths().get(subpathId).getEdges().size() - this.pendingTransmissions[subpathId];
		}
		if (remaining != 0) {
			int transDT = this.periodId * flow.getPeriod() + deadline - remaining;
			if (transDT < nextSlot)
				return true;
		}
		return false;
	}
	
}
