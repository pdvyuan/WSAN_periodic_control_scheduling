package de.seemoo.dyuan.eval;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

public class ConvertOpportunisticAggregate {
	
	private int topologyId;
	
	private double endFraction;
	
	private int flows;
	
	private double expectedUtilization;
	
	private double actualUtilization;
	
	private int schedulable;
	
	private int channels;
	
	private int piggyback;
	
	private double oaPercent;
	
	private int oaMax;
	
	private int time;
	
	private int maxBuffer;
	
	private int bufferTimeProduct;
	
	private int hyperPeriod;
	
	private BufferedReader breader;
	
	private int id = -1;
	
	public void convert(String inputFile) throws IOException {
		FileReader freader = new FileReader(inputFile);
		breader = new BufferedReader(freader);
		String line;
		
		schedulable = -1;
		System.out.print("top\tid\tflows\tu\tau\tch\tpiggyback");
		System.out.print("\tSch\toaPer\toaMax\ttime\tmaxBuf\tprod\thyper");
		System.out.println();
		while ((line = breader.readLine()) != null) {
			processLine(line);
		}
		freader.close();
	}
	
	private void fillSchedulingContents() throws IOException {
		String line = breader.readLine();
		StringTokenizer tokenizer = new StringTokenizer(line);
		tokenizer.nextToken();
		this.time = Integer.parseInt(tokenizer.nextToken());
		tokenizer.nextToken();
		this.maxBuffer = Integer.parseInt(tokenizer.nextToken());
		tokenizer.nextToken();
		this.bufferTimeProduct = Integer.parseInt(tokenizer.nextToken());
		tokenizer.nextToken();
		this.hyperPeriod = Integer.parseInt(tokenizer.nextToken());
	}
	
	private void fillPiggybackContent() throws IOException {
		String line = breader.readLine();
		StringTokenizer tokenizer = new StringTokenizer(line);
		tokenizer.nextToken();
		tokenizer.nextToken();
		tokenizer.nextToken();
		String s = tokenizer.nextToken().replace("%", "");
		this.oaPercent = Double.parseDouble(s);
		tokenizer.nextToken();
		this.oaMax = Integer.parseInt(tokenizer.nextToken());
		
	}
	
	private void resetPiggybackContent() {
		this.oaPercent = 0;
		this.oaMax = 0;
		
	}
	
	private void resetSchedulingContents() {
		this.time = 0;
		this.maxBuffer = 0;
		this.bufferTimeProduct = 0;
		this.hyperPeriod = 0;
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
			id++;
			StringTokenizer tokenizer = new StringTokenizer(line, ", ");
			String token = tokenizer.nextToken();
			this.expectedUtilization = Double.parseDouble(token.substring(2));
			token = tokenizer.nextToken();
			this.actualUtilization = Double.parseDouble(token.substring(3));
		} else if (line.startsWith("channels=")) {
			this.channels = Integer.parseInt( line.trim().substring(9) );
		} else if (line.startsWith("LST")) {
			if (line.contains("feasible")) {
				this.schedulable = 1;
				fillSchedulingContents();
				if (this.piggyback == 1) {
					fillPiggybackContent();
				} else {
					resetPiggybackContent();
				}
			} else if (line.contains("unschedulable")) {
				this.schedulable = 0;
				resetSchedulingContents();
				resetPiggybackContent();
			} else {
				this.schedulable = -1;
				resetSchedulingContents();
				resetPiggybackContent();
			}
			outputRow();
		} else if (line.startsWith("piggyback")) {
			this.piggyback = line.contains("true") ? 1:0;			
		} 
	}


	private void outputRow() {
		System.out.printf("%d\t%d\t%d\t%.3f\t%.3f\t%d\t%d", this.topologyId, this.id, 
				this.flows, this.expectedUtilization, this.actualUtilization, this.channels, this.piggyback);
		System.out.print("\t"+schedulable);
		System.out.printf("\t%.3f\t%d", this.oaPercent, this.oaMax);
		System.out.print("\t"+this.time);
		System.out.print("\t"+this.maxBuffer);
		System.out.print("\t"+this.bufferTimeProduct);
		System.out.print("\t"+this.hyperPeriod);
		System.out.println();
	}

	public static void main(String[] args) throws IOException {
		ConvertOpportunisticAggregate conv = new ConvertOpportunisticAggregate();
		conv.convert(args[0]);
	}
}
