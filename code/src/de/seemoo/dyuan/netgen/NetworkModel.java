package de.seemoo.dyuan.netgen;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.collections15.Transformer;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import de.seemoo.dyuan.netgen.structure.Edge;
import de.seemoo.dyuan.netgen.structure.Node;
import de.seemoo.dyuan.utils.Global;
import edu.uci.ics.jung.algorithms.cluster.BicomponentClusterer;
import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.visualization.BasicVisualizationServer;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;

/**
 * 
 * The network is a undirectional mesh network with 
 * a number of gateways and a number of normal nodes.
 * The normal nodes are sensor/actuator/relay nodes.
 * 
 * 
 * @author dyuan
 *
 */
public class NetworkModel {
	
	protected UndirectedSparseGraph<Node, Edge> graph;
	
	protected List<Node> gateways = new ArrayList<Node>();
	
	protected List<Node> normalNodes = new ArrayList<Node>();
	
	protected List<Flow> flows = new ArrayList<Flow>();	
	
	protected UndirectedSparseGraph<Node, Edge> copyGraph;
	
	protected boolean piggyback = false;
	
	/** First find n most-reliable vertex disjoint path from src to the GWs (must go to different GWs).
	 *  Then reset the graph that all vertices are selectable, find n most-reliable vertex disjoint paths from the GWs 
	 * to the dest (must come from different GWs). These are the upflow subpaths and downflow subpaths.
	 * 
	 * If cannot find n such upflow and downflow subpaths. clear all paths.*/
	public static final int ROUTING_DISJOINT_MOST_RELIABLE = 1;
	
	/** Find n shortest vertex-disjoint paths.*/
	public static final int ROUTING_DISJOINT_SHORTEST_PATH = 2;
	
	/** Find the vertex disjoint shortest paths with dinic algorithm. */	
	public static final int ROUTING_DISJOINT_SHORTEST_PATHS_WITH_DINIC = 3;
	
	public static final int ROUTING_DISJOINT_BHANDARI_MIN_SUM_LOSS_RATE = 4;
	public static final int ROUTING_DISJOINT_BHANDARI_MAX_PROD_REC_RATE = 5;
	public static final int ROUTING_DISJOINT_BHANDARI_MIN_ETX = 6;
	public static final int ROUTING_DISJOINT_BHANDARI_MIN_TOTAL_HOPS = 7;
	
	public static final int ROUTING_DISJOINT_PAIR_MOST_RELIABLE_LC = 8;
	
	public static final int ROUTING_DISJOINT_BF_MOST_RELIABLE = 9;
	
	public static final Map<Integer, String> ROUTING_TYPE_TO_NAME_TABLE;
	
	public static final Map<String, Integer> ROUTING_NAME_TO_TYPE_TABLE;
	
