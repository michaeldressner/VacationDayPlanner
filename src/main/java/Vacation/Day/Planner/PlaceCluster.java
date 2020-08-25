package Vacation.Day.Planner;

import java.util.ArrayList;

import com.google.maps.model.LatLng;
import com.google.maps.model.PlaceDetails;

public class PlaceCluster {
	private ArrayList<PlaceDetails> items;
	private LatLng avgValue;
	
	public PlaceCluster() {
		items = new ArrayList<>();
		avgValue = new LatLng(0.0, 0.0);
	}
	
	public void addPlace(PlaceDetails pd) {
		items.add(pd);
	}
	
	public void recalculateAverage() {
		double totalLat, totalLng;
	}
	
	public LatLng getAvgValue() {
		return avgValue;
	}
}