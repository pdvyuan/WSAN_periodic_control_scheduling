package de.seemoo.dyuan.test.unit;

import java.math.BigInteger;

import de.seemoo.dyuan.netgen.Flow;
import de.seemoo.dyuan.netgen.NetworkModel;
import de.seemoo.dyuan.utils.Global;
import junit.framework.TestCase;

public class TestHyperPeriod extends TestCase {
	
	public void testLCM() {
		BigInteger a = BigInteger.valueOf(0);
		BigInteger b = BigInteger.valueOf(10);
		assertEquals(b, a.gcd(b));
		assertEquals(0, Global.lcm(a, b).intValue());
		a = BigInteger.valueOf(35);
		assertEquals(70, Global.lcm(a, b).intValue());
	}

	public void testHyperPeriod() {
		NetworkModel network = new NetworkModel();
		Flow flow = network.newFlow(null, null);
		flow.setPeriod(10);
		flow = network.newFlow(null, null);
		flow.setPeriod(35);
		flow = network.newFlow(null, null);
		flow.setPeriod(22);
		assertEquals(BigInteger.valueOf(770), network.computeHyperiod(true));
		assertEquals(BigInteger.valueOf(0), network.computeHyperiod(false));
	}
	
}