	static {
		ROUTING_TYPE_TO_NAME_TABLE = new HashMap<Integer, String>();		
		ROUTING_NAME_TO_TYPE_TABLE = new HashMap<String, Integer>();
		
		ROUTING_TYPE_TO_NAME_TABLE.put(ROUTING_DISJOINT_MOST_RELIABLE, "most-reliable");
		ROUTING_NAME_TO_TYPE_TABLE.put("most-reliable", ROUTING_DISJOINT_MOST_RELIABLE);
		
		ROUTING_TYPE_TO_NAME_TABLE.put(ROUTING_DISJOINT_SHORTEST_PATH, "shortest-path");
		ROUTING_NAME_TO_TYPE_TABLE.put("shortest-path", ROUTING_DISJOINT_SHORTEST_PATH);
		
		ROUTING_TYPE_TO_NAME_TABLE.put(ROUTING_DISJOINT_SHORTEST_PATHS_WITH_DINIC, "shortest-path-dinic");
		ROUTING_NAME_TO_TYPE_TABLE.put("shortest-path-dinic", ROUTING_DISJOINT_SHORTEST_PATHS_WITH_DINIC);
		
		ROUTING_TYPE_TO_NAME_TABLE.put(ROUTING_DISJOINT_BHANDARI_MIN_SUM_LOSS_RATE, "min-sum-loss-rate");
		ROUTING_NAME_TO_TYPE_TABLE.put("min-sum-loss-rate", ROUTING_DISJOINT_BHANDARI_MIN_SUM_LOSS_RATE);
		
		ROUTING_TYPE_TO_NAME_TABLE.put(ROUTING_DISJOINT_BHANDARI_MAX_PROD_REC_RATE, "max-prod-rec-rate");
		ROUTING_NAME_TO_TYPE_TABLE.put("max-prod-rec-rate", ROUTING_DISJOINT_BHANDARI_MAX_PROD_REC_RATE);
		
		ROUTING_TYPE_TO_NAME_TABLE.put(ROUTING_DISJOINT_BHANDARI_MIN_ETX, "min-etx");
		ROUTING_NAME_TO_TYPE_TABLE.put("min-etx", ROUTING_DISJOINT_BHANDARI_MIN_ETX);
		
		ROUTING_TYPE_TO_NAME_TABLE.put(ROUTING_DISJOINT_BHANDARI_MIN_TOTAL_HOPS, "min-total-hops");
		ROUTING_NAME_TO_TYPE_TABLE.put("min-total-hops", ROUTING_DISJOINT_BHANDARI_MIN_TOTAL_HOPS);
		
		ROUTING_TYPE_TO_NAME_TABLE.put(ROUTING_DISJOINT_PAIR_MOST_RELIABLE_LC, "pair-most-reliable-lc");
		ROUTING_NAME_TO_TYPE_TABLE.put("pair-most-reliable-lc", ROUTING_DISJOINT_PAIR_MOST_RELIABLE_LC);
		
		ROUTING_TYPE_TO_NAME_TABLE.put(ROUTING_DISJOINT_BF_MOST_RELIABLE, "most-reliable-bf");
		ROUTING_NAME_TO_TYPE_TABLE.put("most-reliable-bf", ROUTING_DISJOINT_BF_MOST_RELIABLE);
	}
	
	public static String getRoutingMethodName(int routingMethod) {
		String name = ROUTING_TYPE_TO_NAME_TABLE.get(routingMethod);
		if (name != null) {
			return name;
		}
		throw new IllegalArgumentException("routing method of type "+routingMethod+" unknown!");
	}
	
	public static int getRoutingMethodFromName(String methodName) {
		Integer type = ROUTING_NAME_TO_TYPE_TABLE.get(methodName);
		if (type != null) {
			return type;
		}
		throw new IllegalArgumentException("routing type for name "+methodName+" unknown!");
	}
	
	//node utilities, initial value of each node util is equal to 1.0
	protected Map<Node, Double> nodeUtils;
	
	
	public NetworkModel() {
		this.graph = new UndirectedSparseGraph<Node, Edge>();
	}
	
	
	public void clearGateways() {
		this.gateways.clear();
	}
	
	public void setPiggyback(boolean piggyback) {
		this.piggyback = piggyback;
	}
	
	public boolean isPiggyback() {
		return this.piggyback;
	}
	
	public Edge addEdge(Node node1, Node node2, double linkQuality) {
		Edge edge = new Edge(node1, node2, linkQuality);
		this.graph.addEdge(edge, node1, node2);
		return edge;
	}
	
	public void removeNeighbourLinks(Node node) {
		List<Edge> edges2Remove = new ArrayList<Edge>(this.graph.getIncidentEdges(node));
		for (Edge edge : edges2Remove) {
			this.graph.removeEdge(edge);
		}
	}
	
	public Edge addEdgeToCopyGraph(Node node1, Node node2, double linkQuality) {
		if (this.copyGraph.containsVertex(node1) && this.copyGraph.containsVertex(node2)) {
			Edge edge = new Edge(node1, node2, linkQuality);
			this.copyGraph.addEdge(edge, node1, node2);
			return edge;
		}
		return null;
	}
	
