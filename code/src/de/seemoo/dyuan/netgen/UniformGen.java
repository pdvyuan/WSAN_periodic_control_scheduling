package de.seemoo.dyuan.netgen;

import gateway_placement.GWPlacer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.linear.EigenDecomposition;
import org.apache.commons.math.linear.EigenDecompositionImpl;
import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.linear.RealVector;

import com.mathworks.toolbox.javabuilder.MWArray;
import com.mathworks.toolbox.javabuilder.MWClassID;
import com.mathworks.toolbox.javabuilder.MWComplexity;
import com.mathworks.toolbox.javabuilder.MWNumericArray;

import de.seemoo.dyuan.netgen.structure.Edge;
import de.seemoo.dyuan.netgen.structure.Node;
import de.seemoo.dyuan.utils.Global;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

/**
 * Create a uniform rectangle space, put a number of nodes 
 * that is uniformly distributed in the network. 
 * 
 * Link quality depends on the distance between nodes.
 * 
 * @author dyuan
 *
 */
public class UniformGen extends NetworkGenerator {
	
	private final int linkQualityLowerBound;
	
	private final int linkQualityUpperBound;
	
	private final boolean physicalModel;

	public UniformGen(NetworkModel model, double commRange, int linkLB, int linkUB) {
		super(model);
		this.physicalModel = false;
		this.comm_range = commRange;
		this.linkQualityLowerBound = linkLB;
		this.linkQualityUpperBound = linkUB;
	}
	
	public UniformGen(NetworkModel model, int linkLB, int linkUB) {
		super(model);
		this.physicalModel = true;
		this.linkQualityLowerBound = linkLB;
		this.linkQualityUpperBound = linkUB;
	}

	private double comm_range;
	
	
	private Map<Node, Coordinates> nodePositions;
	
	private double side;
	
	private GWPlacer matlabPlacer = null;
	
	public double getSide() {
		return side;
	}
	
	public static int DISTANCE_COMPUTING_ALL_NODES = 0;
	
	public static int DISTANCE_COMPUTING_END_NODES = 1;
	
	public static int DISTANCE_COMPUTING_WEIGHTED_END_NODES = 2;
	
	
	/**
	 * add sensor nodes
	 * These nodes are randomly distributed between a rectangle of (0, 0) - (width, height).
	 */
	public void addSensors(int nNodes, double side) {
		this.side = side;
		this.nodePositions = new HashMap<Node, Coordinates>();
		for (int i=1; i<=nNodes; i++) {
			Node node = new Node(""+i);
			network.addNormalNode(node);
			double x = Global.randomGen.nextDouble() * side;
			double y = Global.randomGen.nextDouble() * side;
			Coordinates co = new Coordinates(x, y);
			nodePositions.put(node, co);
		}
	}
	
	/**
	 * 
	 * Random edges according to the distance between two nodes and assign random link qualities to the edges.
	 * 
	 * @param lbLQ Link quality lower bound, a number in [0, 100]. the bound is inclusive.
	 * @param ubLQ Link quality upper bound, a number in [0, 100]. the bound is inclusive.
	 * 
	 */
	public void buildEdgeAndRandomLQ() {
		int delta = linkQualityUpperBound - linkQualityLowerBound;
		//outgoing link;
		for (int i=0; i<this.network.normalNodes.size(); i++) {
			Node node1 = this.network.normalNodes.get(i);
			Coordinates co1 = this.nodePositions.get(node1);
			for (int j=i+1; j<this.network.normalNodes.size(); j++) {
				Node node2 = this.network.normalNodes.get(j);
				Coordinates co2 = this.nodePositions.get(node2);
				double dist = co1.euclidDistance(co2);
				if (this.physicalModel) {
					double lq = Global.genLinkQuality(dist);
					double lqPercent = lq * 100;
					if (lqPercent >= linkQualityLowerBound && lqPercent <= linkQualityUpperBound) {
						network.addEdge(node1, node2, lq);
					}
				} else {
					if (dist <= comm_range) {
						double lq = (linkQualityLowerBound + Global.randomGen.nextInt(delta+1)) / (100f);
						network.addEdge(node1, node2, lq);
					}
				}
			}
		}
	}
	
