package VacationDayPlanner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class Place implements Serializable {
	private static final long serialVersionUID = -7677084253002512358L;
	private String id;
	private String name;
	private Location location;
	private int numRatings;
	private double rating;
	private String vicinity;
	
	public Place(String id, String name, double lat, double lng) {
		this.id = id;
		this.name = name;
		this.location = new Location(lat, lng);
		this.numRatings = -1;
		this.rating = 0.0;
		this.vicinity = "";
	}
	
	public Place(String id, String name, double lat, double lng,
			int numRatings, double rating, String vicinity) {
		this.id = id;
		this.name = name;
		this.location = new Location(lat, lng);
		this.numRatings = numRatings;
		this.rating = rating;
		this.vicinity = vicinity;
	}
	
	public String getId() { return id; }
	
	public String getName() { return name; }

	public Location getLocation() { return location; }
	
	public int getNumRatings() { return numRatings; }
	
	public double getRating() { return rating; }
	
	public String getVicinity() { return vicinity; }
	
	public static void writeDataToFile(String fileName,
			ArrayList<Place> places) {
		try {
			FileOutputStream fos = new FileOutputStream(new File(fileName));
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			
			for (Place p : places) {
				oos.writeObject(p);
			}
			
			oos.close();
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean equals(Object p) {
		if (!(p instanceof Place)) return false;
		
		Place p2 = (Place) p;
		return this.id.equals(p2.id);
	}
	
	@Override
	public String toString() {
		DecimalFormat df = new DecimalFormat("#.##");
		// Convert Place to string
		StringBuilder result = new StringBuilder();
		// Add place name
		result = result.append(name + "\n");
		
		// Add address
		if (vicinity != null)
			result = result.append(vicinity + "\n");
		
		result = result.append("Number of ratings: " + numRatings +
				" Rating: " + df.format(rating) + "\n");

		return result.toString();
	}
}