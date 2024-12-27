package de.seemoo.dyuan.eval;

import de.seemoo.dyuan.netgen.UniformGenSetting;

public class Settings {
	
	// node degree 10.0023, 10000 random networks, with 2 GWs in grid center, GWs and other nodes have random link.
	public static final UniformGenSetting DISK_SMALL_NET_HIGH_CONN = new UniformGenSetting(100, 500, 96.4);

	// node degree 3.0097, 10000 random networks, with 2 GWs in grid center, GWs and other nodes have random link.
	public static final UniformGenSetting DISK_SMALL_NET_LOW_CONN = new UniformGenSetting(100, 500, 50.8);
	
	// node degree 10.0050, 10000 random networks, with 2 GWs in grid center, GWs and other nodes have random link.
	public static final UniformGenSetting REAL_SMALL_NET_HIGH_CONN = new UniformGenSetting(100, 1249, -1);
	
	
	public static int getCommunicationRange(boolean highConnectivity) {
		if (highConnectivity)
			return 100;
		return 80;
	}
	
	//random model, comm range 100, mean degree = 5.5979
	//random model, comm range 80, mean degree = 3.7240
	
	//physical model, side = 1200, mean degree = 5.5051
	//physical model, side = 1600, mean degree = 3.5526
	public static int getSpaceSide(boolean physicalModel, boolean highConnectivity) {
		if (physicalModel) {
			return (highConnectivity ? 1200 : 1800);
		} 
		return 500;
	}
	
	public static int getLinkQualityLB() {
		return 50;
	}
	
	public static int getLinkQualityUB() {
		return 100;
	}

}
