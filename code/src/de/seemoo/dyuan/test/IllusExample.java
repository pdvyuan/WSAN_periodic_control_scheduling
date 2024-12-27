package de.seemoo.dyuan.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import de.seemoo.dyuan.netgen.Flow;
import de.seemoo.dyuan.netgen.NetworkGenerator;
import de.seemoo.dyuan.netgen.NetworkModel;
import de.seemoo.dyuan.netgen.Subpath;
import de.seemoo.dyuan.netgen.structure.Edge;
import de.seemoo.dyuan.netgen.structure.Node;
import de.seemoo.dyuan.scheduler.HeuristicScheduler;
import de.seemoo.dyuan.scheduler.ScheduleResult;
import de.seemoo.dyuan.scheduler.SchedulerBase;
import de.seemoo.dyuan.scheduler.Schedulers;
import de.seemoo.dyuan.scheduler.dynamic_priority.lst.LSTScheduler;
import de.seemoo.dyuan.scheduler.fixed_priority.RMScheduler;

public class IllusExample {
	
	private NetworkModel model;
	
	private Node g1, g2;
	
	private Node s1, s2, a1, a2;
	
	private Node a, b, c, d, e, f, g, h, i;
	
	private Edge s1a, ab, bc, cs2;
	private Edge dg1, g1e, eg2, g2f;
	private Edge a2g, gh, hi, ia1;
	private Edge s1d, da2, ag1, g1g, be, eh, cg2, g2i, s2f, fa1;
	
	private Flow f1, f2;
	
	private final int numChannels = 2;
	
	public IllusExample() throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		initNetwork();
		setTimings();
		schedule();
	}
	
	public static void main(String[] args) throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		IllusExample exp = new IllusExample();
	}
	
	private void schedule() throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Class[] classes = Schedulers.schedulers_classes;
		for (Class clazz : classes) {
			Constructor constructor = clazz.getConstructor(NetworkModel.class, boolean.class, boolean.class, int.class);
			HeuristicScheduler scheduler = (HeuristicScheduler) constructor.newInstance(model, false, false, numChannels);
			//System.out.println(scheduler.getName());
			long diff = System.currentTimeMillis();
			ScheduleResult result = scheduler.schedule();
			diff = System.currentTimeMillis() - diff;
			//result.print();
			if (result.getStatus() == ScheduleResult.FEASIBLE) {
				//System.out.printf("time %d max-buf %d buf-time-prod %d\n", diff, Node.getMaxBuffer(), scheduler.getBufferTimeProduct());
				result.printRTGrid();
				//result.validate();
				System.out.println();
				System.out.printf("scheduled = %d, hyper-period transmissions = %d, repetitive transmissions = %d\n",
						result.getScheduledTransmissionCount(), result.getHyperperiodTransmissionCount(), result.getReptitiveTransmissionCount());
			} else {
				System.out.println("infeasible");
			}
			System.out.println("----------------");
		}		
	}
	
	public void setTimings() {
		f1.setPeriod(10);
		f1.setDeadline(10);
		f2.setPeriod(20);
		f2.setDeadline(9);
	}
	
	private void initNetwork() {
		model = new NetworkModel();
		Node g1 = new Node("g1");
		Node g2 = new Node("g2");
		model.addGateway(g1);
		model.addGateway(g2);
		
		Node s1 = new Node("s1");
		Node s2 = new Node("s2");
		Node a1 = new Node("a1");
		Node a2 = new Node("a2");
		
		Node a = new Node("a");
		Node b = new Node("b");
		Node c = new Node("c");
		Node d = new Node("d");
		Node e = new Node("e");
		Node f = new Node("f");
		Node g = new Node("g");
		Node h = new Node("h");
		Node i = new Node("i");
		
		model.addNormalNode(s1);
		model.addNormalNode(s2);
		model.addNormalNode(a1);
		model.addNormalNode(a2);
		model.addNormalNode(a);
		model.addNormalNode(b);
		model.addNormalNode(c);
		model.addNormalNode(d);
		model.addNormalNode(e);
		model.addNormalNode(f);
		model.addNormalNode(g);
		model.addNormalNode(h);
		model.addNormalNode(i);
		
		s1a = model.addEdge(s1, a, 1.0);
		ab = model.addEdge(a, b, 1.0);
		bc = model.addEdge(b, c, 1.0);
		cs2 = model.addEdge(c, s2, 1.0);
		dg1 = model.addEdge(d, g1, 1.0);
		g1e = model.addEdge(g1, e, 1.0);
		eg2 = model.addEdge(e, g2, 1.0);
		g2f = model.addEdge(g2, f, 1.0);
		a2g = model.addEdge(a2, g, 1.0);
		gh = model.addEdge(g, h, 1.0);
		hi = model.addEdge(h, i, 1.0);
		ia1 = model.addEdge(i, a1, 1.0);
		
		s1d = model.addEdge(s1, d, 1.0);
		da2 = model.addEdge(d, a2, 1.0);
		ag1 = model.addEdge(a, g1, 1.0);
		g1g = model.addEdge(g1, g, 1.0);
		be = model.addEdge(b, e, 1.0);
		eh = model.addEdge(e, h, 1.0);
		cg2 = model.addEdge(c, g2, 1.0);
		g2i = model.addEdge(g2, i, 1.0);
		s2f = model.addEdge(s2, f, 1.0);
		fa1 = model.addEdge(f, a1, 1.0);
		
		f1 = model.newFlow(s1, a1);
		List<Edge> edges = new ArrayList<Edge>();
		edges.add(s1d);
		edges.add(dg1);
		Subpath subpath = new Subpath(edges, s1);
		f1.addSubpath(subpath, true);
		
		edges = new ArrayList<Edge>();
		edges.add(s1a);
		edges.add(ab);
		edges.add(bc);
		edges.add(cg2);
		subpath = new Subpath(edges, s1);
		f1.addSubpath(subpath, true);
	
		edges = new ArrayList<Edge>();
		edges.add(g1g);
		edges.add(gh);
		edges.add(hi);
		edges.add(ia1);
		subpath = new Subpath(edges, g1);
		f1.addSubpath(subpath, false);
		
		edges = new ArrayList<Edge>();
		edges.add(g2f);
		edges.add(fa1);
		subpath = new Subpath(edges, g2);
		f1.addSubpath(subpath, false);
		
		f2 = model.newFlow(s2, a2);
		edges = new ArrayList<Edge>();
		edges.add(cs2);
		edges.add(cg2);
		subpath = new Subpath(edges, s2);
		f2.addSubpath(subpath, true);
		
		edges = new ArrayList<Edge>();
		edges.add(g1g);
		edges.add(a2g);
		subpath = new Subpath(edges, g1);
		f2.addSubpath(subpath, false);
	}

}