	public void addGateway(Node node) {
		node.setType(Node.GATEWAY);
		this.gateways.add(node);
		this.graph.addVertex(node);
		//System.out.printf("gw: name %s, id: %d\n", node.getId(), node.getIndex());
	}
	
	public void addNormalNode(Node node) {
		node.setType(Node.NORMAL);
		this.normalNodes.add(node);
		this.graph.addVertex(node);
		//System.out.printf("nm: name %s, id: %d\n", node.getId(), node.getIndex());
	}
	
	/**
	 * Visualization of the network.
	 * @param highlightEdges The edges to be highlighted.
	 * @return The visualization component.
	 */
	public BasicVisualizationServer<Node, Edge> getGraphicComponent() {
		Layout<Node, Edge> layout;
		
		final int width = 1900;
		final int height = 1100;
		
		if (this.netGen instanceof UniformGen) {
			layout = new StaticLayout<Node, Edge>(graph);
			((StaticLayout)layout).setInitializer(new Transformer<Node, Point2D>() {

				@Override
				public Point2D transform(Node node) {
					int widthOff = 50;
					int heightOff = 50;
					double xscale = width*0.9 / ((UniformGen)netGen).getSide();
					double yscale = height*0.9 / ((UniformGen)netGen).getSide();
					Coordinates co = ((UniformGen)netGen).getNodeCoordinates(node);
					return new Point((int)(co.x * xscale)+widthOff, (int)(co.y * yscale)+heightOff);
				}
			});
		} else if (this.netGen instanceof GridGen) {
			layout = new StaticLayout<Node, Edge>(graph);
			((StaticLayout)layout).setInitializer(new Transformer<Node, Point2D>() {

				@Override
				public Point2D transform(Node node) {
					int widthOff = 50;
					int heightOff = 50;
					int side = ((GridGen)netGen).getSide();
					double xscale = width / (double)side;
					double yscale = height / (double)side;
					Coordinates co = ((GridGen)netGen).getNodeCoordinates(node);
					return new Point((int)(co.x * xscale)+widthOff, (int)(co.y * yscale)+heightOff);
				}
			});
			
		} else {
			layout = new ISOMLayout<Node, Edge>(graph);
		}

		layout.setSize(new Dimension(width, height));
		BasicVisualizationServer<Node, Edge> server = new BasicVisualizationServer<Node, Edge>(layout);
		server.setPreferredSize(new Dimension(width+10, height+10));
		server.getRenderContext().setVertexLabelTransformer(new Transformer<Node, String>() {

			@Override
			public String transform(Node node) {
				return node.getId();
			}
		});
		Transformer<Node, Font> fontTransformer = new Transformer<Node, Font>() {

			@Override
			public Font transform(Node node) {
				return new Font("Arial", Font.PLAIN, 15);
			}
			
		};
		Transformer<Node, Shape> shapeTransformer = new Transformer<Node, Shape>() {

			@Override
			public Shape transform(Node node) {
				return new Ellipse2D.Float(-5f, -5f, 10.0f, 10.0f);
			}
			
		};
		server.getRenderContext().setVertexShapeTransformer(shapeTransformer);
		server.getRenderContext().setVertexFontTransformer(fontTransformer);
		server.getRenderer().getVertexLabelRenderer().setPosition(Position.AUTO);
		
		Transformer<Node, Paint> vertexPaint = new Transformer<Node, Paint>() {

			@Override
			public Paint transform(Node node) {
				if (node.getType() == Node.GATEWAY)
					return Color.RED;
				if (node.getType() == Node.NORMAL) {
//					if (node.isSourceOrDest())
//						return Color.MAGENTA;
					return Color.GREEN;
				}
				return Color.YELLOW;
			}
			
		};
		server.getRenderContext().setVertexFillPaintTransformer(vertexPaint);
		Transformer<Edge, Font> edgeFontTransformer = new Transformer<Edge, Font>() {

			@Override
			public Font transform(Edge edge) {
				return new Font("Arial", Font.PLAIN, 15);
			}
		};
		server.getRenderContext().setEdgeFontTransformer(edgeFontTransformer);
		server.getRenderContext().setEdgeLabelTransformer(new ToStringLabeller());
		
		Transformer<Edge, Paint> edgeColorer = new Transformer<Edge, Paint>() {
			Color[] all_colors = new Color[]{
					Color.blue, Color.cyan, Color.green, 
					Color.magenta, Color.orange, Color.red, Color.yellow, Color.darkGray
			};
			private Map<Subpath, Color> colors;
			{
				colors = new HashMap<Subpath, Color>();
				Random rand = Global.randomGen;
				for (Flow flow : flows) {
					if (flow.hasPath()) {
						List<Subpath> paths = flow.getUpflowPaths();
						int r, g, b;
						r = rand.nextInt(256);
						g = rand.nextInt(256);
						b = rand.nextInt(256);
						for (Subpath path : paths) {
							colors.put(path, new Color(r, g, b));
						}
					}
				}
			}
			@Override
			public Paint transform(Edge e) {
//				for (Subpath p : colors.keySet()) {
//					if (p.getEdges().contains(e)) {
//						return colors.get(p);
//					}
//				}
				return Color.black;
			}
		};
		
		server.getRenderContext().setEdgeDrawPaintTransformer(edgeColorer);
		return server;
	}
	