	private Map<Node, Coordinates> gwPos; 
	
	public static class NodeAndFlow {
		public Node node;
		public Flow flow;
		
		public boolean equals(Object o) {
			if (!(o instanceof NodeAndFlow))
				return false;
			NodeAndFlow no = (NodeAndFlow) o;
			if (this.node.equals(no.node)) {
				return true;
			}
			return false;
		}
		
		public int hashCode() {
			int hashCode = node.hashCode() * 31;
			if (flow != null)
				hashCode += flow.hashCode();
			return hashCode;
		}
	}
	
	private Map<Node, Set<NodeAndFlow>> gwClusters; 
	
	public static int TRY_TIMES = 10;
	
	/**
	 * Compute the sum distance square.
	 */
	private double computeSumDistance(int type) {
		double sum = 0;
		for (Map.Entry<Node, Set<NodeAndFlow>> entry : gwClusters.entrySet()) {
			Node gateway = entry.getKey();
			Coordinates gwCo = this.gwPos.get(gateway);
			Set<NodeAndFlow> cluster = entry.getValue();
			for (NodeAndFlow nf : cluster) {
				Coordinates nodeCo = this.nodePositions.get(nf.node);
				int weight;
				if (type == DISTANCE_COMPUTING_WEIGHTED_END_NODES) {
					weight = nf.flow.getPeriod();
				} else {
					weight = 1;
				}
				sum += Math.pow(nodeCo.x - gwCo.x, 2) / weight + Math.pow(nodeCo.y - gwCo.y, 2) / weight;					
			}
		}
		return sum;
	}
	

	/**
	 * Add n same gateways to the point with the min sum distance to all nodes (weighted).
	 * 
	 * Assumption, the gateways should be placed before the flow paths are set.
	 * 
	 * checked.
	 */
	public void addSameGatewaysMinSumDistToGWs(int nGateway, boolean perfectLink, 
			int type) throws Exception {
		clearGateways();
		
		//create n gateways and assign them random coordinates.
		for (int i=1; i<=nGateway; i++) {
			Node gateway = new Node(i+"");
			network.addGateway(gateway);
		}
		
		int numSensors;
		if (type == DISTANCE_COMPUTING_ALL_NODES) {
			numSensors = network.getNormalNodes().size();
		} else {
			numSensors = 2 * network.getFlows().size();
		}
				
		gwPos = new HashMap<Node, Coordinates>();
		
		if (numSensors == 0) {
			//if no flows. put gateways in the center.
			for (int i=0; i<network.getGateways().size(); i++) {
				Node gw = network.getGateways().get(i);
				Coordinates co = new Coordinates(this.side / 2.0, this.side / 2.0);
				gwPos.put(gw, co);
			}
		} else {
			double[][] sensorData = new double[numSensors][];
			int id = 0;
			int weight;
			if (type == DISTANCE_COMPUTING_ALL_NODES) {
				weight = 1;
				for (Node node : this.network.getNormalNodes()) {
					Coordinates co = this.nodePositions.get(node);
					sensorData[id] = new double[3];
					sensorData[id][0] = co.x;
					sensorData[id][1] = co.y;
					sensorData[id][2] = weight;
					id++;
				}
			} else {
				for (Flow flow : network.getFlows()) {
					if (type == DISTANCE_COMPUTING_END_NODES) {
						weight = 1;
					} else {
						weight = flow.getPeriod();
					}
					Node node = flow.getSource();
					Coordinates co = this.nodePositions.get(node);
					sensorData[id] = new double[3];
					sensorData[id][0] = co.x;
					sensorData[id][1] = co.y;
					sensorData[id][2] = weight;
					id++;
					
					node = flow.getDestination();
					co = this.nodePositions.get(node);
					sensorData[id] = new double[3];
					sensorData[id][0] = co.x;
					sensorData[id][1] = co.y;
					sensorData[id][2] = weight;
					id++;	
				}
			}
			
			
			if (matlabPlacer == null) {
				matlabPlacer = new GWPlacer();
			}
			
			int[] dims = {numSensors, 3};
			MWNumericArray matlabSensorsData = null;
			Object[] result = null;
			try {
				matlabSensorsData = MWNumericArray.newInstance(dims, MWClassID.DOUBLE, MWComplexity.REAL);
				
				int[] index = {1, 1};
				for (index[0] = 1; index[0]<=numSensors; index[0]++) {
					for (index[1]=1; index[1]<=3; index[1]++) {
						matlabSensorsData.set(index, sensorData[index[0]-1][index[1]-1]);
					}
				}

				result = matlabPlacer.computeMinSumDistGW(1, matlabSensorsData, this.side, this.side);
				MWNumericArray matlabGWPos = (MWNumericArray) result[0];
				double[][] values = (double[][]) matlabGWPos.toDoubleArray();
				double x = values[0][0];
				double y = values[0][1];
				for (int i=0; i<network.getGateways().size(); i++) {
					Node gw = network.getGateways().get(i);
					Coordinates co = new Coordinates(x, y);
					gwPos.put(gw, co);
				}
				
			} catch(Exception e) {
				e.printStackTrace();
			} finally {
				MWArray.disposeArray(matlabSensorsData);
				MWArray.disposeArray(result);
			}
		}
		
		
		this.nodePositions.putAll(this.gwPos);
		
		setupGatewaysLinks(perfectLink);
		
	}
	
	
		
