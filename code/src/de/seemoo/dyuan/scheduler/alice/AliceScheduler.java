package de.seemoo.dyuan.scheduler.alice;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.seemoo.dyuan.netgen.Flow;
import de.seemoo.dyuan.netgen.NetworkModel;
import de.seemoo.dyuan.netgen.Subpath;
import de.seemoo.dyuan.netgen.structure.Node;
import de.seemoo.dyuan.scheduler.ActiveTransmission;
import de.seemoo.dyuan.scheduler.FlowTransmissionState;
import de.seemoo.dyuan.scheduler.HeuristicScheduler;

/**
 * The class for ALICE scheduler. 
 * See. ALICE: Autonomous Link-based Cell Scheduling for TSCH.
 * The ALICE scheduler only works for no aggregation and hyper-period mode.
 * 
 * @author dyuan
 *
 */
public class AliceScheduler extends HeuristicScheduler {
	
	public static class AliceActiveTransmission extends ActiveTransmission {
		public int slotId;
		public int channelId;
		
		@Override
		public String toString() {
			int sdr = (int) this.sender.getIndex();
			int rcv = (int) this.edge.getTheOtherNode(this.sender).getIndex();
			return "src "+sdr+", dst "+rcv+": slot "+slotId+" channel "+channelId;
		}
		
	}
	
	/**
	 * number of slots in a frame.
	 */
	private static int slotFrameLength = 3;
	
	public static void setSlotFrameLength(int len) {
		slotFrameLength = len;
	}
	
	public AliceScheduler(NetworkModel network, boolean piggyback, boolean shortSchedule, int nChannels) { 
		super(network, piggyback, shortSchedule, nChannels);
		if (piggyback) {
			throw new IllegalArgumentException("cannot run in aggregation mode!");
		}
		if (shortSchedule) {
			throw new IllegalArgumentException("cannot run in repetitive scheduling mode!");
		}
	}

	@Override
	protected List<? extends ActiveTransmission> collectAndOrderPotentialTransmissions(int slot) {
		throw new IllegalStateException("the function should not be called");
	}
	
	/**
	 * 32 bit Mix Hash Function.
	 * @return a positive value.
	 */
	private long hash(int key)
	{
	  key = ~key + (key << 15); // key = (key << 15) - key - 1;
	  key = key ^ (key >>> 12);
	  key = key + (key << 2);
	  key = key ^ (key >>> 4);
	  key = key * 2057; // key = (key + (key << 3)) + (key << 11);
	  key = key ^ (key >>> 16);
	  return (key & 0x00000000ffffffffL);
	}
	
	/**
	 * 
	 * @param at: a transmission.
	 * @param asfn: absolute slot frame number.
	 * @return the cell to schedule.
	 */
	private void cellForTransmission(AliceActiveTransmission at, int asfn) {
		int sdrId = (int) at.sender.getIndex();
		int rcvId = (int) at.edge.getTheOtherNode(at.sender).getIndex();
		long h = hash(256*sdrId + rcvId + asfn);
		at.slotId = (int) (h % slotFrameLength);
		at.channelId = (int) (h % this.numChannels);
	}
	
	/**
	 * @param slot: current slot, start from 0 to infinity.
	 * Collect all potential transmissions.
	 * 
	 */
	private List<AliceActiveTransmission> collectPotentialTransmissions(int slot) {
		List<AliceActiveTransmission> potentialTransmissions = new ArrayList<AliceActiveTransmission>();
		int asfn = slot / slotFrameLength;
		int slotId = slot % slotFrameLength;
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
					AliceActiveTransmission at = new AliceActiveTransmission();
					this.initActiveTransmission(flowState, subpath, hop, at);
					//absolution slotframe number.
					cellForTransmission(at, asfn);
					if (at.slotId == slotId)  {
						// if current slot can be used for scheduling.
						potentialTransmissions.add(at);
					}
				}
			}
		}
		//Collections.sort(potentialTransmissions);
		return potentialTransmissions;
	}
	
	
	/**
	 * Select actual transmissions from the potential transmission set to be scheduled in this slot.
	 * This works for the non-shortSchedule version.
	 * 
	 * @param potentialSet potential transmission set
	 */
	private void selectActualTransmissions(List<AliceActiveTransmission> potentialSet) {
		List<OneTransmission> transmissionsInOneSlot = new ArrayList<OneTransmission>();
		int numTrans = 0;
		Set<Node> conflictNodes = new HashSet<Node>();
		boolean[] channelScheduled = new boolean[this.numChannels];
		for (int i=0; i < channelScheduled.length; i++) {
			channelScheduled[i] = false;
		}
		Iterator<AliceActiveTransmission> iter = potentialSet.iterator();
		while (numTrans < this.getNumOfChannels() && iter.hasNext()) {
			AliceActiveTransmission at = iter.next();
			
			Node node1 = at.edge.getBiggerNode();
			Node node2 = at.edge.getSmallerNode();
			
			if (!conflictNodes.contains(node1) && !conflictNodes.contains(node2)
					&& !channelScheduled[at.channelId]) {
				channelScheduled[at.channelId] = true;
				//System.err.println("schedule "+at.pathId);
				Subpath subpath = at.subpath;
				OneTransmission oneTrans = new OneTransmission(subpath, at.edge, at.sender);
				transmissionsInOneSlot.add(oneTrans);
				transmitNext(at.flowState, oneTrans, at.sender, at.edge.getTheOtherNode(at.sender));
				conflictNodes.add(at.edge.getBiggerNode());
				conflictNodes.add(at.edge.getSmallerNode());
				numTrans++;
			}
		}
		this.scheduleBook.add(transmissionsInOneSlot);		
	}
	
	@Override
	protected int scheduleSlot(int slot) {
		List<AliceActiveTransmission> potentialTransmissions = collectPotentialTransmissions(slot);
		
		selectActualTransmissions(potentialTransmissions);
		
		return this.auditSchedulingState(slot);
		
	}

}
