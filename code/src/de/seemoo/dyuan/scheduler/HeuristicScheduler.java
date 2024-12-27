package de.seemoo.dyuan.scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.seemoo.dyuan.netgen.Flow;
import de.seemoo.dyuan.netgen.NetworkModel;
import de.seemoo.dyuan.netgen.Subpath;
import de.seemoo.dyuan.netgen.structure.Edge;
import de.seemoo.dyuan.netgen.structure.Node;

/**
 * The base-class for scheduling heuristics.
 * 
 * @author dyuan
 *
 */
public abstract class HeuristicScheduler extends SchedulerBase {
	
	/**
	 * Three states of the scheduling: running, finished, deadline miss.
	 */
	public static final int SCHEDULING_RUNNING = 0;
	public static final int SCHEDULING_FINISHED = 1;
	public static final int SCHEDULING_DEADLINE_MISS = 2;
	
	/**
	 * Class for one transmission.
	 *
	 */
	public static class OneTransmission {
		/*
		 * upflow or downflow subpath.
		 */
		public final Subpath subpath;
		public final Edge edge;
		public final Node sender;
		
		public OneTransmission(Subpath subpath, Edge edge, Node sender) {
			this.subpath = subpath;
			this.edge = edge;
			this.sender = sender;
		}
	}

	//schedule book audits the scheduling of slots till now.
	protected List<List<OneTransmission>> scheduleBook = new ArrayList<List<OneTransmission>>();
	protected List<Set<Node>> conflictBook = new ArrayList<Set<Node>>();
	
	protected List<FlowTransmissionState> flowStates;
	
	protected double totalUtil;
	
	public HeuristicScheduler(NetworkModel network, boolean piggyback, boolean shortSchedule, int nChannels) {
		super(network, piggyback, shortSchedule, nChannels);
	}

	/**
	 * "In 2006, Cucu and Goossens showed that the
		taskset hyperperiod (0,H] is a feasibility interval for
		implicit- and constrained-deadline *synchronous* periodic
		tasksets, scheduled by a deterministic and memoryless algorithm." 
		
		We assume that our tasks are all synchronous.
	 */
	@Override
	public ScheduleResult schedule() {
		if (!checkSchedulability()) {
			//schedulability check failed. Impossible to schedule.
			return ScheduleResult.createCheckFailureResult(this);
		}
		//initialize scheduler.
		initScheduler();
		if (this.shortSchedule) {
			//repetitive scheduling.
			for (int periodId = 0; periodId < this.flowsOfPeriods.size(); periodId++) {
				FlowsOfPeriod fp = this.flowsOfPeriods.get(periodId);
				initFlowsOfPeriod(fp);
				SCHEDULE_ONE_PERIOD:
				for (int slot = 0; slot < fp.getPeriod(); slot++) {
					int state = scheduleSlotForFlowsOfPeriod(slot);
					switch (state) {
						//the flows of a period finished.
						case SCHEDULING_FINISHED:
							//we should copy the schedule for k times.
							doFinishSchedulingOnePeriod(periodId);
							//we come to the last period. 
							if (periodId == this.flowsOfPeriods.size()-1) {
								return ScheduleResult.createScheduleResultFromEndingHeuristicScheduler(this);
							} 
							
							break SCHEDULE_ONE_PERIOD;
							
						case SCHEDULING_DEADLINE_MISS:
							return ScheduleResult.createUnschedulableResult(this);
					}
				}
			}
			return ScheduleResult.createUnschedulableResult(this);
		} else {
			//hyper-period scheduling.
			this.initPendingPackets();
			int hyperPeriod = this.getHyperPeriod();
			for (int slot = 0; slot < hyperPeriod; slot++) {
				int state = scheduleSlot(slot);
				switch (state) {
					case SCHEDULING_FINISHED:
						doBeforeSuccess();
						return ScheduleResult.createScheduleResultFromEndingHeuristicScheduler(this);
					case SCHEDULING_DEADLINE_MISS:
						return ScheduleResult.createUnschedulableResult(this);
				}
			}
			return ScheduleResult.createUnschedulableResult(this);
		}
		
	}
	


	protected void initScheduler() {
		
	}