	public UndirectedSparseGraph<Node, Edge> getGraph() {
		return this.graph;
	}
	
	public UndirectedSparseGraph<Node, Edge> getCopyGraph() {
		return this.copyGraph;
	}
	
	
	public List<Node> getGateways() {
		return this.gateways;
	}
	
	public List<Node> getNormalNodes() {
		return this.normalNodes;
	}
	/**
	 * Note flow is directional
	 */
	public Flow newFlow(Node source, Node destination) {
		Flow flow = new Flow(this, source, destination);
		this.flows.add(flow);
		return flow;
	}
	
	public List<Flow> getFlows() {
		return this.flows;
	}
	
	
	/**
	 * 
	 * @return all non-empty flows.
	 */
	public List<Flow> getNonEmptyFlows() {
		List<Flow> nonEmptyFlows = new ArrayList<Flow>();
		for (Flow flow : this.getFlows()) {
			if (flow.hasPath()) {
				nonEmptyFlows.add(flow);
			}
		}
		return nonEmptyFlows;
	}
	
	public double computeMeanFlowReliability() {
		int count = 0;
		double r = 0;
		for (Flow flow : this.getFlows()) {
			if (flow.hasPath()) {
				count++;
				r += flow.getReliability2P();
			}
		}
		if (count != 0) {
			return r / count;
		}
		return 0;
	}
	
	public double computeMeanWeightedFlowReliability() {
		double weight = 0;
		double r = 0;
		for (Flow flow : this.getFlows()) {
			if (flow.hasPath()) {
				int period = flow.getPeriod();
				r += flow.getReliability2P() / period;
				weight += 1.0 / period;
			}
		}
		if (weight != 0) {
			return r / weight;
		}
		return 0;
	}
	
	public double computeMeanFlowHops() {
		int count = 0;
		int hops = 0;
		for (Flow flow : this.getFlows()) {
			if (flow.hasPath()) {
				count++;
				hops += flow.getAllPathHops();
			}
		}
		if (count != 0) {
			return (double)hops / count;
		}
		return 0;
	}
	
	
	public int getMinDeadlineLength() {
		int len = Integer.MAX_VALUE;
		for (Flow flow : this.getFlows()) {
			if (flow.hasPath()) {
				int l = flow.getMinDeadlineLength();
				if (l < len) {
					len = l;
				}
			}
		}
		return len;
	}
	
	public int getMaxDeadlineLength() {
		int len = 0;
		for (Flow flow : this.getFlows()) {
			if (flow.hasPath()) {
				int l = flow.getMinDeadlineLength();
				if (l > len) {
					len = l;
				}
			}
		}
		return len;
	}
	
