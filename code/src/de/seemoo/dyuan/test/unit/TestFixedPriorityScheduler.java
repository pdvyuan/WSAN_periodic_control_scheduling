package de.seemoo.dyuan.test.unit;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import de.seemoo.dyuan.netgen.Flow;
import de.seemoo.dyuan.netgen.NetworkModel;
import de.seemoo.dyuan.netgen.Subpath;
import de.seemoo.dyuan.netgen.structure.Edge;
import de.seemoo.dyuan.netgen.structure.Node;
import de.seemoo.dyuan.scheduler.HeuristicScheduler.OneTransmission;
import de.seemoo.dyuan.scheduler.ScheduleResult;
import de.seemoo.dyuan.scheduler.fixed_priority.RMScheduler;

public class TestFixedPriorityScheduler extends TestCase {
	
	private NetworkModel model;
	private Node g1, g2;
	private Node n1, n2, n3;
	
	private Edge n1g1, g1n2, n2n3, n3g2, g2n1;
	
	protected void setUp() {
		this.model = new NetworkModel();
		g1 = new Node("g1");
		g1.setType(Node.GATEWAY);
		g2 = new Node("g2");
		g2.setType(Node.GATEWAY);
		model.addGateway(g1);
		model.addGateway(g2);
		
		n1 = new Node("n1");
		n1.setType(Node.NORMAL);
		n2 = new Node("n2");
		n2.setType(Node.NORMAL);
		n3 = new Node("n3");
		n3.setType(Node.NORMAL);
		model.addNormalNode(n1);
		model.addNormalNode(n2);
		model.addNormalNode(n3);
		n1g1 = model.addEdge(n1, g1, 0.9);
		g1n2 = model.addEdge(g1, n2, 0.9);
		
		n2n3 = model.addEdge(n2, n3, 0.9);
		n3g2 = model.addEdge(n3, g2, 0.9);
		g2n1 = model.addEdge(g2, n1, 0.9);		
	}	
	
	public void testRMPriority() {
		model.newFlow(n1, n2);
		model.newFlow(n2, n1);
		Flow flow0 = model.getFlows().get(0);
		Flow flow1 = model.getFlows().get(1);
		model.doVertexDisjointRouting(2, NetworkModel.ROUTING_DISJOINT_MOST_RELIABLE);
		List<Subpath> uppaths = flow0.getSubpaths(true);
		List<Subpath> downpaths = flow0.getSubpaths(false);
		assertEquals(2, uppaths.size());
		assertEquals(2, downpaths.size());
		
		List<Edge> uppath1 = new ArrayList<Edge>();
		uppath1.add(n1g1);
		List<Edge> uppath2 = new ArrayList<Edge>();
		uppath2.add(g2n1);
		
		assertTrue(uppaths.get(0).getEdges().equals(uppath1) || uppaths.get(0).getEdges().equals(uppath2));
		assertTrue(uppaths.get(1).getEdges().equals(uppath1) || uppaths.get(1).getEdges().equals(uppath2));
		
		List<Edge> downpath1 = new ArrayList<Edge>();
		downpath1.add(g1n2);
		List<Edge> downpath2 = new ArrayList<Edge>();
		downpath2.add(n3g2);
		downpath2.add(n2n3);
		
		assertTrue(downpaths.get(0).getEdges().equals(downpath1) || downpaths.get(0).getEdges().equals(downpath2));
		assertTrue(downpaths.get(1).getEdges().equals(downpath1) || downpaths.get(1).getEdges().equals(downpath2));
		
		uppaths = flow1.getSubpaths(true);
		downpaths = flow1.getSubpaths(false);
		uppath1.clear();
		uppath1.add(g1n2);
		uppath2.clear();
		uppath2.add(n2n3);
		uppath2.add(n3g2);
		assertTrue(uppaths.get(0).getEdges().equals(uppath1) || uppaths.get(0).getEdges().equals(uppath2));
		assertTrue(uppaths.get(1).getEdges().equals(uppath1) || uppaths.get(1).getEdges().equals(uppath2));
		
		downpath1.clear();
		downpath1.add(n1g1);
		downpath2.clear();
		downpath2.add(g2n1);
		assertTrue(downpaths.get(0).getEdges().equals(downpath1) || downpaths.get(0).getEdges().equals(downpath2));
		assertTrue(downpaths.get(1).getEdges().equals(downpath1) || downpaths.get(1).getEdges().equals(downpath2));
		
		flow0.setPeriod(10);
		flow0.setDeadline(10);
		flow1.setPeriod(20);
		flow1.setDeadline(20);
		RMScheduler scheduler = new RMScheduler(this.model, false, false, 2);
		scheduler.initPendingPackets();
		scheduler.initScheduler();
		assertTrue(scheduler.getPriority(flow0) < scheduler.getPriority(flow1));
	}
	
	public void testRMSchedulingWaitAtGWs() {
		model.newFlow(n1, n2);
		model.newFlow(n2, n1);
		Flow flow0 = model.getFlows().get(0);
		Flow flow1 = model.getFlows().get(1);
		model.doVertexDisjointRouting(2, NetworkModel.ROUTING_DISJOINT_MOST_RELIABLE);
		flow0.setPeriod(5);
		flow0.implicitDeadline();
		flow1.setPeriod(10);
		flow1.implicitDeadline();
		RMScheduler scheduler = new RMScheduler(this.model, false, false, 2);
		ScheduleResult res = scheduler.schedule();
		res.validate();
		assertEquals(ScheduleResult.FEASIBLE, res.getStatus());
		assertEquals(10, res.getScheduleBook().size());

		this.checkSlot(new int[]{0, 0, 1, 1}, res.getScheduleBook().get(0));
		this.checkSlot(new int[]{0, 1, 1, 0}, res.getScheduleBook().get(1));
		this.checkSlot(new int[]{0, 0, 0, 1}, res.getScheduleBook().get(2));
		this.checkSlot(new int[]{0, 1}, res.getScheduleBook().get(3));
		this.checkSlot(new int[]{1, 1}, res.getScheduleBook().get(4));
		this.checkSlot(new int[]{0, 0}, res.getScheduleBook().get(5));
		this.checkSlot(new int[]{0, 1}, res.getScheduleBook().get(6));
		this.checkSlot(new int[]{0, 0, 0, 1}, res.getScheduleBook().get(7));
		this.checkSlot(new int[]{0, 1, 1, 0}, res.getScheduleBook().get(8));
		this.checkSlot(new int[]{1, 1}, res.getScheduleBook().get(9));
	}
	
	private void checkSlot(int[] flowAndPathIds, List<OneTransmission> transmissions) {
		assertEquals(flowAndPathIds.length, transmissions.size()*2);
		
		for (int i=0; i<transmissions.size(); i++) {
			OneTransmission trans = transmissions.get(i);
			int flowId = trans.subpath.getParentFlow().getFlowId();
			int pathId = trans.subpath.getSubpathId();
			int expFlowId = flowAndPathIds[2*i];
			int expPathId = flowAndPathIds[2*i+1];
			assertEquals(expFlowId, flowId);
			assertEquals(expPathId, pathId);
		
		}
	}
}