	private void doFinishSchedulingOnePeriod(int periodId) {
		
		if (periodId + 1 < this.flowsOfPeriods.size()) {
			FlowsOfPeriod fp = this.flowsOfPeriods.get(periodId);
			//append empty schedule and conflict set if necessary
			for (int slot = scheduleBook.size(); slot < fp.getPeriod(); slot++) {
				List<OneTransmission> emptyTransmissions = new ArrayList<OneTransmission>();
				this.scheduleBook.add(emptyTransmissions);
				
				if (!this.piggybacking) {
					Set<Node> emptyConflicts = new HashSet<Node>();
					this.conflictBook.add(emptyConflicts);
				}
				
			}
			//replicate schedules and conflict set.		
			FlowsOfPeriod nextfp = this.flowsOfPeriods.get(periodId+1);
			for (int slot = fp.getPeriod(); slot < nextfp.getPeriod(); slot++) {
				//deep copy.
				int slot2Copy = slot % fp.getPeriod();
				List<OneTransmission> copyTrans = new ArrayList<OneTransmission>(this.scheduleBook.get(slot2Copy));
				this.scheduleBook.add(copyTrans);
				
				if (!this.piggybacking) {
					Set<Node> copyConflicts = new HashSet<Node>(this.conflictBook.get(slot2Copy));
					this.conflictBook.add(copyConflicts);
				}
			}
		}
		
	}

	protected void doBeforeSuccess() {
	}

	/**
	 * 
	 * @return false if not schedulable. true if may be schedulable.
	 */
	private boolean checkSchedulability() {
		this.totalUtil = 0;
		for (Flow flow : this.nonEmptyFlows) {
			if (flow.isDeadlineTooSmall()) {
				return false;
			}
			this.totalUtil += flow.getUtil();
		}
		if (!this.piggybacking && this.totalUtil > this.numChannels) {
			return false;
		}
		return true;
	}

	/**
	 * In the current slot, transmit the next transmission on a certain path.
	 * 
	 */
	protected void transmitNext(FlowTransmissionState flowState, OneTransmission trans, Node sender, Node receiver) {
		Subpath subpath = trans.subpath;
		boolean upflow = subpath.isUpflow();
		flowState.transmitNext(subpath.getSubpathId(), upflow);
	}
	
	
	public List<List<OneTransmission>> getScheduleBook() {
		return this.scheduleBook;
	}

	/**
	 * @return int scheduling state
	 * Schedule a slot.
	 * @throws SchedulerException
	 */
	protected int scheduleSlot(int slot) {
		List<? extends ActiveTransmission> potentialSet = collectAndOrderPotentialTransmissions(slot);
		//System.err.println("in slot "+slot);
		selectActualTransmissions(potentialSet);
//		if (piggyback != 0) {
//			System.err.println(this.hashCode()+" slot "+slot+" pig = "+piggyback);
//		}
		return this.auditSchedulingState(slot);
	}
	
	private int scheduleSlotForFlowsOfPeriod(int slot) {
		List<? extends ActiveTransmission> potentialSet = collectAndOrderPotentialTransmissions(slot);
		selectActualTransmissionsForShortSchedule(potentialSet, slot);
		return this.auditSchedulingState(slot);
	}
	
	/**
	 * @return scheduling state.
	 * This works with shortSchedule or not.
	 * 
	 */
	protected int auditSchedulingState(int currentSlot) {
		int nextSlot = currentSlot+1;
		boolean finished = true;
		for (FlowTransmissionState flowState : this.flowStates) {
			int state = flowState.auditSchedulingState(nextSlot, this.shortSchedule);
			if (state == SCHEDULING_DEADLINE_MISS) {
				return SCHEDULING_DEADLINE_MISS;
			} else if (state != SCHEDULING_FINISHED) {
				finished = false;
			}
		}
		if (finished) {
			return SCHEDULING_FINISHED;
		}
		return SCHEDULING_RUNNING;	
	}
	
	/**
	 * Collect the potential transmissions and put them in order.
	 * 
	 * @throws SchedulerException if scheduling exception that as deadline miss occurs already. 
	 */
	protected abstract List<? extends ActiveTransmission> collectAndOrderPotentialTransmissions(int slot);


