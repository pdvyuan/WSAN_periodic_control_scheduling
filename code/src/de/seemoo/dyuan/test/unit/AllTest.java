package de.seemoo.dyuan.test.unit;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTest {
	
	public static Test suite() {
		TestSuite suite = new TestSuite("Unit tests");
		suite.addTestSuite(TestFixedPriorityScheduler.class);
		suite.addTestSuite(TestNetworkGeneration.class);
		suite.addTestSuite(TestDisjointPaths.class);
		suite.addTestSuite(TestBhandariAlgorithm.class);
		suite.addTestSuite(TestPathAndSubpath.class);
		suite.addTestSuite(TestHyperPeriod.class);
		suite.addTestSuite(TestBFAlgo.class);
		suite.addTestSuite(TestKDAlgo.class);
		suite.addTestSuite(TestMostReliablePairDisjointPath.class);
		
		return suite;
	}

}
