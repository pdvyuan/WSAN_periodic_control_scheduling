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
 * proportional deadline monotonic scheduling. it schedules a transmission based on its 
 * packet's relative subdeadline defined as the maximum relative dealine of the upflow or downflow
 * divided by the number of hops on the subpath.
 * 
 * See. paper "Real-Time Scheduling for WirelessHART Networks". And it was adapted as the scheduling model
 * is different between our implementation and in the paper.
 * 
 * The priority is set based on the subpath.
 * 
 * Work for both shortSchedule or not.
 * 
 * 
 * @author dyuan
 * 
 */
public class PDMScheduler extends FixedPriorityScheduler {

	public PDMScheduler(NetworkModel network, boolean piggyback, boolean shortSchedule, int nChannels) {
		super(network, piggyback, shortSchedule, nChannels);
	}
	
	private static class FlowSubpath {
		public Flow flow;
		public Subpath subpath;
		
		public FlowSubpath(Flow flow, Subpath subpath) {
			this.flow = flow;
			this.subpath = subpath;
		}
	}

	@Override
	protected void assignPriorities() {
		
		List<FlowSubpath> orderedFSes = new ArrayList<FlowSubpath>();
		for (Flow flow : this.nonEmptyFlows) {
			for (Subpath subpath : flow.getUpflowPaths()) {
				FlowSubpath fs = new FlowSubpath(flow, subpath);
				orderedFSes.add(fs);
			}
			for (Subpath subpath : flow.getDownflowPaths()) {
				FlowSubpath fs = new FlowSubpath(flow, subpath);
				orderedFSes.add(fs);
			}
		}

		Comparator<FlowSubpath> fsComparator = new Comparator<FlowSubpath>() {
			
			@Override
			public int compare(FlowSubpath fs1, FlowSubpath fs2) {
				boolean upflow1 = fs1.subpath.isUpflow();
				boolean upflow2 = fs2.subpath.isUpflow();
				int deadline1 = fs1.flow.getMaxSubflowDeadline(upflow1);
				int deadline2 = fs2.flow.getMaxSubflowDeadline(upflow2);
				double pd1 = ((double)deadline1) / fs1.subpath.getEdges().size();
				double pd2 = ((double)deadline2) / fs2.subpath.getEdges().size();
				if (pd1 < pd2)
					return -1;
				if (pd1 > pd2)
					return 1;
				return 0;
			}
			
		};
		Collections.sort(orderedFSes, fsComparator);
		for (int i=0; i<orderedFSes.size(); i++) {
			this.priorities.put(orderedFSes.get(i).subpath, i);
		}
	}

	@Override
	protected FPActiveTransmission newTransmission(FlowTransmissionState flowState, Subpath subpath, int hop) {
		FPActiveTransmission at = new FPActiveTransmission();
		this.initActiveTransmission(flowState, subpath, hop, at);
		at.route = subpath;
		return at;
	}


}
