package de.seemoo.dyuan.scheduler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.seemoo.dyuan.netgen.NetworkModel;
import de.seemoo.dyuan.netgen.Subpath;
import de.seemoo.dyuan.netgen.structure.Edge;
import de.seemoo.dyuan.netgen.structure.Node;

public class ValidatorScheduler extends HeuristicScheduler {

	private final ScheduleResult scheduleResult;
	
	
	public ValidatorScheduler(NetworkModel network, boolean piggyBack, int nChannels, ScheduleResult scheduleResult) {
		super(network, piggyBack, false, nChannels);
		this.scheduleResult = scheduleResult;
		if (scheduleResult.getStatus() != ScheduleResult.FEASIBLE)
			throw new IllegalArgumentException("the scheduler result must be feasible");
	}

	@Override
	protected int scheduleSlot(int slot) {
		List<OneTransmission> transesInSlot = this.scheduleResult.getScheduleBook().get(slot);
		//check transmissions are released.
		
		for (OneTransmission trans : transesInSlot) {
			int flowId = trans.subpath.getParentFlow().getFlowId();
			int subpathId = trans.subpath.getSubpathId();
			boolean upflow = trans.subpath.isUpflow();
			if (this.flowStates.get(flowId).isInUpflow() != upflow) {
				throw new IllegalStateException("the current active flow cannot be "+(upflow ? "upflow" : "downflow"));
			}
			if (this.flowStates.get(flowId).getPendingTransmissionStates()[subpathId] != FlowTransmissionState.STATE_TRANSMISSION_RELEASED) {
				throw new IllegalStateException("cannot transmit on "+(upflow ? "upflow":"downflow")
						+" subpath "+subpathId+" in slot "+slot);
			}
		}
		checkConfliction(transesInSlot);
		//System.err.println("in slot "+slot);
		doTransmissions(transesInSlot);
		return this.auditSchedulingState(slot);
	}
	
	private void checkConfliction(List<OneTransmission> transesInSlot) {
		if (this.piggybacking) {
			int numTrans = 0;
			Set<Node> senders = new HashSet<Node>();
			Set<Node> receivers = new HashSet<Node>();
			Set<Edge> links = new HashSet<Edge>();
			for (OneTransmission oneTrans : transesInSlot) {
				Subpath subpath = oneTrans.subpath;
				FlowTransmissionState flowState = this.flowStates.get(subpath.getParentFlow().getFlowId());
				int hop = flowState.getPendingTransmissions()[subpath.getSubpathId()];
				Edge edge = subpath.getEdges().get(hop);
				Node sender = oneTrans.sender;
				Node receiver = edge.getTheOtherNode(sender);
				if (senders.contains(sender)) {
					//can be piggyback
					//receiver is free or the link is activated.
					if ((!senders.contains(receiver) && !receivers.contains(receiver)) || links.contains(edge)) {
						//a valid piggyback.
						receivers.add(receiver);
						links.add(edge);
					} else {
						throw new IllegalStateException("invalid piggyback");
					}
				} else {
					if (!receivers.contains(sender) && !senders.contains(receiver) && !receivers.contains(receiver)) {
						//a valid transmission
						senders.add(sender);
						receivers.add(receiver);
						links.add(edge);
						numTrans++;
					} else {
						throw new IllegalStateException("invlaid transmission");
					}
				}
			}
			if (numTrans > this.numChannels) {
				throw new IllegalStateException("cannot contain "+transesInSlot.size()+" transmissions in a slot");
			}
			
		} else {
			if (transesInSlot.size() > this.numChannels) {
				throw new IllegalStateException("cannot contain "+transesInSlot.size()+" transmissions in a slot");
			}
			Edge[] edges = new Edge[transesInSlot.size()];
			for (int i=0; i<edges.length; i++) {
				OneTransmission oneTrans = transesInSlot.get(i);
				Subpath subpath = oneTrans.subpath;
				FlowTransmissionState flowState = this.flowStates.get(subpath.getParentFlow().getFlowId());
				int hop = flowState.getPendingTransmissions()[subpath.getSubpathId()];
				Edge edge = subpath.getEdges().get(hop);
				edges[i] = edge;
			}
			
			for (int i=0; i<edges.length; i++) {
				Edge edge1 = edges[i];
				for (int j=i+1; j<edges.length; j++) {
					Edge edge2 = edges[j];
					if (edge1.isConflict(edge2)) {
						throw new IllegalStateException("Concurrent edges are conflicting: "+edge1.description()
								+" "+edge2.description());
					}
				}
			}
		}
		
		
	}

	private final void doTransmissions(List<OneTransmission> transesInSlot) {
		for (OneTransmission trans : transesInSlot) {
			Subpath subpath = trans.subpath;
			FlowTransmissionState flowState = this.flowStates.get(subpath.getParentFlow().getFlowId());
			int hop = flowState.getPendingTransmissions()[subpath.getSubpathId()];
			Edge edge = subpath.getEdges().get(hop);
			this.transmitNext(flowState, trans, trans.sender, edge.getTheOtherNode(trans.sender));
		}
		this.scheduleBook.add(transesInSlot);
	}


	@Override
	protected List<? extends ActiveTransmission> collectAndOrderPotentialTransmissions(
			int slot) {
		return null;
	}

}
