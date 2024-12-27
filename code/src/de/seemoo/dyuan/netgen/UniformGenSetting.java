package de.seemoo.dyuan.netgen;

/**
 * The setting parameters for uniform generators.
 * @author dyuan
 *
 */
public class UniformGenSetting {
	public final int nodes;
	public final double side;
	public final double commRange;
	
	public UniformGenSetting(int nodes, double side, double commRange) {
		this.nodes = nodes;
		this.side = side;
		this.commRange = commRange;
	}
}