	/**
	 * Select actual transmissions from the potential transmission set to be scheduled in this slot.
	 * This is the version for the shortSchedule version.
	 * 
	 * slot. the slot id.
	 * 
	 * @param potentialSet potential transmission set
	 */
	private final void selectActualTransmissionsForShortSchedule(List<? extends ActiveTransmission> potentialSet, 
			int slot) {
		if (this.piggybacking) {
			
			List<OneTransmission> transmissionsInOneSlot;
			if (this.scheduleBook.size() > slot) {
				transmissionsInOneSlot = this.scheduleBook.get(slot);
			} else {
				transmissionsInOneSlot = new ArrayList<OneTransmission>();
				this.scheduleBook.add(transmissionsInOneSlot);
			}
			
			Map<Node, Integer> senders = new HashMap<Node, Integer>();
			Set<Node> receivers = new HashSet<Node>();
			Set<Edge> activeEdges = new HashSet<Edge>();
			for (OneTransmission trans : transmissionsInOneSlot) {
				activeEdges.add(trans.edge);
				Node sdr = trans.sender;
				Node rcv = trans.edge.getTheOtherNode(sdr);
				if (!senders.containsKey(sdr)) {
					senders.put(sdr, 1);
				} else {
					int times = senders.get(sdr);
					senders.put(sdr, times+1);
				}
				receivers.add(rcv);
			}
			int numTrans = senders.keySet().size();
			
			Iterator<? extends ActiveTransmission> iter = potentialSet.iterator();
			while (iter.hasNext()) {
				ActiveTransmission at = iter.next();
				Node sender = at.sender;
				Node receiver = at.edge.getTheOtherNode(sender);
				//we can piggyback if the sender->receiver is already activated. 
				//or the sender is activated, but the receiver is not yet.
				if (senders.containsKey(sender)) {
					if ((!senders.containsKey(receiver) && !receivers.contains(receiver)) || activeEdges.contains(at.edge)) {
						Subpath subpath = at.subpath;
						OneTransmission oneTrans = new OneTransmission(subpath, at.edge, at.sender);
						transmissionsInOneSlot.add(oneTrans);
						transmitNext(at.flowState, oneTrans, sender, receiver);
						receivers.add(receiver);
						activeEdges.add(at.edge);
						this.piggybacks++;
						this.transmitted++;
						
						int times = senders.get(sender);
						if (times + 1 > this.maxPiggybackOnSender) {
							maxPiggybackOnSender = times+1;
						}
						senders.put(sender, times+1);
					}
				} else {
					//send to a node not appearing in the receiver.
					if (numTrans < this.getNumOfChannels() && !receivers.contains(sender) && !receivers.contains(receiver) 
							&& !senders.containsKey(receiver)) {
						Subpath subpath = at.subpath;
						OneTransmission oneTrans = new OneTransmission(subpath, at.edge, at.sender);
						transmissionsInOneSlot.add(oneTrans);
						transmitNext(at.flowState, oneTrans, sender, receiver);
						senders.put(sender, 1);
						receivers.add(receiver);
						activeEdges.add(at.edge);
						numTrans++;
						this.transmitted++;
					}
				}
			}
			
		} else {
			List<OneTransmission> transmissionsInOneSlot;
			Set<Node> conflictNodes;
			if (this.scheduleBook.size() > slot) {
				transmissionsInOneSlot = this.scheduleBook.get(slot);
				conflictNodes = this.conflictBook.get(slot);
			} else {
				transmissionsInOneSlot = new ArrayList<OneTransmission>();
				conflictNodes = new HashSet<Node>();
				this.scheduleBook.add(transmissionsInOneSlot);
				this.conflictBook.add(conflictNodes);
			}
			int numTrans = transmissionsInOneSlot.size();
			
			Iterator<? extends ActiveTransmission> iter = potentialSet.iterator();
			while (numTrans < this.getNumOfChannels() && iter.hasNext()) {
				ActiveTransmission at = iter.next();
				
				Node node1 = at.edge.getBiggerNode();
				Node node2 = at.edge.getSmallerNode();
				
				if (!conflictNodes.contains(node1) && !conflictNodes.contains(node2)) {
					OneTransmission oneTrans = new OneTransmission(at.subpath, at.edge, at.sender);
					transmissionsInOneSlot.add(oneTrans);
					transmitNext(at.flowState, oneTrans, at.sender, at.edge.getTheOtherNode(at.sender));
					conflictNodes.add(at.edge.getBiggerNode());
					conflictNodes.add(at.edge.getSmallerNode());
					numTrans++;
					
				}
			}
		}
	}
	
