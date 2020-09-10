package VacationDayPlanner;

import java.io.Serializable;

public class Location implements Serializable {
	private static final long serialVersionUID = 1892531909175230310L;
	private double lat;
	private double lng;
	
	public Location(double lat, double lng) {
		this.lat = lat;
		this.lng = lng;
	}
	
	public double getLat() { return lat; }
	
	public double getLng() { return lng; }
	
	public static double EuclideanDistance(Location l1, Location l2) {
		return Math.sqrt(Math.pow(l2.getLat() - l1.getLng(), 2.0) + 
				Math.pow(l2.getLat() - l1.getLng(), 2.0));
	}
}
