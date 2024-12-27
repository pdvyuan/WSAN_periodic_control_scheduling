package de.seemoo.dyuan.netgen.greenorbs;

public class Neighbour {
	
	private int neighbourId;
	
	private double pathEtx;
	
	public Neighbour(int nId, double pathEtx) {
		this.neighbourId = nId;
		this.pathEtx = pathEtx;
	}
	
	public int getNeighbourId() {
		return this.neighbourId;
	}
	
	public double getPathETX() {
		return this.pathEtx;
	}
	
	public void setPathETX(double etx) {
		this.pathEtx = etx;
	}

}
