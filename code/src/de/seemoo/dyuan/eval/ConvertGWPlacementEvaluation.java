package de.seemoo.dyuan.eval;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

public class ConvertGWPlacementEvaluation {
	
	private int topologyId;
	
	private double endFraction;
	
	private int flows;
	
	private double expectedUtilization;
	
	private double actualUtilization;
	
	private int channels;
	
	private int gwType;
	
	private int schedulable;
	
	private int gwReliable;
	
	private double reliability;
	
	public void convert(String inputFile) throws IOException {
		FileReader freader = new FileReader(inputFile);
		BufferedReader breader = new BufferedReader(freader);
		String line;
		
		System.out.print("top\te\tflows\tu\tau\tch");
		System.out.println("\tEDZL\tGWType\tGWReliable\tReliability");
		while ((line = breader.readLine()) != null) {
			processLine(line);
		}
		freader.close();
	}
	


	private void processLine(String line) {
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
		} else if (line.startsWith("channels=")) {
			this.channels = Integer.parseInt( line.trim().substring(9) );
		} else if (line.startsWith("EDZL")) {
			String s = line.substring(6);
			if (s.equals("feasible")) {
				schedulable = 1;
			} else {
				schedulable = 0;
			}
			outputRow();
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
		} else if (line.startsWith("reliability")) {
			this.reliability = Double.parseDouble(line.substring("reliability=".length()).trim());
		}
	}
	

	private void outputRow() {
		System.out.printf("%d\t%.3f\t%d\t%.3f\t%.3f\t%d", this.topologyId, this.endFraction, 
				this.flows, this.expectedUtilization, this.actualUtilization, this.channels);
		System.out.print("\t"+schedulable);
		System.out.print("\t"+this.gwType);
		System.out.print("\t"+this.gwReliable);
		System.out.printf("\t%.3f", this.reliability);
		System.out.println();
	}

	public static void main(String[] args) throws IOException {
		ConvertGWPlacementEvaluation conv = new ConvertGWPlacementEvaluation();
		conv.convert(args[0]);
	}
}
