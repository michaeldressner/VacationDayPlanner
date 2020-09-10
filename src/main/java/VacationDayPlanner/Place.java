package VacationDayPlanner;

public class Place {
	private String name;
	private double lat;
	private double lng;
	private int numRatings;
	private double rating;
	private String vicinity;
	
	public Place(String name, double lat, double lng) {
		this.name = name;
		this.lat = lat;
		this.lng = lng;
		this.numRatings = -1;
		this.rating = 0.0;
		this.vicinity = "";
	}
	
	public Place(String name, double lat, double lng, int numRatings,
			double rating, String vicinity) {
		this.name = name;
		this.lat = lat;
		this.lng = lng;
		this.numRatings = numRatings;
		this.rating = rating;
		this.vicinity = vicinity;
	}
	
	public String getName() { return name; }
	
	public double getLat() { return lat; }
	
	public double getLng() { return lng; }
	
	public int getNumRatings() { return numRatings; }
	
	public double getRating() { return rating; }
	
	public String getVicinity() { return vicinity; }
}