package de.seemoo.dyuan.scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.seemoo.dyuan.netgen.Flow;
import de.seemoo.dyuan.netgen.Subpath;
import de.seemoo.dyuan.netgen.structure.Node;
import de.seemoo.dyuan.scheduler.HeuristicScheduler.OneTransmission;

/**
 * A class representing the result of a scheduling.
 * @author dyuan
 *
 */
public class ScheduleResult {
	
	public static final int FEASIBLE = 0;
	
	public static final int UNSCHEDULABLE = 1;
	
	public static final int CHECK_FAILURE = 2;
	
	private final int status;
	
	private final SchedulerBase scheduler;
	
	public int getStatus() {
		return this.status;
	}
	
	public String getStatusStr() {
		if (this.status == FEASIBLE)
			return "feasible";
		if (this.status == CHECK_FAILURE)
			return "check-failure";
		return "unschedulable";
	}
	
	public ScheduleResult(int status, SchedulerBase scheduler) {
		this.status = status;
		this.scheduler = scheduler;
	}
	
	private List<List<OneTransmission>> scheduleBook = new ArrayList<List<OneTransmission>>();
	
	public List<List<OneTransmission>> getScheduleBook() {
		return this.scheduleBook;
	}
//	/**
//	 * Create a successful schedule result from an ending node.
//	 * @param node the ending node
//	 * @return A schedule result.
//	 */
//	public static ScheduleResult createScheduleResultFromBBEndingNode(SubSchedule node, OptimalBBScheduler scheduler) {
//		if (!node.isScheduleEnded()) {
//			throw new IllegalArgumentException("the input should be an ended node");
//		}
//		ScheduleResult ret = new ScheduleResult(ScheduleResult.FEASIBLE, scheduler);
//		List<List<Integer>> schedules = new ArrayList<List<Integer>>();
//		while (node.getParent() != null) {	
//			schedules.add(0, node.getTransmissionsHappenedBeforeTheSlot());
//			node = node.getParent();
//		}		
//		ret.scheduleBook = schedules;
//		return ret;
//	}
	
	public static ScheduleResult createScheduleResultFromEndingHeuristicScheduler(HeuristicScheduler scheduler) {
		ScheduleResult ret = new ScheduleResult(ScheduleResult.FEASIBLE, scheduler);
		ret.scheduleBook = scheduler.getScheduleBook();
		return ret;
	}
	
	
	public static ScheduleResult createUnschedulableResult(SchedulerBase scheduler) {
		ScheduleResult ret = new ScheduleResult(ScheduleResult.UNSCHEDULABLE, scheduler);
		return ret;
	}
	
	public static ScheduleResult createCheckFailureResult(SchedulerBase scheduler) {
		ScheduleResult ret = new ScheduleResult(ScheduleResult.CHECK_FAILURE, scheduler);
		return ret;
	}
	
	public void printSimple() {
		
		System.out.printf("Schedule result: %s\n", this.getStatusStr());
	}
	
	private List<Flow> getFlows() {
		return ((HeuristicScheduler)this.scheduler).getNonEmptyFlows();
	}
	
	private int getNumOfPaths() {
		int total = 0;
		for (Flow flow : getFlows()) {
			int n = flow.getUpflowPaths().size();
			if (n != flow.getDownflowPaths().size())
				throw new IllegalArgumentException();
			total += n;
		}
		return total;
	}
	
	static class TaskExecution {
		public int startTime;
		public int endTime;
		
		public int taskId;
		
		public TaskExecution(int taskId) {
			this.startTime = -1;
			this.endTime = -1;
			this.taskId = taskId;
		}
	}
	
	private HashMap<Subpath, TaskExecution> taskExecutions;
	
