package de.seemoo.dyuan.test;

import javax.swing.JFrame;

import de.seemoo.dyuan.netgen.NetworkModel;
import de.seemoo.dyuan.netgen.structure.Node;

public class TestNetworkModel {

	public static void main(String[] args) {
		NetworkModel netModel = new NetworkModel();
		Node g1 = new Node("g1");
		g1.setType(Node.GATEWAY);
		Node g2 = new Node("g2");
		g2.setType(Node.GATEWAY);
		netModel.addGateway(g1);
		netModel.addGateway(g2);
		
		Node n1 = new Node("n1");
		n1.setType(Node.NORMAL);
		Node n2 = new Node("n2");
		n2.setType(Node.NORMAL);
		Node n3 = new Node("n3");
		n3.setType(Node.NORMAL);
		netModel.addNormalNode(n1);
		netModel.addNormalNode(n2);
		netModel.addNormalNode(n3);
		
		netModel.addEdge(g1, n1, 0.7);
		netModel.addEdge(n1, n2, 0.5);
		netModel.addEdge(n1, n3, 0.6);
		netModel.addEdge(n2, n3, 0.4);
		netModel.addEdge(g2, n3, 0.8);
		
		JFrame frame = new JFrame("Test Network Model");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(netModel.getGraphicComponent());
		frame.pack();
		frame.setVisible(true);
		
	}
	
	
}