	/**
	 * Select actual transmissions from the potential transmission set to be scheduled in this slot.
	 * This works for the non-shortSchedule version.
	 * 
	 * @param potentialSet potential transmission set
	 */
	private final void selectActualTransmissions(List<? extends ActiveTransmission> potentialSet) {
		if (this.piggybacking) {
			List<OneTransmission> transmissionsInOneSlot = new ArrayList<OneTransmission>();
			int numTrans = 0;
			Map<Node, Integer> senders = new HashMap<Node, Integer>();
			Set<Node> receivers = new HashSet<Node>();
			Set<Edge> activeEdges = new HashSet<Edge>();
			Iterator<? extends ActiveTransmission> iter = potentialSet.iterator();
			while (iter.hasNext()) {
				ActiveTransmission at = iter.next();
				Node sender = at.sender;
				Node receiver = at.edge.getTheOtherNode(sender);
				//we can piggyback if the sender->receiver is already activated. 
				//or the sender is activated, but the receiver is not yet.
				if (senders.containsKey(sender)) {
					if ((!senders.containsKey(receiver) && !receivers.contains(receiver)) || activeEdges.contains(at.edge)) {
						Subpath subpath = at.subpath;
						OneTransmission oneTrans = new OneTransmission(subpath, at.edge, at.sender);
						transmissionsInOneSlot.add(oneTrans);
						transmitNext(at.flowState, oneTrans, sender, receiver);
						receivers.add(receiver);
						activeEdges.add(at.edge);
						this.piggybacks++;
						this.transmitted++;
						
						int times = senders.get(sender);
						if (times + 1 > this.maxPiggybackOnSender) {
							maxPiggybackOnSender = times+1;
						}
						senders.put(sender, times+1);
					}
				} else {
					//send to a node not appearing in the receiver.
					if (numTrans < this.getNumOfChannels() && !receivers.contains(sender) && !receivers.contains(receiver) 
							&& !senders.containsKey(receiver)) {
						Subpath subpath = at.subpath;
						OneTransmission oneTrans = new OneTransmission(subpath, at.edge, at.sender);
						transmissionsInOneSlot.add(oneTrans);
						transmitNext(at.flowState, oneTrans, sender, receiver);
						senders.put(sender, 1);
						receivers.add(receiver);
						activeEdges.add(at.edge);
						numTrans++;
						this.transmitted++;
					}
				}
			}
			this.scheduleBook.add(transmissionsInOneSlot);

			
		} else {
			List<OneTransmission> transmissionsInOneSlot = new ArrayList<OneTransmission>();
			int numTrans = 0;
			Set<Node> conflictNodes = new HashSet<Node>();
			Iterator<? extends ActiveTransmission> iter = potentialSet.iterator();
			while (numTrans < this.getNumOfChannels() && iter.hasNext()) {
				ActiveTransmission at = iter.next();
				
				Node node1 = at.edge.getBiggerNode();
				Node node2 = at.edge.getSmallerNode();
				
				if (!conflictNodes.contains(node1) && !conflictNodes.contains(node2)) {
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
		
	}
	
	/**
	 * Check whether the scheduling has ended?
	 * @return
	 */
	public boolean isScheduleEnded() {
		for (FlowTransmissionState flowState : this.flowStates) {
			if (!flowState.isFinished())
				return false;
		}
		return true;
	}
	
	protected void initFlowsOfPeriod(FlowsOfPeriod fp) {
		this.flowStates = new ArrayList<FlowTransmissionState>();
		for (Flow flow : fp.getFlows()) {
			FlowTransmissionState state = new FlowTransmissionState(this, flow);
			this.flowStates.add(state);
		}
	}
	
	/**
	 * initialize the pending packets. 
	 * The next transmission on every path is the 0th period and 0th hop.
	 */
	public void initPendingPackets() {
		
		this.flowStates = new ArrayList<FlowTransmissionState>();
		for (Flow flow : this.getNonEmptyFlows()) {
			FlowTransmissionState state = new FlowTransmissionState(this, flow);
			this.flowStates.add(state);
		}
		
		piggybacks = 0;
		transmitted = 0;
		maxPiggybackOnSender = 0;
	}
	
	protected void initActiveTransmission(FlowTransmissionState flowState, Subpath subpath, int hop, ActiveTransmission at) {
		at.sender = subpath.getSenderNodes().get(hop);
		at.edge = subpath.getEdges().get(hop);
		at.subpath = subpath;
		at.flowState = flowState;
	}
	
	
	public final String getName() {
		String name = this.getClass().getSimpleName();
		return name.substring(0, name.length() - 9);
	}

}
