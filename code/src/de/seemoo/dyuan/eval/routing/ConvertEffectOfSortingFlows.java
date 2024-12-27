package de.seemoo.dyuan.eval.routing;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

public class ConvertEffectOfSortingFlows {
	
	private int topologyId;
	
	private int flows;
	
	private double meanReliability;
	
	private int numGWs;
	
	private int routableFlows;
	
	private double meanHops;
	
	private int routingType;
	
	private int numPaths;
	
	private long time;
	
	private int[] feasibles = new int[16];
	
	private int channelId;
	
	private int routingSeq;
	
	public void convert(String inputFile) throws IOException {
		FileReader freader = new FileReader(inputFile);
		BufferedReader breader = new BufferedReader(freader);
		String line;
		System.out.print("top\tnGWs\tnPaths\trouting\tflows\troutableFlows\treliability\thops\ttime\trouteSeq\t");
		for (int ch = 1; ch <= 16; ch++) {
			System.out.print("ch"+ch+"\t");
		}
		System.out.println();
		while ((line = breader.readLine()) != null) {
			processLine(line);
		}
		freader.close();
	}
	


	private void processLine(String line) {
		if (line.startsWith("GWs=")) {
			this.numGWs = Integer.parseInt(line.substring(4));
		} else if (line.startsWith("topo")) {
			this.topologyId = Integer.parseInt(line.substring(5));
		} else if (line.startsWith("routing-type")) {
			StringTokenizer tokenizer = new StringTokenizer(line);
			tokenizer.nextToken();
			String s = tokenizer.nextToken();
			this.routingType = Integer.parseInt(s.substring(0, s.length()-1));
			tokenizer.nextToken();
			this.flows = Integer.parseInt(tokenizer.nextToken());
			tokenizer.nextToken();
			this.routableFlows = Integer.parseInt(tokenizer.nextToken());
			tokenizer.nextToken();
			this.meanReliability = Double.parseDouble(tokenizer.nextToken());
			tokenizer.nextToken();
			this.meanHops = Double.parseDouble(tokenizer.nextToken());
			tokenizer.nextToken();
			this.time = Long.parseLong(tokenizer.nextToken());
		} else if (line.startsWith("numPaths")) {
			StringTokenizer tokenizer = new StringTokenizer(line);
			tokenizer.nextToken();
			this.numPaths = Integer.parseInt(tokenizer.nextToken());
		} else if (line.startsWith("routing-failure")) {
			for (int i=0; i<feasibles.length; i++) {
				feasibles[i] = 0;
			}
			outputRow();
		} else if (line.startsWith("channel count")) {
			StringTokenizer tokenizer = new StringTokenizer(line);
			tokenizer.nextToken();
			tokenizer.nextToken();
			this.channelId = Integer.parseInt(tokenizer.nextToken());
		} else if (line.startsWith("Schedule result:")) {
			if (line.contains("feasible")) {
				this.feasibles[this.channelId-1] = 1;
			} else {
				this.feasibles[this.channelId-1] = 0;
			}
			if (this.channelId == 16) {
				outputRow();
			}
		} else if (line.startsWith("routing-seq")) {
			StringTokenizer tokenizer = new StringTokenizer(line);
			tokenizer.nextToken();
			this.routingSeq = Integer.parseInt(tokenizer.nextToken());
		}
	}
	

	private void outputRow() {
		if (this.flows != 0) {
			System.out.printf("%d\t%d\t%d\t%d\t%d\t%d\t%.3f\t%.3f\t%d\t%d\t", 
					this.topologyId, this.numGWs, this.numPaths, this.routingType, this.flows, 
					this.routableFlows, this.meanReliability, this.meanHops, this.time, this.routingSeq);
			for (int f : this.feasibles) {
				System.out.print(f+"\t");
			}
			System.out.println();
		}
			
		
	}

	public static void main(String[] args) throws IOException {
		ConvertEffectOfSortingFlows conv = new ConvertEffectOfSortingFlows();
		conv.convert(args[0]);
	}
}
