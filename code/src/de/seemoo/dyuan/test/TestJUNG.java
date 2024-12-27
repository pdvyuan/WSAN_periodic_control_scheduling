package de.seemoo.dyuan.test;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Paint;

import javax.swing.JFrame;

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.visualization.BasicVisualizationServer;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;

public class TestJUNG {
	
	public static void main(String[] args)  {
		UndirectedSparseGraph<Integer, Double> graph = new UndirectedSparseGraph<Integer, Double>();
		graph.addVertex(1);
		graph.addVertex(2);
		graph.addVertex(3);
		graph.addEdge(1.0, 1, 2);
		graph.addEdge(0.8, 2, 4);
		Layout<Integer, Double> layout = new CircleLayout<Integer, Double>(graph);
		layout.setSize(new Dimension(600, 600));
		BasicVisualizationServer<Integer, Double> server = new BasicVisualizationServer<Integer, Double>(layout);
		server.setPreferredSize(new Dimension(650,650));
		server.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
		Transformer<Integer, Font> fontTransformer = new Transformer<Integer, Font>() {

			@Override
			public Font transform(Integer nodeId) {
				return new Font("Arial", Font.BOLD, 20);
			}
			
		};
		server.getRenderContext().setVertexFontTransformer(fontTransformer);
		
		Transformer<Integer, Paint> vertexPaint = new Transformer<Integer, Paint>() {

			@Override
			public Paint transform(Integer arg0) {
				return Color.GREEN;
			}
			
		};
		server.getRenderContext().setVertexFillPaintTransformer(vertexPaint);
		
		server.getRenderer().getVertexLabelRenderer().setPosition(Position.CNTR);
		
		
		JFrame frame = new JFrame("Simple Graph View");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(server);
		frame.pack();
		frame.setVisible(true);
	}

}
