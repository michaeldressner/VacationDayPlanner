package VacationDayPlanner;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

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
	
	public static ArrayList<Place> mergeDataLists( ArrayList<Place> l1,
			ArrayList<Place> l2) {
		Set<Place> mergedSet = new TreeSet<>(
				new Comparator<Place>() {
					@Override
					public int compare(Place p1, Place p2) {
						return p1.getId().compareTo(p2.getId());
					}
				}
		);
		ArrayList<Place> result = new ArrayList<>();
		
		mergedSet.addAll(l1);
		mergedSet.addAll(l2);
		
		result.addAll(mergedSet);
		return result;
	}
	
	public static ArrayList<Place> getDataFromFile(String fileName) {
		ArrayList<Place> result = new ArrayList<>();
		
		try {
			FileInputStream fis = new FileInputStream(fileName);
			ObjectInputStream ois = new ObjectInputStream(fis);
			
			try {
				while (true) {
					Place p = (Place) ois.readObject();
					result.add(p);
				}
			}
			catch (EOFException e) {
				// No more PlaceDetail records in the file
				return result;
			}
			catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			finally {
				ois.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
}