	public double computeMeanWeightedFlowHops() {
		double weight = 0;
		double hops = 0;
		for (Flow flow : this.getFlows()) {
			if (flow.hasPath()) {
				int period = flow.getPeriod();
				hops += flow.getAllPathHops() / (double)period;
				weight += 1.0 / period;
			}
		}
		if (weight == 0) {
			return 0;
		}
		return hops / weight;
	}
	
//	/**
//	 * method to build the neighbouring table. Every node is associated with
//	 * a set of incident edges.
//	 */
//	public void buildNeighbourTable() {
//		this.neighbourTable = new HashMap<Node, Set<Edge>>();
//		for (Gateway gw : this.gateways) {
//			buildNeighbourTableForNode(gw);
//		}
//		for (NormalNode node : this.normalNodes) {
//			buildNeighbourTableForNode(node);
//		}
//	}
//	/**
//	 * 
//	 * @return the neighbouring table.
//	 */
//	public Map<Node, Set<Edge>> getNeighbourTable() {
//		return this.neighbourTable;
//	}
	
//	/**
//	 * Check whether an edge is incident on a node?
//	 */
//	public boolean isEdgeIncidentOnNode(Node node, Edge edge) {
//		Set<Edge> set = this.neighbourTable.get(node);
//		if (set == null)
//			return false;
//		return set.contains(edge);
//	}

	public static final int MAX_DISCARD = 10000;
	
//	public static final double min_flow_utilization = 0.001;
	
	private static boolean isValidUtilization(double util, Flow flow) {
		return util <= flow.getMaximumUtilization();
	}
	
	public static int MAX_PERIOD = 10000;
	
	public static int[] possible_periods = Global.findFactorsOf(MAX_PERIOD);
	
	public static int[] harmonious_periods = {2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192};
	
	/**
	 * For each flow, generate a factor f in (0, 1). The utilization of this flow is
	 * set to f*u_max, then the period is selected accordingly.
	 * @return true. if no possible assignment after max number of tries. 
	 */
//	public boolean utilScaling(double maxUtil) {
//		List<Flow> nonEmptyFlows = this.getNonEmptyFlows();
//		int discard = 0;
//		OUTER:
//		while (true) {
//			for (int i=0; i<nonEmptyFlows.size(); i++) {
//				Flow flow = nonEmptyFlows.get(i);
//				double factor = Global.randomGen.nextDouble();
//				double flowUtil = factor * flow.getMaximumUtilization();
//				
//				double periodCeil = flow.getAllPathHops() / flowUtil;				
//				int period = findProperPeriod(periodCeil);
//				if (period != -1) {
//					flow.setPeriod(period);
//				} else {	
//					discard++;
//					if (discard == MAX_DISCARD) {
//						return true;
//					}
//					continue OUTER;
//				}
//			}
//			double sumUtil = 0;
//			for (Flow flow : nonEmptyFlows) {
//				sumUtil += flow.getAllPathHops() / (double)flow.getPeriod();
//			}
//			if (sumUtil > maxUtil)
//				continue OUTER;
//			return false;
//		}
//		
//	}
	
