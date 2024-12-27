package de.seemoo.dyuan.test.unit;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import de.seemoo.dyuan.netgen.Flow;
import de.seemoo.dyuan.netgen.NetworkModel;
import de.seemoo.dyuan.netgen.Subpath;
import de.seemoo.dyuan.netgen.structure.Edge;
import de.seemoo.dyuan.netgen.structure.Node;
import de.seemoo.dyuan.scheduler.SchedulerException;
import de.seemoo.dyuan.scheduler.cllf.CLLFScheduler;

public class TestNetworkGeneration extends TestCase {
	
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
	
	public void testOneFlow2Paths() {
		model.newFlow(n1, n2);
		List<Flow> flows = model.getFlows();
		assertEquals(1, flows.size());
		Flow flow = flows.get(0);
		assertFalse(flow.hasPath());
		model.doVertexDisjointRouting(2, NetworkModel.ROUTING_DISJOINT_MOST_RELIABLE);
		assertTrue(flow.hasPath());
		List<Subpath> uppaths = flow.getUpflowPaths();
		assertEquals(2, uppaths.size());
		Subpath uppath1 = uppaths.get(0);
		Subpath uppath2 = uppaths.get(1);
		assertEquals(1, uppath1.getSenderNodes().size());
		assertEquals(this.n1, uppath1.getSenderNodes().get(0));
		assertEquals(0.9, uppath1.getReliability());
		
		assertEquals(1, uppath2.getSenderNodes().size());
		assertEquals(this.n1, uppath2.getSenderNodes().get(0));
		assertEquals(0.9, uppath2.getReliability());
		
		List<Subpath> downpaths = flow.getDownflowPaths();
		assertEquals(2, downpaths.size());
		Subpath downpath1 = downpaths.get(0);
		Subpath downpath2 = downpaths.get(1);
		assertEquals(1, downpath1.getSenderNodes().size());
		assertEquals(this.g1, downpath1.getSenderNodes().get(0));
		assertEquals(0.9, downpath1.getReliability());
		
		assertEquals(2, downpath2.getSenderNodes().size());
		assertEquals(this.g2, downpath2.getSenderNodes().get(0));
		assertEquals(this.n3, downpath2.getSenderNodes().get(1));
		assertEquals(0.9 * 0.9, downpath2.getReliability());
		List<Edge> edges = downpath2.getEdges();
		
		assertTrue(edges.get(0).hasEnds(g2, n3));
		assertTrue(edges.get(1).hasEnds(n3, n2));
	}
	
