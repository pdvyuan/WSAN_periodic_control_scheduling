package de.seemoo.dyuan.eval;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

public class ConvertGatewayPlacementResult {
	
	private int topologyId;
	
	private double endFraction;
	
	private int path;
	
	private double expectedUtilization;
	
	private double actualUtilization;
	
	public final String[] schedulers = {"CLLF", "RM", "DM", "EDF", "LST", "PD",
			"EPD", "Pfair-PF", "Pfair-PD2", "ERfair-PF", "ERfair-PD2", "EDZL"};
	
	private int[] schedulable;
	
	private int recordIndex = 1;
	
	
	public void convert(String inputFile) throws IOException {
		FileReader freader = new FileReader(inputFile);
		BufferedReader breader = new BufferedReader(freader);
		String line;
		this.schedulable = new int[schedulers.length];
		clearSchedulable();
		System.out.print("top\te\tpath\tu1\tau1");
		for (String name : schedulers) {
			System.out.print("\t"+name+"1");
		}
		System.out.print("\tu2\tau2");
		for (String name : schedulers) {
			System.out.print("\t"+name+"2");
		}
		System.out.print("\tu3\tau3");
		for (String name : schedulers) {
			System.out.print("\t"+name+"3");
		}
		System.out.println();
		while ((line = breader.readLine()) != null) {
			processLine(line);
		}
		freader.close();
	}
	
	private void clearSchedulable() {
		for (int i=0; i<schedulable.length; i++) {
			schedulable[i] = -1;
		}
	}

	private void processLine(String line) {
		if (line.startsWith("topo")) {
			this.topologyId = Integer.parseInt(line.substring(5));
		} else if (line.startsWith("e=")) {
			StringTokenizer tokenizer = new StringTokenizer(line, ", ");
			String token = tokenizer.nextToken();
			this.endFraction = Double.parseDouble(token.substring(2));
			token = tokenizer.nextToken();
			this.path = Integer.parseInt(token.substring(5));
		} else if (line.startsWith("u=")) {
			StringTokenizer tokenizer = new StringTokenizer(line, ", ");
			String token = tokenizer.nextToken();
			this.expectedUtilization = Double.parseDouble(token.substring(2));
			token = tokenizer.nextToken();
			this.actualUtilization = Double.parseDouble(token.substring(3));
		} else {
			for (int i=0; i<schedulers.length; i++) {
				if (line.startsWith(schedulers[i])) {
					String s = line.substring(schedulers[i].length()+2);
					if (s.equals("feasible")) {
						schedulable[i] = 1;
					} else {
						schedulable[i] = 0;
					}
					if (i == schedulers.length-1) {
						outputRow();
					}
					break;
				}
			}
		}
	}

	private void outputRow() {
		if (recordIndex % 3 == 1) {
			System.out.printf("%d\t%.3f\t%d\t%.3f\t%.3f", this.topologyId, this.endFraction, this.path, this.expectedUtilization, this.actualUtilization);
			for (int feasible : schedulable) {
				System.out.print("\t"+feasible);
			}
		} else {
			System.out.printf("\t%.3f\t%.3f", this.expectedUtilization, this.actualUtilization);
			for (int feasible : schedulable) {
				System.out.print("\t"+feasible);
			}
			if (recordIndex % 3 == 0)
				System.out.println();
		}
		recordIndex++;
		clearSchedulable();
	}

	public static void main(String[] args) throws IOException {
		ConvertGatewayPlacementResult conv = new ConvertGatewayPlacementResult();
		conv.convert(args[0]);
	}
}