	/**
	 * Setting periods according to the uunifast algorithm by Bini and adapted by Davis.
	 * The algorithm generates a taskset with uniform distributed utilizations. From the utilizations, 
	 * proper periods are picked. 
	 * C.f. Enrico Bini and Giorgio C. Buttazzo. Measuring the performance of schedulability
		tests. Real-Time Systems, 30(1-2):129{154, 2005.
	 * Robert I. Davis and Alan Burns. Improved priority assignment for global
		fixed priority pre-emptive scheduling in multiprocessor real-time systems. Real-Time Systems, 47(1):1{40, 2011.
		@param totalUtil the total utilization of all tasks.
		@return true. if no possible assignment after max number of tries. 
	 */
	public boolean uUniFastSettingPeriods(double totalUtil, boolean implicit, boolean harmonious) {
		List<Flow> nonEmptyFlows = this.getNonEmptyFlows();
		
		//the check works with both piggyback or not.
		double util = 0;
		for (Flow flow : nonEmptyFlows) {
			util += flow.getMaximumUtilization();
		}
		if (util < totalUtil) {
			totalUtil = util;
		}
		
		int discard = 0;
		int nFlows = nonEmptyFlows.size();
		if (nFlows == 0)
			return false;
		double[] utils = new double[nFlows]; 
		OUTER:
		while (true) {
			double sumUtil = totalUtil;
			for (int i=0; i<nFlows; i++) {
				double nextSumUtil;
				if (i == nFlows-1)
					nextSumUtil = 0;
				else
					nextSumUtil = sumUtil * Math.pow(Global.randomGen.nextDouble(), 1.0/(nFlows-i-1));
				utils[i] = sumUtil - nextSumUtil;
				if (!isValidUtilization(utils[i], nonEmptyFlows.get(i))) {
					discard++;
					if (discard == MAX_DISCARD) {
						return true;
					}
					continue OUTER;
				}
				sumUtil = nextSumUtil;
			}
			
			for (int i=0; i<nFlows; i++) {
				Flow flow = nonEmptyFlows.get(i);
				
				double periodLB = flow.getAllPathHops() / utils[i];
				int minPeriod = flow.getMinDeadlineLength();
				if (!implicit) {
					minPeriod++;
				}
				int period = findProperPeriod(periodLB, minPeriod, harmonious);
				if (period != -1) {
					flow.setPeriod(period);
				} else {
					discard++;
					if (discard == MAX_DISCARD) {
						return true;
					}
					continue OUTER;
				}
			}
			if (this.computeHyperiod(false).compareTo(BigInteger.valueOf(MAX_PERIOD)) < 1) {
				return false;
			} else {
				discard++;
				if (discard == MAX_DISCARD) {
					return true;
				}
			}
		}
	}
	
	/**
	 * @return a value that is at least so big as period, which is a proper period.
	 * If no period is found, return -1.
	 */
	private int findProperPeriod(double period, int minPeriod, boolean harmonious) {
		if (harmonious) {
			for (int i=0; i<harmonious_periods.length; i++) {
				if (harmonious_periods[i] >= period && harmonious_periods[i] >= minPeriod) {
					return harmonious_periods[i];
				}
			}
		} else {
			for (int i=0; i<possible_periods.length; i++) {
				if (possible_periods[i] >= period && possible_periods[i] >= minPeriod) {
					return possible_periods[i];
				}	
			}
		}
		
		return -1;
	}

	public void implicitDeadlines() {
		for (Flow flow : this.flows) {
			flow.implicitDeadline();
		}
	}
	
	/**
	 * random deadlines.
	 */
	public void randomDeadlines() {
		for (Flow flow : this.getNonEmptyFlows()) {
			flow.randomDeadline();
		}
	}

	public double getTotalUtilization() {
		double sum = 0;
		for (Flow flow : this.getNonEmptyFlows()) {
			sum += flow.getAllPathHops() / (double)flow.getPeriod();
		}
		return sum;
	}
	
	private NetworkGenerator netGen;
	
	public void setNetworkGenerator(NetworkGenerator gen) {
		this.netGen = gen;
	}
	
	/**
	 * allFlows: true, compute for each flow, irrespective whether it has paths.
	 * 			 false, compute for the flows that have upflow and downflow paths.
	 * 
	 * compute the hyper period of all flows with at least one path.
	 * The hyper period is the lcm of all flow periods.
	 */
	public BigInteger computeHyperiod(boolean allFlows) {
		BigInteger lcm = BigInteger.ZERO;
		for (Flow flow : this.getFlows()) {
			if (allFlows || flow.hasPath()) {
				if (lcm.compareTo(BigInteger.ZERO) == 0) {
					lcm = BigInteger.valueOf(flow.getPeriod());
				} else {
					lcm = Global.lcm(lcm, BigInteger.valueOf(flow.getPeriod()));
				}
			}			
		}
		return lcm;
	}
	