	public void test2Flows1Path() {
		model.newFlow(n1, n2);
		model.newFlow(n2, n1);
		List<Flow> flows = model.getFlows();
		assertEquals(2, flows.size());
		Flow flow1 = flows.get(0);
		Flow flow2 = flows.get(1);
		model.doVertexDisjointRouting(1, NetworkModel.ROUTING_DISJOINT_MOST_RELIABLE);
		assertEquals(1, flow1.getUpflowPaths().size());
		assertEquals(1, flow1.getDownflowPaths().size());
		assertEquals(1, flow2.getUpflowPaths().size());
		assertEquals(1, flow2.getDownflowPaths().size());
		
		assertEquals(0.9, flow1.getUpflowPaths().get(0).getReliability());
		assertEquals(0.9, flow1.getDownflowPaths().get(0).getReliability());
		
		assertEquals(0.9, flow2.getUpflowPaths().get(0).getReliability());
		assertEquals(0.9, flow2.getDownflowPaths().get(0).getReliability());
	}
	
//	public void testScheduler() throws SchedulerException {
//		model.newFlow(n1, n2);
//		model.newFlow(n2, n1);
//		List<Flow> flows = model.getFlows();
//		Flow flow1 = flows.get(0);
//		Flow flow2 = flows.get(1);
//		flow1.createMostReliablePaths(1);
//		List<Edge> edges = new ArrayList<Edge>();
//		edges.add(n2n3);
//		edges.add(n3g2);
//		edges.add(g2n1);
//		Subpath path1 = new Subpath(edges, n2);
//		flow2.addPath(new Path(flow2, path1, new Subpath(null, null)));
//		flow1.setPeriod(3);
//		flow1.setDeadline(3);
//		flow2.setPeriod(6);
//		flow2.setDeadline(5);
//		OptimalBBScheduler scheduler = new OptimalBBScheduler(this.model, 2);
//		assertEquals(6, scheduler.getHyperPeriod());
//		SubSchedule subSch = scheduler.createRootSubSchedule();
//		int[][] transmissions = subSch.getPendingTransmissions();
//		assertEquals(2, transmissions.length);
//		for (int i=0; i<transmissions.length; i++) {
//			for (int j=0; j<transmissions[i].length; j++) {
//				assertEquals(0, transmissions[i][j]);
//			}
//		}
//		edges = subSch.computeTransmissionsInRange(0, 1, 4);
//		assertEquals(2, edges.size());
//		assertTrue(edges.get(0).hasEnds(g1, n2));
//		assertTrue(edges.get(1).hasEnds(n1, g1));
//		edges = subSch.computeTransmissionsInRange(1, 1, 4);
//		assertEquals(2, edges.size());
//		assertTrue(edges.get(0).hasEnds(n3, g2));
//		assertTrue(edges.get(1).hasEnds(g2, n1));
//		assertEquals(1, subSch.computeLaxityUpperBound());
//		subSch = subSch.schedulePathsAndGenNextSubSchedule(new int[]{0});
//		edges = subSch.computeTransmissionsInRange(0, 1, 3);
//		assertEquals(1, edges.size());
//		assertTrue(edges.get(0).hasEnds(g1, n2));
//		
//		edges = subSch.computeTransmissionsInRange(1, 1, 3);
//		assertEquals(2, edges.size());
//		assertTrue(edges.get(0).hasEnds(n2, n3));
//		assertTrue(edges.get(1).hasEnds(n3, g2));
//		
//		edges = subSch.computeTransmissionsInRange(0, 2, 4);
//		assertEquals(1, edges.size());
//		assertTrue(edges.get(0).hasEnds(n1, g1));
//		
//		edges = subSch.computeTransmissionsInRange(1, 2, 4);
//		assertEquals(2, edges.size());
//		assertTrue(edges.get(0).hasEnds(n3, g2));
//		assertTrue(edges.get(1).hasEnds(g2, n1));
//		
//		edges = subSch.computeTransmissionsInRange(0, 5, 10);
//		assertEquals(0, edges.size());
//		
//		edges = subSch.computeTransmissionsInRange(1, 4, 10);
//		assertEquals(0, edges.size());
//		assertEquals(0, subSch.computeLaxityUpperBound());
//		assertEquals(1, subSch.getCurrentSlotNum());
//		subSch = subSch.schedulePathsAndGenNextSubSchedule(new int[] {});
//		assertEquals(2, subSch.getCurrentSlotNum());
//		assertTrue(subSch.computeLaxityUpperBound() < 0);
//	}
	
//	public void testScheduler2() throws SchedulerException {
//		model.newFlow(n1, n2);
//		model.newFlow(n2, n1);
//		List<Flow> flows = model.getFlows();
//		Flow flow1 = flows.get(0);
//		Flow flow2 = flows.get(1);
//		flow1.createMostReliablePaths(1);
//		List<Edge> edges = new ArrayList<Edge>();
//		edges.add(n2n3);
//		edges.add(n3g2);
//		edges.add(g2n1);
//		Subpath path1 = new Subpath(edges, n2);
//		flow2.addPath(new Path(flow2, path1, new Subpath(null, null)));
//		flow1.setPeriod(3);
//		flow1.setDeadline(3);
//		flow2.setPeriod(6);
//		flow2.setDeadline(5);
//		OptimalBBScheduler scheduler = new OptimalBBScheduler(this.model, 2);
//		assertEquals(6, scheduler.getHyperPeriod());
//		SubSchedule subSch = scheduler.createRootSubSchedule();
//		assertEquals(1, subSch.computeLaxityUpperBound());
//		subSch.createAllPossibleSubSchedules();
//		assertEquals(3, subSch.getChildren().size());
//		subSch = subSch.schedulePathsAndGenNextSubSchedule(new int[]{0, 1});
////		System.out.println("transmission happened!");
//		assertEquals(1, subSch.computeLaxityUpperBound());
//		subSch = subSch.schedulePathsAndGenNextSubSchedule(new int[]{0, 1});
//		assertEquals(1, subSch.computeLaxityUpperBound());
//		try {
//			subSch = subSch.schedulePathsAndGenNextSubSchedule(new int[] {0});
//			fail("exception should happen");
//		} catch (SchedulerException e) {
//			assertEquals(SchedulerException.PACKET_NOT_RELEASED, e.getReason());
//		}
//		subSch = subSch.schedulePathsAndGenNextSubSchedule(new int[]{1});
//		assertEquals(3, subSch.getCurrentSlotNum());
//		assertEquals(1, subSch.computeLaxityUpperBound());
//	}
	
//	public void testBBMain() {
//		model.newFlow(n1, n2);
//		model.newFlow(n2, n1);
//		List<Flow> flows = model.getFlows();
//		Flow flow1 = flows.get(0);
//		Flow flow2 = flows.get(1);
//		flow1.createMostReliablePaths(1);
//		List<Edge> edges = new ArrayList<Edge>();
//		edges.add(n2n3);
//		edges.add(n3g2);
//		edges.add(g2n1);
//		Subpath path1 = new Subpath(edges, n2);
//		flow2.addPath(new Path(flow2, path1, new Subpath(null, null)));
//		flow1.setPeriod(12);
//		flow1.setDeadline(6);
//		flow2.setPeriod(6);
//		flow2.setDeadline(5);
//		OptimalBBScheduler scheduler = new OptimalBBScheduler(this.model, 2);
//		ScheduleResult ret = scheduler.schedule();
//		ret.print();
//		ret.validate();
//	}
	
	public void testCLLFMain() throws SchedulerException {
		model.newFlow(n1, n2);
		model.newFlow(n2, n1);
		List<Flow> flows = model.getFlows();
		Flow flow1 = flows.get(0);
		Flow flow2 = flows.get(1);
		model.doVertexDisjointRouting(1, NetworkModel.ROUTING_DISJOINT_MOST_RELIABLE);
		List<Edge> edges = new ArrayList<Edge>();
		edges.add(n2n3);
		edges.add(n3g2);
		Subpath path1 = new Subpath(edges, n2);
		flow2.addSubpath(path1, true);
		
		edges = new ArrayList<Edge>();
		edges.add(g2n1);
		Subpath path2 = new Subpath(edges, g2);
		flow2.addSubpath(path2, false);
		
		flow1.setPeriod(3);
		flow1.setDeadline(3);
		flow2.setPeriod(6);
		flow2.setDeadline(5);
		CLLFScheduler scheduler = new CLLFScheduler(this.model, false, false, 2);
		scheduler.initPendingPackets();
		int laxity = scheduler.computeMinLaxityOfTransmission(1, 0, 2, n2);
		System.out.println(laxity);
	}
	
}