	public void printRTGrid() {
		if (status != FEASIBLE)
			return;
		System.out.println("\\begin{figure}[htbp]");
		System.out.println("\\centering");
		System.out.printf("\\begin{RTGrid}[nosymbols=1,width=5cm]{%d}{%d}\n", this.getNumOfPaths(), this.scheduler.getHyperPeriod());
		
		int taskId = 1;
		int flowId = 0;
		for (Flow flow : this.getFlows()) {
			for (int pathId=0; pathId<flow.getUpflowPaths().size(); pathId++) {
				System.out.printf("\\RowLabel{%d}{$\\pi_{%d%d}$}\n", taskId, flowId, pathId);
				taskId++;
			}
			flowId++;
		}
		taskId = 1;
		this.taskExecutions = new HashMap<Subpath, TaskExecution>();
		for (Flow flow : this.getFlows()) {
			for (int pathId=0; pathId<flow.getUpflowPaths().size(); pathId++) {
				int arrTime = 0;
				while (arrTime < this.scheduler.getHyperPeriod()) {
					System.out.printf("\\TaskArrDead{%d}{%d}{%d}\n", taskId, arrTime, flow.getDeadline());
					arrTime += flow.getPeriod();
				}
				
				Subpath path = flow.getUpflowPaths().get(pathId);
				this.taskExecutions.put(path, new TaskExecution(taskId));
				path = flow.getDownflowPaths().get(pathId);
				this.taskExecutions.put(path, new TaskExecution(taskId));
				
				taskId++;
			}
		}

		for (int time = 0; time < this.scheduleBook.size(); time++) {
			List<OneTransmission> transes = this.scheduleBook.get(time);
			for (OneTransmission trans : transes) {
				Subpath subpath = trans.subpath;
				
				TaskExecution exe = this.taskExecutions.get(subpath);
				if (exe.startTime == -1) {
					exe.startTime = time;
					exe.endTime = time+1;
				} else {
					if (time == exe.endTime) {
						exe.endTime++;
					} else {
						System.out.printf("\\TaskExecution[fillstyle=%clines]{%d}{%d}{%d}\n", (subpath.isUpflow() ? 'v':'h')
								, exe.taskId, exe.startTime, exe.endTime);
						exe.startTime = time;
						exe.endTime = time+1;
					}
				}
			}
		}
		for (Subpath subpath : this.taskExecutions.keySet()) {
			TaskExecution exe = this.taskExecutions.get(subpath);
			if (exe.startTime != -1) {
				System.out.printf("\\TaskExecution[fillstyle=%clines]{%d}{%d}{%d}\n", (subpath.isUpflow() ? 'v':'h')
						, exe.taskId, exe.startTime, exe.endTime);
			}
		}
		System.out.println("\\end{RTGrid}");
		System.out.printf("\\caption{%s}\n", scheduler.getName());
		System.out.println("\\end{figure}");
	}
	
	
	public void print() {
		System.out.printf("Schedule result: %s\n", (status == FEASIBLE) ? "feasible" : "unschedulable");
//		for (int i=0; i<this.getScheduler().getNonEmptyFlows().size(); i++) {
//			printFlow(i);
//		}
		
		
		for (int i=0; i<this.scheduleBook.size(); i++) {
			System.out.print("#"+i+": ");
			List<OneTransmission> transes = this.scheduleBook.get(i);
			for (OneTransmission trans : transes) {
				Subpath subpath = trans.subpath;
				System.out.print("("+(subpath.getParentFlow().getFlowId()*2+subpath.getSubpathId())
						+','+(subpath.isUpflow() ? '^':'_')+") ");
				//System.out.print("("+trans.flowId+','+trans.subpathId+','+(trans.upflow ? '^':'_')+") ");
			}
			System.out.println();
		}
	}
	
	private void printFlow(int flowId) {
		System.out.println("flow "+flowId);
		for (int i=0; i<this.scheduleBook.size(); i++) {
			System.out.print("#"+i+":");
			List<OneTransmission> transes = this.scheduleBook.get(i);
			for (OneTransmission trans : transes) {
				Subpath sp = trans.subpath;
				if (sp.getParentFlow().getFlowId() == flowId) {
					int subpath = sp.getSubpathId();
					boolean upflow = sp.isUpflow();
					System.out.print(subpath + (upflow ? "^":"_") + " ");
				}
			}
			System.out.println();
		}
	}

	public SchedulerBase getScheduler() {
		return this.scheduler;
	}
	
	private int numBufferedInNetwork;
	
	private long bufferTimeProduct;
	
	public static class MemStats {
		public final int maxBuffer;
		public final long bufferTimeProduct;
		public MemStats(int maxBuf, long bufTimeProd) {
			this.maxBuffer = maxBuf;
			this.bufferTimeProduct = bufTimeProd;
		}
	}
	
	public int getScheduledTransmissionCount() {
		if (this.scheduler.isShortSchedule()) {
			return this.getReptitiveTransmissionCount();
		} else {
			return this.getHyperperiodTransmissionCount();
		}
	}
	
	public int getNonemptySlots() {
		if (this.status != ScheduleResult.FEASIBLE) {
			return 0;
		}
		int total = 0;
		for (List<OneTransmission> oneSlotTrans : this.scheduleBook) {
			if (oneSlotTrans.size() > 0)
				total++;
		}
		return total;
	}
	
	/**
	 * The number of transmissions to be scheduled for hyper-period scheduling 
	 */
	public int getHyperperiodTransmissionCount() {
		if (this.status != ScheduleResult.FEASIBLE)
			return 0;
		int total = 0;
		for (List<OneTransmission> oneSlotTrans : this.scheduleBook) {
			total += oneSlotTrans.size();
		}
		return total;
	}
	/**
	 * The number of transmissions to be scheduled for repetitive scheduling.
	 * @return
	 */
	public int getReptitiveTransmissionCount() {
		if (this.status != ScheduleResult.FEASIBLE) 
			return 0;
		int total = 0;
		for (Flow flow : this.scheduler.getNonEmptyFlows()) {
			total += flow.getAllPathHops();
		}
		return total;
	}
	
