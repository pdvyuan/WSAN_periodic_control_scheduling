package de.seemoo.dyuan.scheduler;

import de.seemoo.dyuan.netgen.Subpath;
import de.seemoo.dyuan.netgen.structure.Edge;
import de.seemoo.dyuan.netgen.structure.Node;

/**
 * ActiveTransmission. the edge of the transmission and the path Id.
 * @author dyuan
 *
 */
public class ActiveTransmission {
	public Edge edge;
	public Node sender;
	public Subpath subpath;
	public FlowTransmissionState flowState;
}
