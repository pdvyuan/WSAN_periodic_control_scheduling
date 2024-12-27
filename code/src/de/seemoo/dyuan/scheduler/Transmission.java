package de.seemoo.dyuan.scheduler;

import de.seemoo.dyuan.netgen.structure.Edge;

/**
 * A class representing the information of a pending transmission.
 * @author dyuan
 *
 */
public class Transmission {
	/**
	 * path Id in the Scheduler.
	 */
	public int pathId;
	public int anticipatedReleaseTime;
	public int deadline;
	public Edge preEdge = null;
	public Edge postEdge = null;
	public Edge currEdge;
}