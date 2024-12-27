OpportunisticAggregate

	private final int numPathsPerFlow = 2;
	private final int[] all_channels = {1, 2, 4, 8, 16};
	
	private final Type type = Type.PhysicalModel;
	private final boolean implicitDeadline = false;
	
	private final int numSensors = 100;
	private final int numGWs = 2;
	private final int numTopologies = 100;
	private final int numFlowsToTry = 5;
	private final int numUtilsToTry = 10;
	private final int maxFlows = numSensors / 2;
	private final int maxUtil = 25;