	/**
	 * add nGateways which minimizes the sum distance of all source-destination sensors 
	 * to their nearest GW.
	 * c.f. "Deploying multiple sinks in multi-hop wireless sensor networks" by Vincze et al.
	 * call Matlab code to do the work.
	 * 
	 * @param nGateway number of gateways.
	 * 
	 * @param replaceGW true, keep the current flows, delete existing GWs and add new ones.
	 * @throws Exception 
	 * 
	 * checked.
	 * 
	 */
	public void addGatewaysMinSumDistToNearestGW(int nGateway, boolean perfectLink, int type) throws Exception {
		clearGateways();
		
		//create n gateways and assign them random coordinates.
		for (int i=1; i<=nGateway; i++) {
			Node gateway = new Node(i+"");
			network.addGateway(gateway);
		}
		
		int numSensors;
		if (type == DISTANCE_COMPUTING_ALL_NODES) {
			numSensors = network.getNormalNodes().size();
		} else {
			numSensors = 2 * network.getFlows().size();
		}
		
		
		double[][] sensorData = new double[numSensors][];
		int id = 0;
		
		int weight;
		
		if (type == DISTANCE_COMPUTING_ALL_NODES) {
			weight = 1;
			for (Node node : network.getNormalNodes()) {
				Coordinates co = this.nodePositions.get(node);
				sensorData[id] = new double[3];
				sensorData[id][0] = co.x;
				sensorData[id][1] = co.y;
				sensorData[id][2] = weight;
				id++;
			}
		} else {
			for (Flow flow : network.getFlows()) {
				if (type == DISTANCE_COMPUTING_END_NODES) {
					weight = 1;
				} else {
					weight = flow.getPeriod();
				}
				
				Node node = flow.getSource();
				Coordinates co = this.nodePositions.get(node);
				sensorData[id] = new double[3];
				sensorData[id][0] = co.x;
				sensorData[id][1] = co.y;
				sensorData[id][2] = weight;
				id++;
				
				node = flow.getDestination();
				co = this.nodePositions.get(node);
				sensorData[id] = new double[3];
				sensorData[id][0] = co.x;
				sensorData[id][1] = co.y;
				sensorData[id][2] = weight;
				id++;		
			}
		}
		
		gwPos = new HashMap<Node, Coordinates>();
		
		if (matlabPlacer == null) {
			matlabPlacer = new GWPlacer();
		}
		
//		for (int i=0; i<sensorData.length; i++) {
//			System.out.printf("%.1f, %.1f, %.1f\n", sensorData[i][0], sensorData[i][1], sensorData[i][2]);
//		}
		
		int[] dims = {numSensors, 3};
		MWNumericArray matlabSensorsData = null;
		Object[] result = null;
		try {
			matlabSensorsData = MWNumericArray.newInstance(dims, MWClassID.DOUBLE, MWComplexity.REAL);
			
			int[] index = {1, 1};
			for (index[0] = 1; index[0]<=numSensors; index[0]++) {
				for (index[1]=1; index[1]<=3; index[1]++) {
					matlabSensorsData.set(index, sensorData[index[0]-1][index[1]-1]);
				}
			}

			result = matlabPlacer.computeGWsMinSumDist(1, matlabSensorsData, nGateway, this.side, this.side);
			MWNumericArray matlabGWPos = (MWNumericArray) result[0];
			double[][] values = (double[][]) matlabGWPos.toDoubleArray();
			for (int i=0; i<network.getGateways().size(); i++) {
				Node gw = network.getGateways().get(i);
				double x = values[i][0]; 
				double y = values[i][1]; 
				Coordinates co = new Coordinates(x, y);
				gwPos.put(gw, co);
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			MWArray.disposeArray(matlabSensorsData);
			MWArray.disposeArray(result);
		}
		
//		for (Coordinates co : this.gwPos.values()) {
//			System.err.printf("%.3f, %.3f\n", co.x, co.y);
//		}
		
		this.nodePositions.putAll(this.gwPos);
		
		setupGatewaysLinks(perfectLink);
		
	}

	/**
	 * Precondition: the GWs are added to the network and their positions are already fixed.
	 * 	In addition, the link quality range should already be set.
	 */
	private void setupGatewaysLinks(boolean perfectLink) {
		int delta = this.linkQualityUpperBound - this.linkQualityLowerBound;
		for (Node gateway : this.network.gateways) {
			Coordinates co1 = this.nodePositions.get(gateway);
			for (int i=0; i<this.network.normalNodes.size(); i++) {
				Node node = this.network.normalNodes.get(i);
				Coordinates co2 = this.nodePositions.get(node);
				double dist = co1.euclidDistance(co2);
			
				if (this.isPhysicalModelUsed()) {
					double lq;
					if (perfectLink) {
						lq = 1;
						network.addEdge(gateway, node, lq);
					} else {
						lq = Global.genLinkQuality(dist);
						double lqPercent = lq * 100;
						if (lqPercent >= this.linkQualityLowerBound && lqPercent <= this.linkQualityUpperBound) {
							network.addEdge(gateway, node, lq);
						}
					}
				} else {
					if (dist <= comm_range) {
						double lq;
						if (perfectLink)
							lq = 1;
						else
							lq = (this.linkQualityLowerBound + Global.randomGen.nextInt(delta+1)) / (100f);
						network.addEdge(gateway, node, lq);
					}
				}				
			}
		}
	}

	private void clearGateways() {		
		for (Node oldGW : network.gateways) {
			network.graph.removeVertex(oldGW);
			this.nodePositions.remove(oldGW);
		}
		network.clearGateways();
	}
	
	
	
	/**
	 * add nGateways which minimizes the sum square distance of all source-destination sensors 
	 * to their nearest GW.
	 * also c.f. "Deploying multiple sinks in multi-hop wireless sensor networks" by Vincze et al.
	 * 
	 * @param nGateway number of gateways.
	 * 
	 * @param replaceGW true, keep the current flows, delete existing GWs and add new ones.
	 * 
	 * checked.
	 */
	public void addGatewaysMinSumSquareDistToNearestGW(int nGateway, boolean perfectLink, int type) {
		this.clearGateways();
		
		Map<Node, Coordinates> bestGWPos = null;
		Map<Node, Set<NodeAndFlow>> bestGWClusters = null;
		double bestDist = Double.MAX_VALUE;
		
		//create n gateways and assign them random coordinates.
		for (int i=1; i<=nGateway; i++) {
			Node gateway = new Node(i+"");
			network.addGateway(gateway);
		}
		for (int times = 0; times < TRY_TIMES; times++) {
			gwPos = new HashMap<Node, Coordinates>();
			gwClusters = new HashMap<Node, Set<NodeAndFlow>>();
			for (Node gw : network.getGateways()) {
				gwPos.put(gw, new Coordinates(Global.randomGen.nextDouble()*side, Global.randomGen.nextDouble()*side));
				gwClusters.put(gw, new HashSet<NodeAndFlow>());
			}

			while (true) {
				boolean sameCluster = clusterNodesToGateways(type);
				if (sameCluster)
					break;
				replaceGateways(type);			
			}
			double dist = this.computeSumDistance(type);
			if (dist < bestDist) {
				bestDist = dist;
				bestGWPos = gwPos;
				bestGWClusters = gwClusters;
			}
		}
		if (bestGWPos == null || this.gwPos.size() != nGateway) {
			throw new IllegalStateException();
		}
		this.gwPos = bestGWPos;
		this.gwClusters = bestGWClusters;
		
		
		this.nodePositions.putAll(this.gwPos);
		
		setupGatewaysLinks(perfectLink);
	}
	
	/**
	 * 
	 * Add GWs to the same place which minimizes the min sum square distance.
	 * 
	 * checked.
	 */
	public void addSameGatewaysMinSumSquareDistToGWs(int nGateway, boolean perfectLink, int type) {
		clearGateways();
		
		Coordinates pos = computeGWPositionMinSquareDist(type);
		gwPos = new HashMap<Node, Coordinates>();
		
		//create n gateways and assign them random coordinates.
		for (int i=1; i<=nGateway; i++) {
			Node gateway = new Node(i+"");
			network.addGateway(gateway);
			gwPos.put(gateway, new Coordinates(pos));
		}

		this.nodePositions.putAll(this.gwPos);
		
		setupGatewaysLinks(perfectLink);
	}
	
	
	//private static double gradient_error = 1e-6;
	
	private Coordinates computeGWPositionMinSquareDist(int type) {
		double x = 0;
		double y = 0;
		double totalWeight = 0;
		int weight;
		if (type == DISTANCE_COMPUTING_ALL_NODES) {
			weight = 1;
			for (Node node : this.network.getNormalNodes()) {
				Coordinates nodeCo = this.nodePositions.get(node);
				x += nodeCo.x / weight;
				y += nodeCo.y / weight;
				totalWeight += 1.0 / weight;
			}
		} else {
			for (Flow flow : this.network.getFlows()) {
				if (type == DISTANCE_COMPUTING_END_NODES) {
					weight = 1;
				} else {
					weight = flow.getPeriod();
				}
				
				Node[] nodes = new Node[] {flow.getSource(), flow.getDestination()};
				for (Node node : nodes) {
					Coordinates nodeCo = this.nodePositions.get(node);
					x += nodeCo.x / weight;
					y += nodeCo.y / weight;
					totalWeight += 1.0 / weight;
				}
				
			}
		}
		
		if (totalWeight != 0) {
			x = x / totalWeight;
			y = y / totalWeight;
		}
		return new Coordinates(x, y);
	}
	
	/**
	 * Replace the gateway according to each cluster, place the gw at the best place.
	 * The method used is the newton's method.
	 * 
	 */
	private void replaceGateways(int type) {
		for (Map.Entry<Node, Set<NodeAndFlow>> entry : this.gwClusters.entrySet()) {
			Node gateway = entry.getKey();
			Coordinates gwCo = this.gwPos.get(gateway);
			Set<NodeAndFlow> clusteredNodes = entry.getValue();
			double x = 0;
			double y = 0;
			double totalWeight = 0;
			
			for (NodeAndFlow nf : clusteredNodes) {
				int weight;
				if (type == DISTANCE_COMPUTING_WEIGHTED_END_NODES) {
					weight = nf.flow.getPeriod();
				} else {
					weight = 1;
				}
				Coordinates nodeCo = this.nodePositions.get(nf.node);
				x += nodeCo.x / weight;
				y += nodeCo.y / weight;
				totalWeight += 1.0 / weight;
				
			}
			if (clusteredNodes.size() != 0) {
				x = x / totalWeight;
				y = y / totalWeight;
			}
//			System.err.printf("Node %s, move from (%.1f, %.1f) to (%.1f, %.1f)\n", 
//					gateway.getId(), gwCo.x, gwCo.y, x, y);
			
			gwCo.x = x;
			gwCo.y = y;
			
//			while (true) {
//				double gradientX = 0;
//				double gradientY = 0;
//				double h11 = 0;
//				double h12 = 0;
//				double h22 = 0;
//				for (Node node : clusteredNodes) {
//					Coordinates nodeCo = this.nodePositions.get(node);
//					double dist = nodeCo.euclidDistance(gwCo);
//					double deltaX = gwCo.x - nodeCo.x;
//					double deltaY = gwCo.y - nodeCo.y;
//					gradientX += deltaX / dist;
//					gradientY += deltaY / dist;
//					h11 += 1/dist - Math.pow(deltaX, 2) / Math.pow(dist, 3);
//					h22 += 1/dist - Math.pow(deltaY, 2) / Math.pow(dist, 3);
//					h12 -= deltaX * deltaY / Math.pow(dist, 3);
//				}
//				//System.out.printf("gw = %s, gx = %.3f, gy = %.3f\n", gateway.getId(), gradientX, gradientY);
//				if (gradientX*gradientX + gradientY*gradientY < gradient_error) {
//					break;
//				}
//				RealMatrix gradM = new Array2DRowRealMatrix(new double[][]{{gradientX}, {gradientY}});
//				RealMatrix hessian;
//				//System.out.printf("%.3f, %.3f | %.3f %.3f %.3f %.3f\n", gradientX, gradientY, h11, h12, h12, h22);
//				try {
//					hessian = new Array2DRowRealMatrix(new double[][]{{h11, h12}, {h12, h22}});
//					hessian = new LUDecompositionImpl(hessian).getSolver().getInverse();
//				} catch (Exception e) {
//					hessian = new Array2DRowRealMatrix(new double[][]{{step, 0}, {0, step}});
//				}
//		
//				gwCoM = gwCoM.subtract(hessian.multiply(gradM));
//				gwCo.x = gwCoM.getEntry(0, 0);
//				gwCo.y = gwCoM.getEntry(1, 0);
//				removed = true;
//			}			
		}
	}

	/**
	 * Find the nearest gateway of a node
	 */
	private Node findTheNearestGW(Node node) {
		double dist = Double.MAX_VALUE;
		Node gateway = null;
		Coordinates nodeCo = nodePositions.get(node);
		Iterator<Map.Entry<Node, Coordinates>> iter = gwPos.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<Node, Coordinates> entry = iter.next();
			Node gw = entry.getKey();
			Coordinates gwCo = entry.getValue();
			double d = gwCo.euclidDistance(nodeCo); 
			if (d < dist) {
				dist = d;
				gateway = gw;
			}
		}
		return gateway;		
	}
	
	
	/**
	 * Cluster the source and destination nodes to the nearest gateway.
	 * @return same as old clustering. 
	 */
	private boolean clusterNodesToGateways(int type) {
		Map<Node, Set<NodeAndFlow>> newGWClusters = new HashMap<Node, Set<NodeAndFlow>>();
		for (Node gateway : gwPos.keySet()) {
			newGWClusters.put(gateway, new HashSet<NodeAndFlow>());
		}
		
		if (type == DISTANCE_COMPUTING_ALL_NODES) {
			for (Node node : this.network.getNormalNodes()) {
				Node gw = this.findTheNearestGW(node);
				Set<NodeAndFlow> nodes = newGWClusters.get(gw);
					
				NodeAndFlow nf = new NodeAndFlow();
				nf.node = node;
				nf.flow = null;
				nodes.add(nf);
			}
		} else {
			for (Flow flow : network.getFlows()) {
				Node node = flow.getSource();
				Node gw = this.findTheNearestGW(node);
				Set<NodeAndFlow> nodes = newGWClusters.get(gw);
					
				NodeAndFlow nf = new NodeAndFlow();
				nf.node = node;
				nf.flow = flow;
				nodes.add(nf);
				
				node = flow.getDestination();
				nf = new NodeAndFlow();
				nf.node = node;
				nf.flow = flow;
				gw = this.findTheNearestGW(node);
				nodes = newGWClusters.get(gw);
				nodes.add(nf);		
			}
		}
		
		//check whether the two cluterings are the same.
		boolean same = this.sameClusterAsBefore(newGWClusters);
		this.gwClusters = newGWClusters;
		return same;
	}
	
