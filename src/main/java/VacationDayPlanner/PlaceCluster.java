package VacationDayPlanner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.maps.model.LatLng;
import com.google.maps.model.PlacesSearchResult;

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
	
	private static PlaceCluster getLargestPlaceCluster(ArrayList<PlaceCluster> clusters) {
		PlaceCluster largest = new PlaceCluster();
		int maxSize = 0;
		
		for (PlaceCluster pc : clusters) {
			if (pc.getPlaces().size() > maxSize) {
				largest = pc;
				maxSize = pc.getPlaces().size();
			}
		}
		
		return largest;
	}
	
	public static PlaceCluster[] 
			kMeans(ArrayList<Place> destinations, int days) {
		// Create the array of clusters
		Map<Place, Integer> placeClusters = new HashMap<>();
		PlaceCluster[] clusters = new PlaceCluster[days];
		
		for (int i = 0; i < days; ++i)
			clusters[i] = new PlaceCluster();
		
		// Copy the destination ArrayList so we can shuffle and
		// delete elements without affecting the original
		ArrayList<Place> destCopy = new ArrayList<>(destinations);
		
		// Shuffle the list
		Collections.shuffle(destCopy);
		// Partition the dest_copy array into days
		for (int i = 0; i < days; ++i) {
			double start = i * (destCopy.size() / (double) days);
			double end = (i + 1) * (destCopy.size() / (double) days);
			List<Place> randomPartition =
					destCopy.subList((int) start, (int) end);
			for (Place p : randomPartition) {
				clusters[i].addPlace(p);
				placeClusters.put(p, i);
			}
			
			clusters[i].recalculateAverage();
		}
		
		
		// Now that we have initialized the clusters and the averages
		// for each cluster, we can now repeat the process of calculating
		// the new clusters for each point and recalculating the cluster
		// averages until no more points have changed clusters.
		boolean done = false;
		while (!done) {
			done = true;
			
			Set<Place> keys = placeClusters.keySet();
			for (Place p : keys) {
				int oldClusterIdx = placeClusters.get(p);
				Location pdLoc = p.getLocation();
				int nearestClusterIdx = 0;
				double nearestClusterDist = Location
						.EuclideanDistance(pdLoc, clusters[0].getAvgValue());
				
				for (int i = 1; i < days; ++i) {
					double dist = Location.
							EuclideanDistance(pdLoc, clusters[i].getAvgValue());
					
					if (dist < nearestClusterDist) {
						nearestClusterDist = dist;
						nearestClusterIdx = i;
					}
				}
				
				if (oldClusterIdx != nearestClusterIdx) {
					done = false;
					placeClusters.put(p, nearestClusterIdx);
				}
			}
			
			// Reset the cluster
			for (int i = 0; i < days; ++i) {
				clusters[i].reset();
			}
			
			// Put each place in its new cluster
			for (Place p : keys) {
				int idx = placeClusters.get(p);
				clusters[idx].addPlace(p);
			}
			
			// Recalculate averages for each cluster
			for (int i = 0; i < days; ++i) {
				clusters[i].recalculateAverage();
			}
		}
		
		ArrayList<PlaceCluster> nonEmptyClusters = new ArrayList<>();
		
		for (int i = 0; i < clusters.length; ++i) {
			ArrayList<Place> places = clusters[i].getPlaces();
			
			if (places.size() > 0)
				nonEmptyClusters.add(clusters[i]);
		}
		
		do {
			if (nonEmptyClusters.size() < days) {
				PlaceCluster largestCluster = PlaceCluster
						.getLargestPlaceCluster(nonEmptyClusters);
				
				PlaceCluster[] splitLargest = kMeans(largestCluster.getPlaces(), 2);
				nonEmptyClusters.remove(largestCluster);
				for (int i = 0; i < splitLargest.length; ++i) {
					if (splitLargest[i].getPlaces().size() > 0)
						nonEmptyClusters.add(splitLargest[i]);
				}
			}
		} while (nonEmptyClusters.size() < days);
		
		PlaceCluster[] clusterList = new PlaceCluster[nonEmptyClusters.size()];
		
		int i = 0;
		for (PlaceCluster pc : nonEmptyClusters) {
			clusterList[i++] = pc;
		}
		
		return clusterList;
	}
}