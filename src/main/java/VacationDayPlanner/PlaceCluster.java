package VacationDayPlanner;

import java.util.ArrayList;

import com.google.maps.model.LatLng;
import com.google.maps.model.PlaceDetails;
import com.google.maps.model.PlacesSearchResult;

public class PlaceCluster {
	private ArrayList<PlacesSearchResult> items;
	private LatLng avgLoc;
	
	public PlaceCluster() {
		items = new ArrayList<>();
		avgLoc = new LatLng(0.0, 0.0);
	}
	
	public void addPlace(PlacesSearchResult psr) {
		items.add(psr);
	}
	
	public void recalculateAverage() {
		double totalLat = 0.0, totalLng = 0.0,
				avgLat, avgLng;
		
		for (PlacesSearchResult psr : items) {
			LatLng location = psr.geometry.location;
			
			totalLat += location.lat;
			totalLng += location.lng;
		}
		
		avgLat = totalLat / items.size();
		avgLng = totalLng / items.size();
		
		avgLoc = new LatLng(avgLat, avgLng);
	}
	
	public LatLng getAvgValue() {
		return avgLoc;
	}
	
	public ArrayList<PlacesSearchResult> getPlaces() {
		return new ArrayList<PlacesSearchResult>(items);
	}
	
	public void reset() {
		items.clear();
		avgLoc = new LatLng(0.0, 0.0);
	}
}