	/**
	 * 
	 * return whether the new clustering is the same as old clustering.
	 * 
	 */
	private boolean sameClusterAsBefore(Map<Node, Set<NodeAndFlow>> newGWClusters) {
		if (newGWClusters.size() != this.gwClusters.size())
			return false;
		for (Map.Entry<Node, Set<NodeAndFlow>> entry : gwClusters.entrySet()) {
			Node oldGW = entry.getKey();
			Set<NodeAndFlow> oldNodes = entry.getValue();
			Set<NodeAndFlow> newNodes = newGWClusters.get(oldGW);
			if (newNodes == null) 
				return false;
			if (!oldNodes.equals(newNodes))
				return false;
		}
		return true;
	}

	/**
	 * add n gateways to the network.
	 * gateways are put in the center of local grid as possible.
	 * 
	 * @param perfectLink, if perfectLink is set, the links with a node GW have 1 PRR.
	 * 
	 * checked.
	 */
	public void addSectorCenterGateways(int nGateway, boolean perfectLink) {
		clearGateways();
		int widthDivisions = (int) Math.sqrt(nGateway);
		int heightDivisions = widthDivisions;
		if (widthDivisions * heightDivisions < nGateway) {
			widthDivisions++;
		}
		if (widthDivisions * heightDivisions < nGateway) {
			heightDivisions++;
		}
		//the width of a sector
		double xDivision = this.side / widthDivisions;
		//the height of a sector
		double yDivision = this.side / heightDivisions;
		
		for (int i=0; i<nGateway; i++) {
			int xId = i / heightDivisions;
			double yId;
			if (i == nGateway-1) {
				yId = ((i % heightDivisions) + heightDivisions-1)/2.0;
			} else {
				yId = i % heightDivisions;  
			}
			double x_co = (xId + 0.5) * xDivision;
			double y_co = (yId + 0.5) * yDivision;
			
			Node gateway = new Node(""+(i+1));
			network.addGateway(gateway);
			Coordinates co = new Coordinates(x_co, y_co);
			this.nodePositions.put(gateway, co);
		}
		
		this.setupGatewaysLinks(perfectLink);
		
	}
	
