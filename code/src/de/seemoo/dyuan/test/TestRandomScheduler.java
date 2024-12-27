package de.seemoo.dyuan.test;

import java.awt.BorderLayout;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;

import de.seemoo.dyuan.eval.Settings;
import de.seemoo.dyuan.netgen.Flow;
import de.seemoo.dyuan.netgen.NetworkModel;
import de.seemoo.dyuan.netgen.UniformGen;
import de.seemoo.dyuan.netgen.structure.Edge;
import de.seemoo.dyuan.netgen.structure.Node;
import de.seemoo.dyuan.scheduler.ScheduleResult;
import de.seemoo.dyuan.scheduler.SchedulerBase;
import de.seemoo.dyuan.scheduler.Schedulers;
import de.seemoo.dyuan.utils.Global;
import edu.uci.ics.jung.visualization.BasicVisualizationServer;

public class TestRandomScheduler {
	
	private static int total_times;
	
	private static int numberNodes = 100;
	//commRange = 80, deg = 3.7240
	//side = 1600, deg = 3.5526
	private static double commRange = Settings.DISK_SMALL_NET_HIGH_CONN.commRange;
	//private static double side = 1600;
	private static double side = 1200;
	
	private static double totalDegrees = 0;
	
	private static int schedulable_times = 0;
	private static int schedulable_times2 = 0;
	
	private static double piggybackPercent = 0;
	
	private static int maxPiggyback = 0;
	
	public static void main(String[] args) throws Exception {
		Random rnd = new Random();
		int seed = rnd.nextInt();
		System.out.println("seed = "+seed);
		Global.randomSeed();
		total_times = 0;
		schedulable_times = 0;
		schedulable_times2 = 0;

		for (int i=0; i<100; i++) {
			System.out.println(i);
			runOnce(false);
		}
		System.err.printf("%.4f / %d = %.4f\n", totalDegrees, total_times, totalDegrees/total_times);
		System.err.printf("feasible percentage = %.3f%%\n", schedulable_times *100 / (double)total_times);
		System.err.printf("feasible percentage2 = %.3f%%\n", schedulable_times2 *100 / (double)total_times);
		System.err.printf("piggyback percent = %.3f%%\n", piggybackPercent / (double)schedulable_times2);
		System.err.printf("max piggyback on sender = %d\n", maxPiggyback);
	}

	public static void runOnce(boolean showGUI) throws Exception {
		
		final NetworkModel network = new NetworkModel();
		
		final UniformGen gen = new UniformGen(network, 50, 100);
		//final UniformGen gen = new UniformGen(network, 50, 100);
		gen.addSensors(numberNodes, side);
		gen.buildEdgeAndRandomLQ();
		double endFraction = Global.randomGen.nextDouble();
		gen.randomFlows(endFraction);
		
		List<Flow> flows = network.getFlows();
		System.out.println("flows "+flows.size());
		for	(Flow flow : flows) {
			int minIndex = 5;
			int maxIndex = NetworkModel.possible_periods.length;
			int index = Global.randomGen.nextInt(maxIndex - minIndex) + minIndex;
			int period = NetworkModel.possible_periods[index];
			flow.setPeriod(period);
			flow.implicitDeadline();
		}
		
		int numberGWs = 2;
		gen.addSectorCenterGateways(numberGWs, false);
		
		int numPaths = 2;
		int expFlows = flows.size();
		long time = System.currentTimeMillis();
		network.doVertexDisjointRouting(numPaths, NetworkModel.ROUTING_DISJOINT_MOST_RELIABLE);
		time = System.currentTimeMillis() - time;
		System.out.println(time+" ms");
		int actFlowsWithRouting = 0;
		for (Flow flow : flows) {
			if (flow.hasPath()) {
				actFlowsWithRouting++;
			}
		}
		System.err.printf("expected paths = %d, actual paths with routing = %d\n", expFlows, actFlowsWithRouting);
		
		if (network.getNonEmptyFlows().size() == 0)
			return;
			
		//schedule(network);
		totalDegrees += network.getMeanDegree();
		total_times++;
		
		if (showGUI) {
			JFrame frame = new JFrame("Test Network Model");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			JPanel panel = new JPanel(new BorderLayout());
			frame.getContentPane().add(panel);
			final BasicVisualizationServer<Node, Edge> netComp = network.getGraphicComponent(); 
			panel.add(netComp, BorderLayout.CENTER);
			frame.pack();
			frame.setVisible(true);
		}
		
		
//		GraphMLWriter<Node, Edge> writer = new GraphMLWriter<Node, Edge>();
//		writer.addEdgeData("lq", null, null, new Transformer<Edge, String>() {
//			
//			@Override
//			public String transform(Edge edge) {
//				return Math.round(edge.getLinkQuality()*100)+"%";
//			}
//		});
//		
//		writer.save(network.getGraph(), new OutputStreamWriter(System.out));
	}

	private static void schedule(NetworkModel network) throws NoSuchMethodException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException {
		
		Class clazz = Schedulers.best_scheduler_class;
		Constructor constructor = clazz.getConstructor(NetworkModel.class, boolean.class, boolean.class, int.class);
		int channels = 8;
		SchedulerBase scheduler = (SchedulerBase) constructor.newInstance(network, false, false, channels);
		ScheduleResult ret = scheduler.schedule();
		ret.printSimple();
		System.out.println("start checking "+scheduler.getName());
		ret.validate();
		System.out.println("check "+scheduler.getName()+" ok!");
		System.out.println("--------------------------------------------");
		if (ret.getStatus() == ScheduleResult.FEASIBLE) {
			schedulable_times++;
		} 				
		scheduler = (SchedulerBase) constructor.newInstance(network,  true, false, channels);
		ret = scheduler.schedule();
		ret.printSimple();
		System.out.println("start checking "+scheduler.getName());
		ret.validate();
		System.out.println("check "+scheduler.getName()+" ok!");
		System.out.println("--------------------------------------------");
		if (ret.getStatus() == ScheduleResult.FEASIBLE) {
			schedulable_times2++;
			piggybackPercent += scheduler.getPiggybacks() * 100 / (double)scheduler.getTransmittedPackets();
			if (maxPiggyback < scheduler.getMaxPiggybackOnSender()) {
				maxPiggyback = scheduler.getMaxPiggybackOnSender();
			}
		} 	
	}
}
