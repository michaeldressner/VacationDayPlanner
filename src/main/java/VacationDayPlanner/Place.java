package VacationDayPlanner;

import java.io.Serializable;

public class Place implements Serializable {
	private static final long serialVersionUID = -7677084253002512358L;
	private String name;
	private Location location;
	private int numRatings;
	private double rating;
	private String vicinity;
	
	public Place(String name, double lat, double lng) {
		this.name = name;
		this.location = new Location(lat, lng);
		this.numRatings = -1;
		this.rating = 0.0;
		this.vicinity = "";
	}
	
	public Place(String name, double lat, double lng, int numRatings,
			double rating, String vicinity) {
		this.name = name;
		this.location = new Location(lat, lng);
		this.numRatings = numRatings;
		this.rating = rating;
		this.vicinity = vicinity;
	}
	
	public String getName() { return name; }
	
	public Location getLocation() { return location; }
	
	public int getNumRatings() { return numRatings; }
	
	public double getRating() { return rating; }
	
	public String getVicinity() { return vicinity; }
}