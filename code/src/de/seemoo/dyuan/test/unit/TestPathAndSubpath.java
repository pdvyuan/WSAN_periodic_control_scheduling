package de.seemoo.dyuan.test.unit;

import java.util.ArrayList;
import java.util.List;

import de.seemoo.dyuan.netgen.Flow;
import de.seemoo.dyuan.netgen.NetworkModel;
import de.seemoo.dyuan.netgen.Subpath;
import de.seemoo.dyuan.netgen.structure.Edge;
import de.seemoo.dyuan.netgen.structure.Node;
import junit.framework.TestCase;

public class TestPathAndSubpath extends TestCase {
	
	public void test3SubpathDeadlines() {
		NetworkModel model = new NetworkModel();
		Node gw = new Node("gw");
		gw.setType(Node.GATEWAY);
		model.addGateway(gw);
		
		int numNodes = 5;
		Node[] nodes = new Node[numNodes];
		for (int i=1; i<=numNodes; i++) {
			Node node = new Node(i+"");
			nodes[i-1] = node;
			node.setType(Node.NORMAL);
			model.addNormalNode(node);
		}
		
		Edge e1 = model.addEdge(nodes[0], nodes[1], 1);
		Edge e2 = model.addEdge(nodes[1], nodes[2], 1);
		Edge e3 = model.addEdge(nodes[2], gw, 1);
		Edge e4 = model.addEdge(gw, nodes[3], 1);
		Edge e5 = model.addEdge(nodes[3], nodes[4], 1);
		Flow flow = new Flow(model, nodes[0], nodes[4]);
		List<Edge> upEdges = new ArrayList<Edge>();
		upEdges.add(e1);
		upEdges.add(e2);
		upEdges.add(e3);
		Subpath up = new Subpath(upEdges, nodes[0]);
		assertEquals(3, up.getSenderNodes().size());
		assertEquals(nodes[0], up.getSenderNodes().get(0));
		assertEquals(nodes[1], up.getSenderNodes().get(1));
		assertEquals(nodes[2], up.getSenderNodes().get(2));
		assertEquals(nodes[0], up.getFirstNode());
		assertEquals(gw, up.getLastNode());
		
		List<Edge> downEdges = new ArrayList<Edge>();
		downEdges.add(e4);
		downEdges.add(e5);
		Subpath down = new Subpath(downEdges, gw);
		assertEquals(2, down.getSenderNodes().size());
		assertEquals(gw, down.getSenderNodes().get(0));
		assertEquals(nodes[3], down.getSenderNodes().get(1));
		assertEquals(gw, down.getFirstNode());
		assertEquals(nodes[4], down.getLastNode());
		
		flow.addSubpath(up, true);
		flow.addSubpath(down, false);
		
		flow.setPeriod(10);
		flow.setDeadline(8);
		
		
		assertEquals(6, up.getEffectiveDeadline());
		assertEquals(8, down.getEffectiveDeadline());
	}

}