	/**
	 * Remove all flows
	 */
	public void clearFlows() {
		this.flows.clear();
	}
	
	
	public double getDegreesStandardDeviation() {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for (Node node : graph.getVertices()) {
			int neighbours = graph.getNeighborCount(node);
			stats.addValue(neighbours);
		}
		return stats.getStandardDeviation();
	}


	public int getMaxFlowLength() {
		int len = 0;
		for (Flow flow : this.getNonEmptyFlows()) {
			int v = flow.getMinDeadlineLength();
			if (v > len) {
				len = v;
			}
		}
		return len;
	}
	
	/**
	 * Non-empty flows weighted by the reciprocal of period. 
	 * 
	 */
	public double getReliability() {
		double weight = 0.0;
		double sumReliability = 0.0;
		for (Flow flow : this.getNonEmptyFlows()) {
			weight += 1.0/flow.getPeriod();
			sumReliability += flow.getReliability2P()/flow.getPeriod();
		}
		return sumReliability / weight;
	}
	
	/**
	 *
	 * @return whether the whole network excluding GWs are 2-vertex connected.
	 */
	public boolean check2VertexConnected() {
		BicomponentClusterer<Node, Edge> clusterer = new BicomponentClusterer<Node, Edge>();
		Set<Set<Node>> clusters = clusterer.transform(this.graph);
		if (clusters != null && clusters.size() == 1)
			return true;
		return false;
	}
	
	/**
	 * 
	 * @return the average degree count of undirected path.
	 */
	public double getMeanDegree() {
		return ((double)this.graph.getEdges().size()) * 2 / this.graph.getVertexCount();
	}
	
	/**
	 * 
	 * @return a copied graph of the original. prepare to be used by routing.
	 */
	public UndirectedSparseGraph<Node, Edge> copyGraph() {
		UndirectedSparseGraph<Node, Edge> copy = new UndirectedSparseGraph<Node, Edge>();
		for (Node node : this.graph.getVertices()) {
			copy.addVertex(node);
		}
		for (Edge edge : this.graph.getEdges()) {
			copy.addEdge(edge, edge.getBiggerNode(), edge.getSmallerNode());
		}
		return copy;
	}
	
	private double maxNodeUtil = 1.0;
	
	public double getMaxNodeUtil() {
		return this.maxNodeUtil;
	}
	
	public void setMaxNodeUtil(double util) {
		this.maxNodeUtil = util;
	}
	
	
	
	/**
	 * Every node is first initialized to 1.0.
	 * then we minus K * util from each source and destination node.
	 * 
	 * @return false. if initialization failed.
	 */
	private boolean initNodeUtils(int nPaths) {
		this.nodeUtils = new HashMap<Node, Double>();
		for (Node node : this.graph.getVertices()) {
			if (node.isGateway()) {
				this.nodeUtils.put(node, 1.0);
			} else {
				this.nodeUtils.put(node, maxNodeUtil);
			}
			
		}
		for (Flow flow : this.flows) {
			int period = flow.getPeriod();
			if (!this.piggyback && period != 0) {
				double util = 1.0/period;
				Node node = flow.getSource();
				double val = this.nodeUtils.get(node);
				val -= util * nPaths;
				if (val < 0.0) {
					return false;
				}
				this.nodeUtils.put(node, val);
				
				node = flow.getDestination();
				val = this.nodeUtils.get(node);
				val -= util * nPaths;
				if (val < 0.0) {
					return false;
				}
				this.nodeUtils.put(node, val);
			}
		}
		return true;
	}
	
	public static int LOW_PERIOD_FIRST = 0;
	
	public static int HIGH_PERIOD_FIRST = 1;
	
	public static int RANDOM_ORDER = 2;
	
	private int flowRoutingSequence = RANDOM_ORDER;
	
