package VacationDayPlanner;

import java.util.ArrayList;

import com.google.maps.model.LatLng;
import com.google.maps.model.PlaceDetails;

public class PlaceCluster {
	private ArrayList<PlaceDetails> items;
	private LatLng avgLoc;
	
	public PlaceCluster() {
		items = new ArrayList<>();
		avgLoc = new LatLng(0.0, 0.0);
	}
	
	public void addPlace(PlaceDetails pd) {
		items.add(pd);
	}
	
	public void recalculateAverage() {
		double totalLat = 0.0, totalLng = 0.0,
				avgLat, avgLng;
		
		for (PlaceDetails pd : items) {
			LatLng location = pd.geometry.location;
			
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
	
	public void reset() {
		items.clear();
		avgLoc = new LatLng(0.0, 0.0);
	}
}