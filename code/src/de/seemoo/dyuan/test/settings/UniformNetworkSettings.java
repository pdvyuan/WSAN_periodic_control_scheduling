package de.seemoo.dyuan.test.settings;

import javax.swing.JFrame;

import de.seemoo.dyuan.netgen.NetworkModel;
import de.seemoo.dyuan.netgen.UniformGen;
import de.seemoo.dyuan.utils.Global;


/**
 * Small network 100 nodes.
 * Large network 1000 nodes.
 * 
 * Low connectivity 3
 * High connectivity 10
 * 
 * results: run for 10000 times
 * 100 nodes, commRange 96.4, node degree 10.0023
 * 100 nodes, commRange 50.8, node degree 3.0097
 * 
 * 
 * @author dyuan
 *
 */

public class UniformNetworkSettings {
	
	private static int total_times;
	
	private static int numberNodes = 100;
	private static double commRange = 50.8;
	private static double side = 1800;
	
	private static double totalDegrees;

	
	
	public static void main(String[] args) throws Exception {
		Global.randomSeed();
		totalDegrees = 0;
		total_times = 0;

		for (int i=0; i<1000; i++) {
			System.out.println(i);
			generateNetwork();
		}
		System.err.printf("%.4f / %d = %.4f\n", totalDegrees, total_times, totalDegrees/total_times);
		
	}

	public static void generateNetwork() throws Exception {
		
		final NetworkModel network = new NetworkModel();
		
		final UniformGen gen = new UniformGen(network, 50, 100);
		//final UniformGen gen = new UniformGen(network, 50, 100);
		gen.addSensors(numberNodes, side);
		gen.buildEdgeAndRandomLQ();
		//GWs are put to the grid center
		gen.addSectorCenterGateways(2, false);
		totalDegrees += network.getMeanDegree();
		total_times++;
//		JFrame frame = new JFrame("Test Network Model");
//		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		frame.getContentPane().add(network.getGraphicComponent());
//		frame.pack();
//		frame.setVisible(true);
		
	}
}