	public void setFlowRoutingSequence(int seq) {
		this.flowRoutingSequence = seq;
	}
	
	public int getFlowRoutingSequence() {
		return this.flowRoutingSequence;
	}
	
	public static final int FEASIBLE = 0;
	
	public static final int TOTAL_UTIL_OVER_CHANNEL = 1;
	
	public static final int NOT_ENOUGH_GWS = 2;
	
	public static final int DISJOINT_PATH_INFEASIBLE = 3;
	
	public static final int NOT_CONNECTED = 4;
	
	public static final int DEADLINE_INFEASIBLE = 5;
	/**
	 * We check whether GWs count is enough.
	 * whether it is possibly feasible.
	 * whether the total utilization is enough.
	 * @param numChannels
	 */
	public int checkFeasibility(int nPaths, int nGWs, int numChannels) {
		if (this.flows != null) {
			double gwUtil = 0;
			for (Flow flow : this.flows) {
				gwUtil += 1.0/flow.getPeriod()*nPaths*2;
			}
			if (gwUtil > nGWs) {
				return NOT_ENOUGH_GWS;
			}
			
			for (Flow flow : this.flows) {
				int deadlineLB = flow.getDeadlineLowerBound(); 
				if (deadlineLB == -1) {
					return NOT_CONNECTED;
				}
				if (deadlineLB > flow.getDeadline()) {
					return DEADLINE_INFEASIBLE;
				}
				flow.createVertexDisjointRoutingNLCPart(nPaths, NetworkModel.ROUTING_DISJOINT_BHANDARI_MIN_TOTAL_HOPS, 
						true, false, false);
				if (flow.getUpflowPaths().size() != nPaths || flow.getUpflowPaths().size() != nPaths) {
					return DISJOINT_PATH_INFEASIBLE;
				} 				
			}
			if (this.getTotalUtilization() > numChannels) {
				return TOTAL_UTIL_OVER_CHANNEL;
			}
			
			
		}
		return FEASIBLE;
	}
	
	public void doVertexDisjointRouting(int nPaths, int routingMethod) {
		if (this.flows != null) {
			//long time = System.currentTimeMillis();
			//before starting routing, every node has utilization of 1
			if (this.maxNodeUtil != 0.0) {
				if (!initNodeUtils(nPaths)) {
					System.out.println("routing cannot be successful because not enough util at source or destination node!");
					return;
				}
			}
			
			//sort flows according the ascending of the period.
			//flows are sorted with the increase of period.
			//for the same period, flows with short deadlines are prioritied.
			List<Flow> sortedFlows = new ArrayList<Flow>(this.flows);
			
			if (this.getFlowRoutingSequence() != RANDOM_ORDER) {
				Collections.sort(sortedFlows, new Comparator<Flow>() {

					@Override
					public int compare(Flow f1, Flow f2) {
						if (flowRoutingSequence == LOW_PERIOD_FIRST) {
							int v = f1.getPeriod() - f2.getPeriod();
							if (v != 0) {
								return v;
							}
							return f1.getDeadline() - f2.getDeadline();		
						} else {
							int v = f2.getPeriod() - f1.getPeriod();
							if (v != 0) {
								return v;
							}
							return f2.getDeadline() - f1.getDeadline();
						}
					}
					
				});
			}
			for (Flow flow : sortedFlows) {
				//System.out.println("flow "+flow.getSource()+"->"+flow.getDestination());
				flow.createVertexDisjointRouting(nPaths, routingMethod);
			}
			//System.err.println(this.flows.size()+" flows "+ (System.currentTimeMillis() - time ) + "ms");
		}
		
	}
	
	/**
	 * 
	 * @return the minimum number of channels needed for non-piggyback scheduling.
	 */
	public int getMinChannelsForNonpiggybackScheduling() {
		return (int) Math.ceil(this.getTotalUtilization());
	}
	
	public Map<Node, Double> getNodeUtils() {
		return this.nodeUtils;
	}

}