	public void addGatewaysSpectralPartitioning(boolean perfectLink) {
		clearGateways();
		Set<Node> cluster1 = this.spectralPartitionInto2();
		if (cluster1.size() != this.network.getNormalNodes().size() / 2)
			throw new IllegalStateException("the cluster size is wrong");
		double x1 = 0;
		double x2 = 0;
		double y1 = 0;
		double y2 = 0;
		int size1 = 0;
		int size2 = 0;
		for (Node node : this.network.getNormalNodes()) {
			Coordinates co = this.nodePositions.get(node);
			if (cluster1.contains(node)) {
				x1 += co.x;
				y1 += co.y;
				size1++;
			} else {
				x2 += co.x;
				y2 += co.y;
				size2++;
			}
		}
		Node gateway = new Node("1");
		network.addGateway(gateway);
		Coordinates co = new Coordinates(x1 / size1, y1 / size1);
//		System.err.printf("1: %.3f, %3f\n", co.x, co.y);
		this.nodePositions.put(gateway, co);
		gateway = new Node("2");
		network.addGateway(gateway);
		co = new Coordinates(x2 / size2, y2 / size2);
//		System.err.printf("2: %.3f, %3f\n", co.x, co.y);
		this.nodePositions.put(gateway, co);
		this.setupGatewaysLinks(perfectLink);
	}
	
