package de.seemoo.dyuan.netgen.structure;

public class TempNode extends Node {

	private Node backNode;
	
	private boolean first;
	
	public TempNode(Node node, boolean first) {
		super(node.id+(first ? "1st":"2nd"));
//		if (node instanceof TempNode)
//			throw new IllegalArgumentException();
		this.first = first;
		this.backNode = node;
	}
	
	public boolean isFirstNode() {
		return this.first;
	}
	
	public Node getBackNode() {
		return this.backNode;
	}
	
}
