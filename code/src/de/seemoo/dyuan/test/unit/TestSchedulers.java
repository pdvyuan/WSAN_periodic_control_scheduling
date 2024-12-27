package de.seemoo.dyuan.test.unit;

import de.seemoo.dyuan.netgen.Flow;
import de.seemoo.dyuan.netgen.NetworkModel;
import de.seemoo.dyuan.netgen.structure.Edge;
import de.seemoo.dyuan.netgen.structure.Node;
import junit.framework.TestCase;

public class TestSchedulers extends TestCase {
	
	private NetworkModel network;
	private Node a, b, c, d, e;
	private Node g1, g2;
	private Edge ag1, g1b, bd, ac, cg2, g2b, g2e, ed;
	
	protected void setUp() {
		this.network = new NetworkModel();
		this.a = new Node("a");
		this.b = new Node("b");
		this.c = new Node("c");
		this.d = new Node("d");
		this.e = new Node("e");
		a.setType(Node.NORMAL);
		b.setType(Node.NORMAL);
		c.setType(Node.NORMAL);
		d.setType(Node.NORMAL);
		e.setType(Node.NORMAL);
		
		this.g1 = new Node("g1");
		g1.setType(Node.GATEWAY);
		this.g2 = new Node("g2");
		g2.setType(Node.GATEWAY);
		
		this.network.addNormalNode(a);
		this.network.addNormalNode(b);
		this.network.addNormalNode(c);
		this.network.addNormalNode(d);
		this.network.addNormalNode(e);
		
		this.network.addGateway(g1);
		this.network.addGateway(g2);
		
		this.ag1 = this.network.addEdge(a, g1, 1);
		this.g1b = this.network.addEdge(g1, b, 1);
		this.bd = this.network.addEdge(b, d, 1);
		this.ac = this.network.addEdge(a, c, 1);
		this.cg2 = this.network.addEdge(c, g2, 1);
		this.g2b = this.network.addEdge(g2, b, 1);
		this.g2e = this.network.addEdge(g2, e, 1);
		this.ed = this.network.addEdge(e, d, 1);
		
		Flow flow1 = this.network.newFlow(a, b);
		flow1.setPeriod(4);
		flow1.implicitDeadline();
		
	}

}