	//checked.
	public void addGatewaysInTheCenter(int nGateway, boolean perfectLink) {
		clearGateways();
		
		double x = this.side / 2.0;
		double y = this.side / 2.0;
				
		for (int i=0; i<nGateway; i++) {
			Node gateway = new Node(""+(i+1));
			network.addGateway(gateway);
			Coordinates co = new Coordinates(x, y);
			this.nodePositions.put(gateway, co);
		}
		
		this.setupGatewaysLinks(perfectLink);
		
	}
	
	public Coordinates getNodeCoordinates(Node node) {
		return this.nodePositions.get(node);
	}
	
	private static class SortPair implements Comparable<SortPair> {
		public final double value;
		public final int index;
		
		public SortPair(double value, int index) {
			this.value = value;
			this.index = index;
		}

		@Override
		public int compareTo(SortPair o) {
			return Double.compare(value, o.value);
		}

		
	}
	
	private int[] sort(double[] values) {
		SortPair[] pairs = new SortPair[values.length];
		for (int i=0; i<values.length; i++) {
			SortPair item = new SortPair(values[i], i);
			pairs[i] = item;
		}
		Arrays.sort(pairs);
		int[] res = new int[values.length];
		for (int i=0; i<pairs.length; i++) {
			res[i] = pairs[i].index;
		}
		return res;
	}
	
