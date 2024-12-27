package de.seemoo.dyuan.netgen;

import java.util.HashMap;
import java.util.Map;

import de.seemoo.dyuan.netgen.structure.Node;
import de.seemoo.dyuan.utils.Global;

/**
 * Generate a square topology, the grid must have L*L nodes with L >= 1.
 * Any inner points are connected with 4 surrounding points. 
 * 
 * @author dyuan
 *
 */
public class GridGen extends NetworkGenerator {

	
	private final int side;
	
	private Node[][] sensorGrid;

	private int linkQualityLowerBound;

	private int linkQualityUpperBound;

	private Map<Node, Coordinates> nodePositions;
	
	/**
	 * The square is side*side, has side*side sensor nodes.
	 * 
	 */
	public GridGen(NetworkModel network, int side) {
		super(network);
		nodePositions = new HashMap<Node, Coordinates>();
		this.side = side;
		this.sensorGrid = new Node[side][side];
		int id = 1;
		for (int i=0; i<side; i++) {
			for (int j=0; j<side; j++) {
				Node node = new Node(""+id);
				sensorGrid[i][j] = node;
				id++;
				network.addNormalNode(node);
				Coordinates co = new Coordinates(i, j);
				nodePositions.put(node, co);
			}
		}
	}
	
	public int getSide() {
		return this.side;
	}
	
	public Node[][] getSensorGrid() {
		return this.sensorGrid;
	}
	
	/**
	 * 
	 * Random edges according to the distance between two nodes and assign random link qualities to the edges.
	 * 
	 * @param lbLQ Link quality lower bound, a number in [0, 100]. the bound is inclusive.
	 * @param ubLQ Link quality upper bound, a number in [0, 100]. the bound is inclusive.
	 * 
	 */
	public void buildEdgeAndRandomLQ(int lbLQ, int ubLQ) {
		this.linkQualityLowerBound = lbLQ;
		this.linkQualityUpperBound = ubLQ;
		int delta = ubLQ - lbLQ;
		for (int i=0; i<side; i++) {
			for (int j=0; j<side; j++) {
				Node node = this.sensorGrid[i][j];
				if (i != 0) {
					double lq = (lbLQ + Global.randomGen.nextInt(delta+1)) / (100f);
					Node left = this.sensorGrid[i-1][j];
					network.addEdge(node, left, lq);
				}
				if (j != 0) {
					double lq = (lbLQ + Global.randomGen.nextInt(delta+1)) / (100f);
					Node up = this.sensorGrid[i][j-1];
					network.addEdge(node, up, lq);
				}
			}
		}
	}
	
	
	/**
	 * add n gateways to the network.
	 * gateways are put in the center of local grid as possible.
	 * 
	 */
	public void addGateways(int nGateway) {
		int widthDivisions = (int) Math.sqrt(nGateway);
		int heightDivisions = widthDivisions;
		if (widthDivisions * heightDivisions < nGateway) {
			widthDivisions++;
		}
		if (widthDivisions * heightDivisions < nGateway) {
			heightDivisions++;
		}
		double xDivision = (this.side-1) / (double)widthDivisions;
		double yDivision = (this.side-1) / (double)heightDivisions;
		
		for (int i=0; i<nGateway; i++) {
			int xId = i / heightDivisions;
			double yId;
			if (i == nGateway-1) {
				yId = ((i % heightDivisions) + heightDivisions-1)/2.0;
			} else {
				yId = i % heightDivisions;  
			}
			double x_co = (xId + 0.5) * xDivision;
			if (Math.floor(x_co) == x_co) {
				x_co += 0.5;
			}
			double y_co = (yId + 0.5) * yDivision;
			if (Math.floor(y_co) == y_co) {
				y_co += 0.5;
			}
			
			Node gateway = new Node(""+(i+1));
			network.addGateway(gateway);
			Coordinates co = new Coordinates(x_co, y_co);
			this.nodePositions.put(gateway, co);
		}
		
		int delta = this.linkQualityUpperBound - this.linkQualityLowerBound;
		for (Node gateway : this.network.gateways) {
			Coordinates co = this.nodePositions.get(gateway);
			double x_co = co.x;
			double y_co = co.y;
			int right = (int) x_co + 1;
			int down = (int) y_co + 1;			
			double lq = (this.linkQualityLowerBound + Global.randomGen.nextInt(delta+1)) / (100f);
			Node leftUp = this.sensorGrid[right-1][down-1];
			network.addEdge(gateway, leftUp, lq);
			if (down < side) {
				Node leftDown = this.sensorGrid[right-1][down];
				lq = (this.linkQualityLowerBound + Global.randomGen.nextInt(delta+1)) / (100f);
				network.addEdge(gateway, leftDown, lq);
			}
			if (right < side) {
				Node rightUp = this.sensorGrid[right][down-1];
				lq = (this.linkQualityLowerBound + Global.randomGen.nextInt(delta+1)) / (100f);
				network.addEdge(gateway, rightUp, lq);
			}
			if (down < side && right < side) {
				Node rightDown = this.sensorGrid[right][down];
				lq = (this.linkQualityLowerBound + Global.randomGen.nextInt(delta+1)) / (100f);
				network.addEdge(gateway, rightDown, lq);
			}
		}
		
	}

	public Coordinates getNodeCoordinates(Node node) {
		return this.nodePositions.get(node);
	}
	
}
