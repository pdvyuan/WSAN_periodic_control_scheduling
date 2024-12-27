package de.seemoo.dyuan.scheduler;


import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.seemoo.dyuan.netgen.Flow;
import de.seemoo.dyuan.netgen.NetworkModel;

/**
 * The base class of any scheduler.
 * @author dyuan
 *
 */
public abstract class SchedulerBase {
	
	protected NetworkModel network;
	
	protected List<Flow> nonEmptyFlows;
	
	protected int numChannels;
	
	protected int hyperPeriod;
	
	/*
	 * true: opportunistic aggregation
	 * false: no aggregation
	 */
	protected final boolean piggybacking; 
	
	/*
	 * true: repetitive scheduling
	 * false: repetitive scheduling
	 */
	protected final boolean shortSchedule;
	
	protected int piggybacks;
	
	protected int transmitted;
	
	protected int maxPiggybackOnSender;
	
	//each item has a number of flows of a certain period. The items are ordered 
	//according to period.
	protected List<FlowsOfPeriod> flowsOfPeriods;
	
	public static class FlowsOfPeriod {
		private int period;
		private List<Flow> flows;
		
		public FlowsOfPeriod(int period) {
			this.period = period;
			this.flows = new ArrayList<Flow>();
		}
		
		public int getPeriod() {
			return this.period;
		}
		
		public List<Flow> getFlows() {
			return this.flows;
		}
		
		public void addFlow(Flow flow) {
			this.flows.add(flow);
		}
	}
	
	
	/**
	 * Collect all non-empty paths from all the flows and store it in this class.
	 */
	private void collectNonEmptyFlows() {
		this.nonEmptyFlows = this.network.getNonEmptyFlows();
	}
	/**
	 * Return all paths of various flows.
	 */
	public List<Flow> getNonEmptyFlows() {
		return this.nonEmptyFlows;
	}
	
	public SchedulerBase(NetworkModel network, boolean piggyback, boolean shortSchedule, int nChannels) {
		this.network = network;
		this.numChannels = nChannels;
		this.piggybacking = piggyback;
		this.shortSchedule = shortSchedule;
		
		this.collectNonEmptyFlows();
		BigInteger period = network.computeHyperiod(false);
		if (period.compareTo(BigInteger.valueOf(NetworkModel.MAX_PERIOD)) == 1) {
			throw new IllegalArgumentException("hyper period is too big!");
		}
		this.hyperPeriod = period.intValue();
		
		if (this.shortSchedule) {
			Collections.sort(this.nonEmptyFlows, new Comparator<Flow>() {
				@Override
				public int compare(Flow f1, Flow f2) {
					return f1.getPeriod() - f2.getPeriod();
				}
				
			});
			this.flowsOfPeriods = new ArrayList<FlowsOfPeriod>();
			FlowsOfPeriod curr = null;
			for (Flow flow : this.nonEmptyFlows) {
				if (curr == null) {
					curr = new FlowsOfPeriod(flow.getPeriod());
					curr.addFlow(flow);
					this.flowsOfPeriods.add(curr);
				} else {
					if (flow.getPeriod() == curr.getPeriod()) {
						curr.addFlow(flow);
					} else {
						//check harmonious.
						for (FlowsOfPeriod fp: this.flowsOfPeriods) {
							if (flow.getPeriod() % fp.getPeriod() != 0)
								throw new IllegalArgumentException("this flows should have harmonious periods");
						}
						curr = new FlowsOfPeriod(flow.getPeriod());
						curr.addFlow(flow);
						this.flowsOfPeriods.add(curr);
					}
				}
			}	
		}
		
		for (int i=0; i<this.nonEmptyFlows.size(); i++) {
			//set flow id sequentially from 0 to each nonempty flow.
			nonEmptyFlows.get(i).setFlowId(i);
		}
		
	}
	/**
	 * 
	 * @return the hyper period length.
	 */
	public int getHyperPeriod() {
		return this.hyperPeriod;
	}
	
	/**
	 * 
	 * @return the number of channels.
	 */
	public int getNumOfChannels() {
		return this.numChannels;
	}
	
	/**
	 * The network model behind.
	 * @return
	 */
	public NetworkModel getNetwork() {
		return this.network;
	}
	/**
	 * Abstract method for performing the scheduling.
	 * @return schedule result.
	 */
	public abstract ScheduleResult schedule();
	/**
	 * Abstract method for the name of the scheduler.
	 */
	public abstract String getName();
	
	public boolean isPiggybacking() {
		return this.piggybacking;
	}
	
	public int getPiggybacks() {
		return this.piggybacks;
	}
	
	public int getTransmittedPackets() {
		return this.transmitted;
	}
	
	public int getMaxPiggybackOnSender() {
		return this.maxPiggybackOnSender;
	}
	
	public boolean isShortSchedule() {
		return this.shortSchedule;
	}
}
