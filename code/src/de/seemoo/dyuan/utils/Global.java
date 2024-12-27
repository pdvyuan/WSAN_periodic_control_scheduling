package de.seemoo.dyuan.utils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math.MathException;

public class Global {
	
	public static Random randomGen = new Random(1);
	
	private static CC2420IndoorLQCalculator linkQualityCalculator = new CC2420IndoorLQCalculator();
	
	public static void randomSeed() {
		randomGen = new Random();
	}
	
	public static void randomSeed(int seed) {
		randomGen = new Random(seed);
	}
	
	/**
	 * compute the least common multiple
	 * 
	 */
	public static BigInteger lcm(BigInteger i1, BigInteger i2) {
		BigInteger gcd = i1.gcd(i2);
		return i1.multiply(i2).divide(gcd);
	}
	
	/**
	 * Find all factors greater or equal to 2 or a number.
	 * 
	 * @return the array.
	 */
	public static int[] findFactorsOf(int number) {
		List<Integer> vals = new ArrayList<Integer>();
		for (int val = 2; val <= number; val++) {
			if (number % val == 0) 
				vals.add(val);
		}
		int[] ret = new int[vals.size()];
		for (int i=0; i<ret.length; i++) {
			ret[i] = vals.get(i);
		}
		return ret;
	}
	
	public static double genLinkQuality(double dist) {
		try {
			return linkQualityCalculator.calculatePRR(dist, CC2420IndoorLQCalculator.MAX_PHY_PACKET_SIZE);
		} catch (MathException e) {
			throw new IllegalArgumentException(e);
		}
	}	

}
