package de.seemoo.dyuan.netgen;

public class Coordinates {
	public double x;
	public double y;
	
	public Coordinates(double x, double y) {
		this.x = x;
		this.y = y;
	}
	
	public Coordinates(Coordinates co) {
		this(co.x, co.y);
	}
	
	public double euclidDistance(Coordinates co) {
		return Math.sqrt( (this.x - co.x) * (this.x - co.x) + (this.y - co.y) * (this.y - co.y) );
	}
	
}