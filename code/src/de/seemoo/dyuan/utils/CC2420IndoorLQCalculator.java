package de.seemoo.dyuan.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import org.apache.commons.math.MathException;
import org.apache.commons.math.special.Erf;

/**
 * The class calculates the link quality given a distance.
 * According to the log-normal model for in-door environment 
 * and practical TOSSIM model.
 * 
 * A maximum-size IEEE 802.15.4 message is 133 byes.
 * The physical header is 6 bytes. So the MAC layer maximum size is 127 bytes.
 * 
 * The minimum MAC header and footer are 9 bytes and the maximum MAC header without the security bytes is 25 bytes.
 * So the maximum MAC payload could range between 127-25=102 bytes and 127-9=118 bytes depending on the MAC header format.
 * 
 * A WirelessHART message is an IEEE 802.15.4-2003 one. Its data link layer
section that corresponds to IEEE 802.15.4 MAC header is 11 bytes, or 17 bytes if
there is one long address. The extra WirelessHART data link layer fields that fall
in the IEEE 802.15.4 MAC payload are 5 bytes. The WirelessHART standard still
allows the maximum message length to be 133 bytes, which means its maximum
data link payload is 127-11-5=111 bytes, or 127-17-5=105 bytes.
 * 
 * @author dyuan
 *
 */
public class CC2420IndoorLQCalculator {
	
	public static final double PATH_LOSS_EXPONENT = 2.16;
	
	//the noise floor of CC2420.
	public static final int NOISE_FLOOR = -98;
	
	public static final double beta1 = 0.9794;
	
	public static final double beta2 = 2.3851;
	
	public static final int REFERENCE_DISTANCE = 15;
	
	public static final double REFERENCE_PATH_LOSS = 71.84;
	
	public static final int MAX_PHY_PACKET_SIZE = 133;
	
	public static final double SHADOWING_STANDARD_DEVIATION = 8.13;
	
	/**
	 * WirelessHART mesh covers
		a relatively larger area. All devices must provide a nominal EIRP of +10dBm
		(10mW) ¡À3dB. The transmit power is programmable from -10dBm to +10dBm.
		The maximum outdoor line of sight transmission distance could be 100 meters.
	 */
	public static final int TRANSMISSION_POWER = 0; 
	
	/**
	 * 
	 * @param d distance measured in meters
	 * @param l PPDU measured in bytes
	 * @param randVarShadowing random variable due to shadowing
	 * 
	 */
	private double calculatePRR(double d, int l, double randVarShadowing) throws MathException {
		double pathLoss = this.calculatePathLoss(d);
		pathLoss = pathLoss + randVarShadowing;
		double rtPower = TRANSMISSION_POWER - pathLoss;
		double snr = rtPower - NOISE_FLOOR;
		double pse = 0.5 * Erf.erfc(beta1 * (snr - beta2) / Math.sqrt(2));
		double prr = Math.pow(1 - pse, 2*l);
		if (prr > 1.0 || prr < 0) {
			throw new IllegalArgumentException("impossible PRR values for params "+d+", "+l+", "+randVarShadowing);
		}
		return prr;
	}
	
	public double calculatePRR(double d, int l) throws MathException {
		return this.calculatePRR(d, l, Global.randomGen.nextGaussian() * SHADOWING_STANDARD_DEVIATION);
	}
	
	/**
	 * Consider 2 std.
	 * @throws MathException 
	 */
	public double calculatePRR95UB(double d, int l) throws MathException {
		return this.calculatePRR(d, l, -SHADOWING_STANDARD_DEVIATION*2);
	}
	
	public double calculatePRR95LB(double d, int l) throws MathException {
		return this.calculatePRR(d, l, SHADOWING_STANDARD_DEVIATION*2);
	}
	
	private double calculatePathLoss(double d) {
		return REFERENCE_PATH_LOSS + 10*PATH_LOSS_EXPONENT*Math.log10(d/REFERENCE_DISTANCE);
	}
	
	
	public static void main(String[] args) throws IOException, MathException {
		CC2420IndoorLQCalculator calc = new CC2420IndoorLQCalculator();
//		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
//		while (true) {
//			System.out.println("please input distance and PPDU length:");
//			String line;
//			if ((line = reader.readLine()) != null) {
//				StringTokenizer tok = new StringTokenizer(line);
//				double dist = Double.parseDouble(tok.nextToken());
//				int len = Integer.parseInt(tok.nextToken());
//				double lq = calc.calculatePRR(dist, len);
//				System.out.printf("PRR=%.3f%%\n", lq*100);
//			}
//		}
		double dist = 10;
		while (dist <= 1000) {
			double rate1 = calc.calculatePRR95UB(dist, MAX_PHY_PACKET_SIZE);
			double rate2 = calc.calculatePRR95LB(dist, MAX_PHY_PACKET_SIZE);
			System.out.printf("%.4f\t%.4f\t%.4f\n", dist, rate1, rate2);
			dist += 1;
		}
	}

}
