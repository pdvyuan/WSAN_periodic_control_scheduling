package de.seemoo.dyuan.eval;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

import de.seemoo.dyuan.scheduler.Schedulers;

public class ConvertSchedulerComparisonResult {
	
	public static class SchedulerContent {
		public int schedulable;
	
		public int time;
		
		public int maxBuffer;
		
		public int bufferTimeProduct;
	
		public int hyperPeriod;
		
		public void reset() {
			this.time = 0;
			this.maxBuffer = 0;
			this.bufferTimeProduct = 0;
			this.hyperPeriod = 0;
		}
	}
	
	private int topologyId;
	
	private double endFraction;
	
	private int flows;
	
	private double expectedUtilization;
	
	private double actualUtilization;
	
	private SchedulerContent[] contents;
	
	private String[] schedulerNames;
	
	private int channels;
	
	private BufferedReader breader;
	
	private int id = -1;
	
	public void convert(String inputFile) throws IOException {
		FileReader freader = new FileReader(inputFile);
		breader = new BufferedReader(freader);
		String line;
		this.schedulerNames = new String[Schedulers.schedulers_classes.length];
		this.contents = new SchedulerContent[Schedulers.schedulers_classes.length];
		for (int i=0; i<this.schedulerNames.length; i++) {
			String name = Schedulers.schedulers_classes[i].getSimpleName();
			schedulerNames[i] = name.substring(0, name.length()-9);
			this.contents[i] = new SchedulerContent();
		}
		
		System.out.print("top\tid\tflows\tu\tau\tch");
		for (String name : schedulerNames) {
			System.out.print("\t"+name);
			System.out.print("\t"+name+"t");
			System.out.print("\t"+name+"m");
			System.out.print("\t"+name+"p");
			System.out.print("\t"+name+"h");
		}
		System.out.println();
		while ((line = breader.readLine()) != null) {
			processLine(line);
		}
		freader.close();
	}
	

	private void processLine(String line) throws IOException {
		if (line.startsWith("topo")) {
			this.topologyId = Integer.parseInt(line.substring(5));
		} else if (line.startsWith("e=")) {
			StringTokenizer tokenizer = new StringTokenizer(line, ", ");
			String token = tokenizer.nextToken();
			this.endFraction = Double.parseDouble(token.substring(2));
			token = tokenizer.nextToken();
			this.flows = Integer.parseInt(token.substring(6));
		} else if (line.startsWith("u=")) {
			StringTokenizer tokenizer = new StringTokenizer(line, ", ");
			String token = tokenizer.nextToken();
			this.expectedUtilization = Double.parseDouble(token.substring(2));
			token = tokenizer.nextToken();
			this.actualUtilization = Double.parseDouble(token.substring(3));
			id++;
		} else if (line.startsWith("channels=")) {
			this.channels = Integer.parseInt( line.trim().substring(9) );
		} else {
			for (int i=0; i<this.schedulerNames.length; i++) {
				if (line.startsWith(schedulerNames[i]+":")) {
					String s = line.substring(schedulerNames[i].length()+2);
					if (s.equals("feasible")) {
						this.contents[i].schedulable = 1;
						fillContents(i);
					} else if (s.equals("unschedulable")) {
						this.contents[i].schedulable = 0;
						this.contents[i].reset();
					} else {
						this.contents[i].schedulable = -1;
						this.contents[i].reset();
					}
					if (i == schedulerNames.length-1) {
						outputRow();
					}
					break;
				}
			}
		}
	}
	
	private void fillContents(int id) throws IOException {
		String line = breader.readLine();
		StringTokenizer tokenizer = new StringTokenizer(line);
		tokenizer.nextToken();
		this.contents[id].time = Integer.parseInt(tokenizer.nextToken());
		tokenizer.nextToken();
		this.contents[id].maxBuffer = Integer.parseInt(tokenizer.nextToken());
		tokenizer.nextToken();
		this.contents[id].bufferTimeProduct = Integer.parseInt(tokenizer.nextToken());
		tokenizer.nextToken();
		this.contents[id].hyperPeriod = Integer.parseInt(tokenizer.nextToken());
	}

	private void outputRow() {
		System.out.printf("%d\t%d\t%d\t%.3f\t%.3f\t%d", this.topologyId, this.id, 
				this.flows, this.expectedUtilization, this.actualUtilization, this.channels);
		for (SchedulerContent content : this.contents) {
			System.out.print("\t"+content.schedulable);
			System.out.print("\t"+content.time);
			System.out.print("\t"+content.maxBuffer);
			System.out.print("\t"+content.bufferTimeProduct);
			System.out.print("\t"+content.hyperPeriod);
		}
		System.out.println();
	}

	public static void main(String[] args) throws IOException {
		ConvertSchedulerComparisonResult conv = new ConvertSchedulerComparisonResult();
		conv.convert(args[0]);
	}
}