	public MemStats getMemStats() {
		if (this.status != ScheduleResult.FEASIBLE)
			return new MemStats(0, 0);
		else {
			Node.clearMaxBuffer();
			this.numBufferedInNetwork = 0;
			this.bufferTimeProduct = 0;
			
			for (Node node : this.scheduler.network.getGraph().getVertices()) {
				node.setNumPackets(0);
			}
			
			for (List<OneTransmission> oneSlotTrans : this.scheduleBook) {
				//we need to do this for each slot.
				this.bufferTimeProduct += this.numBufferedInNetwork;
				for (OneTransmission trans : oneSlotTrans) {
					Subpath subpath = trans.subpath;
					Node sender = trans.sender;
					Node receiver = trans.edge.getTheOtherNode(sender);
					transmit(subpath, sender, receiver);
				}
			}
			
			return new MemStats(Node.getMaxBuffer(), this.bufferTimeProduct);
		}
	}
	
	private void transmit(Subpath subpath, Node sender, Node receiver) {
		int change = 0;
		if (subpath.isUpflow()) {
			if (!sender.equals(subpath.getParentFlow().getSource())) {
				sender.decNumPackets();
				change--;
			}
			if (!receiver.isGateway()) {
				receiver.incNumPackets();
				change++;
			}
		} else {
			if (!sender.isGateway()) {
				sender.decNumPackets();
				change--;
			}
			//destination node.
			if (!receiver.equals(subpath.getParentFlow().getDestination())) {
				receiver.incNumPackets();
				change++;
			}
		}
		
		this.numBufferedInNetwork += change;
	}
	
	//each row stands for a path. the first item is the number of packet released but not finished.
	//the second item is the next hop to be scheduled. 
//	private int[][] scheduleState;

	public void validate() {
		if (this.status == ScheduleResult.FEASIBLE) {
			ValidatorScheduler validator = new ValidatorScheduler(this.getScheduler().network,
					this.getScheduler().isPiggybacking(), this.getScheduler().getNumOfChannels(), this);
			ScheduleResult res = validator.schedule();
			if (res.getStatus() != FEASIBLE) {
				throw new IllegalStateException("the scheduler validator should end successfully!");
			}
			int len1 = res.getScheduleBook().size();
			int len2 = this.getScheduleBook().size();
			if (len1 < len2) {
				for (int slot = len1; slot < len2; slot++) {
					List<OneTransmission> transes = this.getScheduleBook().get(slot);
					if (!transes.isEmpty()) {
						throw new IllegalStateException("the slot should be empty!");
					}
				}
			}
			
			if (len2 < len1) {
				for (int slot = len2; slot < len1; slot++) {
					List<OneTransmission> transes = res.getScheduleBook().get(slot);
					if (!transes.isEmpty()) {
						throw new IllegalStateException("the slot should be empty!");
					}
				}
			}
			
//			scheduleState = new int[scheduler.getPaths().size()][2];
//			for (int i=0; i<scheduleState.length; i++) {
//				scheduleState[i][0] = 0;
//				scheduleState[i][1] = 0;
//			}
//			for (int i=0; i<this.scheduler.getHyperPeriod(); i++) {
//				generatePackets(i);
//				if (i < this.scheduleBook.size())
//					checkScheduleInSlot(i);
//			}
//			checkScheduleEnded();
		}
	}

//	private void generatePackets(int slot) {
//		for (int i=0; i<this.scheduler.getPaths().size(); i++) {
//			Path path = scheduler.getPaths().get(i);
//			if (slot % path.getFlow().getPeriod() == 0) {
//				//release a new packet.
//				if (this.scheduleState[i][0] != 0)
//					throw new IllegalStateException("when a packet is released, the number of active packet should be 0");
//				this.scheduleState[i][0] = 1;
//				this.scheduleState[i][1] = 0;
//			}
//		}
//	}
//
//	private void checkScheduleEnded() {
//		for (int i=0; i<this.scheduleState.length; i++) {
//			if (this.scheduleState[i][0] != 0)
//				throw new IllegalStateException("there is still active packets");
//		}
//	}

//	private void checkScheduleInSlot(int slot) {
//		List<Edge> edges = new ArrayList<Edge>();
//		List<Integer> scheduleInTheSlot = this.scheduleBook.get(slot);		
//		for (int path : scheduleInTheSlot) {
//			Path apath = this.scheduler.getPaths().get(path);
//			if (this.scheduleState[path][0] != 1) {
//				throw new IllegalStateException("the active packet # of the path should be exactly 1");
//			}
//			int hop = this.scheduleState[path][1];
//			Edge edge = apath.getAllEdges().get(hop);
//			edges.add(edge);
//			hop++;
//			this.scheduleState[path][1] = hop;
//			if (hop == apath.getAllEdges().size()) {
//				//the current packet has arrived at the destination.
//				int slotUsed = slot % apath.getFlow().getPeriod() + 1;
//				if (slotUsed > apath.getFlow().getDeadline())
//					throw new IllegalStateException("a packet uses more slots than deadline");
//				this.scheduleState[path][0] = 0;
//			}
//		}
//		for (int i=0; i<edges.size(); i++) {
//			Edge edge1 = edges.get(i);
//			for (int j=i+1; j<edges.size(); j++) {
//				Edge edge2 = edges.get(j);
//				if (edge1.isConflict(edge2)) {
//					throw new IllegalStateException("slot "+slot+" concurrent edges are conflicting: "+edge1.description()
//							+" "+edge2.description());
//				}
//			}
//		}
//	}
	
}
