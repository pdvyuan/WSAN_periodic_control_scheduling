package de.seemoo.dyuan.netgen.greenorbs;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.JFrame;
import javax.swing.JPanel;

import de.seemoo.dyuan.netgen.NetworkModel;
import de.seemoo.dyuan.netgen.structure.Edge;
import de.seemoo.dyuan.netgen.structure.Node;
import edu.uci.ics.jung.visualization.BasicVisualizationServer;

/**
 * A class for building the green orbs network from the topology 
 * data file 
 * 
 * @author dyuan
 */
public class BuildGreenOrbsNetwork {
	
	private BufferedReader reader;
	
	private NetworkModel network;
	
	private Map<Integer, NodeRecord> nodeTable;
	
	public NetworkModel buildNetwork(String filename) throws IOException {
		this.network = new NetworkModel();
		this.nodeTable = new HashMap<Integer, NodeRecord>();
		FileReader fReader = new FileReader(filename);
		this.reader = new BufferedReader(fReader);
		updateNetwork();
		fReader.close();
		return this.network;
	}

	private void updateNetwork() throws IOException {
		String line;
		while ((line = reader.readLine()) != null) {
			StringTokenizer tk = new StringTokenizer(line);
			tk.nextToken();
			tk.nextToken();
			int nodeId = Integer.parseInt(tk.nextToken());
			checkNodeAvailable(nodeId);
			createNodeRecordAndUpdateGraph(nodeId, tk);
		}
	}

	private void createNodeRecordAndUpdateGraph(int nodeId, StringTokenizer tk) {		
		NodeRecord nc = this.checkNodeAvailable(nodeId);
		nc.clearNeighbours();
		//clear links from nodeId
		Node node = nc.getNode();
		this.network.removeNeighbourLinks(node);
		
		int dstId = Integer.parseInt(tk.nextToken());
		checkNodeAvailable(dstId).setPathEtx(0);
		int parentId = Integer.parseInt(tk.nextToken());
		checkNodeAvailable(parentId);
		nc.setParentId(parentId);
		int neighbourSize = Integer.parseInt(tk.nextToken());
		nc.setPathEtx(Double.NaN);
		for (int i=0; i<neighbourSize; i++) {
			int neighbourId = Integer.parseInt(tk.nextToken());
			int rssi = Integer.parseInt(tk.nextToken());
			double pathEtx = Double.parseDouble(tk.nextToken());
			Neighbour neighbour = new Neighbour(neighbourId, pathEtx);
			nc.addNeighbour(neighbour);
			if (neighbourId == parentId) {
				nc.setPathEtx(pathEtx);
			}
			NodeRecord nnc = this.nodeTable.get(neighbourId);
			if (nnc != null && nnc.getPathEtx() != Double.NaN) {
				double linkEtx = pathEtx - nnc.getPathEtx();
				if (linkEtx >= 1.0) {
					network.addEdge(node, nnc.getNode(), linkEtx);
				}
			}
		}
		
	}
	
	private NodeRecord checkNodeAvailable(int nodeId) {
		if (!nodeTable.containsKey(nodeId)) {
			Node node = new Node(nodeId+"");
			node.setType(Node.NORMAL);
			network.addNormalNode(node);
			NodeRecord nc = new NodeRecord(node);
			nodeTable.put(nodeId, nc);
		} 
		return nodeTable.get(nodeId);
	}

	public static void main(String[] args) throws IOException {
		BuildGreenOrbsNetwork builder = new BuildGreenOrbsNetwork();
		NetworkModel net = builder.buildNetwork(args[0]);
		System.out.println("nodes: "+net.getGraph().getVertexCount());
		System.out.println("edge degree: "+2*net.getGraph().getEdgeCount()/(double)net.getGraph().getVertexCount());
		JFrame frame = new JFrame("green orbs");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel panel = new JPanel(new BorderLayout());
		frame.getContentPane().add(panel);
		final BasicVisualizationServer<Node, Edge> netComp = net.getGraphicComponent(); 
		panel.add(netComp, BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);
		
		int wrong = 0;
		for (Edge edge : net.getGraph().getEdges()) {
			if (edge.getLinkQuality() < 1.0 || edge.getLinkQuality() > 500) {
				wrong++;
			}
		}
		System.out.println("wrong rate = "+((double)wrong)/net.getGraph().getEdgeCount());
	}
}
