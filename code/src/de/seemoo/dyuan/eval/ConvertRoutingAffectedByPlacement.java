package de.seemoo.dyuan.eval;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

public class ConvertRoutingAffectedByPlacement {
	
	private int topologyId;
	
	private int flows;

	private int gwType;
	
	private int gwSubType;
	
	private int gwReliable;
	
	private double meanReliability;
	
	private double meanWeightedReliability;
	
	private int numGWs;
	
	private int actFlows;
	
	private double meanHops;
	
	private double meanWeightedHops;
	
	public void convert(String inputFile) throws IOException {
		FileReader freader = new FileReader(inputFile);
		BufferedReader breader = new BufferedReader(freader);
		String line;
		
		System.out.println("top\tnumGWs\tGWType\tGWSubType\tGWReliable\tflows\tactFlows\treliability\tweighted-reliability\thops\tweighted-hops");
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
		} else if (line.startsWith("GWType")) {
			String typeName = line.substring(7).trim();
			int id = EffectOfPlacement.getTypeIdFromName(typeName);
			this.gwType = id;
		} else if (line.startsWith("gwReliable")) {
			if (line.contains("true")) {
				this.gwReliable = 1;
			} else {
				this.gwReliable = 0;
			}
		} else if (line.startsWith("flows")) {
			StringTokenizer tokenizer = new StringTokenizer(line);
			tokenizer.nextToken();
			this.flows = Integer.parseInt(tokenizer.nextToken());
			tokenizer.nextToken();
			this.actFlows = Integer.parseInt(tokenizer.nextToken());
			tokenizer.nextToken();
			this.meanReliability = Double.parseDouble(tokenizer.nextToken());
			tokenizer.nextToken();
			this.meanWeightedReliability = Double.parseDouble(tokenizer.nextToken());
			tokenizer.nextToken();
			this.meanHops = Double.parseDouble(tokenizer.nextToken());
			tokenizer.nextToken();
			this.meanWeightedHops = Double.parseDouble(tokenizer.nextToken());
			this.outputRow();
		} else if (line.startsWith("distance_computing_type")) {
			StringTokenizer tokenizer = new StringTokenizer(line);
			tokenizer.nextToken();
			this.gwSubType = Integer.parseInt(tokenizer.nextToken());
		}
	}
	

	private void outputRow() {
		System.out.printf("%d\t%d\t%d\t%d\t%d\t%d\t%d\t%.3f\t%.3f\t%.3f\t%.3f\n", 
				this.topologyId, this.numGWs, this.gwType, this.gwSubType, this.gwReliable, 
				this.flows, this.actFlows, this.meanReliability, this.meanWeightedReliability,
				this.meanHops, this.meanWeightedHops);
		
	}

	public static void main(String[] args) throws IOException {
		ConvertRoutingAffectedByPlacement conv = new ConvertRoutingAffectedByPlacement();
		conv.convert(args[0]);
	}
}
