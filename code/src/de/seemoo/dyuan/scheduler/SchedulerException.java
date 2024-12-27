package de.seemoo.dyuan.scheduler;

public class SchedulerException extends Exception {
	
	public static int NOT_ENOUGH_SLOTS_FOR_FUTURE_TRANSMISSION = 0;
	
	public static int PACKET_NOT_RELEASED = 1;
	
	public static int NO_MORE_TRANSMISSION_AVAILABLE = 2;
	
	public static int UNEXPECTED_EXCEPTION = 3;
	
	public static int LAXITY_LESS_THAN_0 = 4;
	
	private int reason;
	
	public SchedulerException(int reason) {
		this.reason = reason;
	}
	
	public int getReason() {
		return this.reason;
	}

}
