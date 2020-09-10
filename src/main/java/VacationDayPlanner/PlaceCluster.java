package VacationDayPlanner;

import java.util.ArrayList;

public class PlaceCluster {
	private ArrayList<Place> places;
	private Location avgLoc;
	
	public PlaceCluster() {
		places = new ArrayList<>();
		avgLoc = new Location(0.0, 0.0);
	}
	
	public void addPlace(Place p) {
		places.add(p);
	}
	
	public void recalculateAverage() {
		double totalLat = 0.0, totalLng = 0.0,
				avgLat, avgLng;
		
		for (Place p : places) {
			Location location = p.getLocation();
			
			totalLat += location.getLat();
			totalLng += location.getLng();
		}
		
		avgLat = totalLat / places.size();
		avgLng = totalLng / places.size();
		
		avgLoc = new Location(avgLat, avgLng);
	}
	
	public Location getAvgValue() {
		return avgLoc;
	}
	
	public ArrayList<Place> getPlaces() {
		return new ArrayList<>(places);
	}
	
	public void reset() {
		places.clear();
		avgLoc = new Location(0.0, 0.0);
	}
}