	/**
	 * Partition the network into 2 parts with the method "spectral partitioning"
	 */
	public Set<Node> spectralPartitionInto2() {
		int numNodes = this.network.getNormalNodes().size();
		HashMap<Node, Integer> nodes2IDs = new HashMap<Node, Integer>();
		Node[] nodes = new Node[numNodes];
		double[][] laplacian = new double[numNodes][numNodes];
		for (int i=0; i<numNodes; i++) {
			for (int j=0; j<numNodes; j++) {
				laplacian[i][j] = 0;
			}
		}
		int id = 0;
		for (Map.Entry<Node, Coordinates> entry : this.nodePositions.entrySet()) {
			Node node = entry.getKey();
			Coordinates co = entry.getValue();
			if (node.getType() == Node.NORMAL) {
				int nodeId = id++;
				nodes2IDs.put(node, nodeId);
				nodes[nodeId] = node;
				//System.out.printf("%s\t%.3f\t%.3f\n", node.getId(), co.x, co.y);
			}
		}
		UndirectedSparseGraph<Node, Edge> graph = this.network.graph;
		for (id = 0; id < numNodes; id++) {
			Node node = nodes[id];
			int degree = 0;
			for (Node neighbor : graph.getNeighbors(node)) {
				if (neighbor.getType() == Node.NORMAL) {
					int neighborId = nodes2IDs.get(neighbor);
					laplacian[id][neighborId] = -1;
					laplacian[neighborId][id] = -1;
					degree++;
				}
			}
			laplacian[id][id] = degree;			
		}
		//print adjacency
//		for (int i=0; i<numNodes; i++) {
//			for (int j=0; j<numNodes; j++) {
//				if (i == j) {
//					System.out.print(0);
//				} else {
//					System.out.printf("%.0f", -laplacian[i][j]);
//				}
//				System.out.print('\t');
//			}
//			System.out.println();
//		}
		RealMatrix matrix = new Array2DRowRealMatrix(laplacian, false);
		EigenDecomposition decomp = new EigenDecompositionImpl(matrix, 0);
		double[] eigenValues = decomp.getRealEigenvalues();
		int[] indices = this.sort(eigenValues);
		RealVector eigVector = decomp.getEigenvector(indices[1]);
		double[] values = eigVector.getData();
		indices = this.sort(values);
		Set<Node> cluster1 = new HashSet<Node>();
		for (int i=0; i<numNodes/2; i++) {
			cluster1.add(nodes[indices[i]]);
		}
		return cluster1;
	}
	
	public boolean isPhysicalModelUsed() {
		return this.physicalModel;
	}
	
}
