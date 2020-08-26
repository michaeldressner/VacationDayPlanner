package VacationDayPlanner;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import com.google.maps.FindPlaceFromTextRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PlacesApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.LatLng;
import com.google.maps.model.PlaceDetails;
import com.google.maps.model.PlaceType;
import com.google.maps.model.PlacesSearchResponse;
import com.google.maps.model.PlacesSearchResult;

public class Main {
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		int mainMenuChoice, radius;
		String apiKey, input;
		ArrayList<PlaceDetails> allPlaceDetails;
		
		printIntroduction();
		
		System.out.println("Choose one of the following options: ");
		System.out.println("1. Run a new dataset from the web (uses a lot of"
				+ " API calls but saves the data in the process)");
		System.out.println("2. Run a dataset from a file ");
		do {
			System.out.print("Enter your choice: ");
			mainMenuChoice = Integer.parseInt(scanner.nextLine());
		} while (mainMenuChoice < 1 || mainMenuChoice > 2);
		
		if (mainMenuChoice == 1) {
			System.out.print("Enter your API key: ");
			apiKey = scanner.nextLine();
			
			System.out.print("Enter a place: ");
			input = scanner.nextLine();
			
			// Note: converts to meters by multiplying by 1609
			System.out.print("Enter the search radius in miles (max 31): ");
			radius = Integer.parseInt(scanner.nextLine()) * 1609;
			
			allPlaceDetails = getNewData(apiKey, input,
					radius);
			
			boolean storeData;
			String storeChoice;
			do {
				System.out.print("Would you like to store this data to a file"
						+ " (y/n): ");
				storeChoice = scanner.nextLine();
			} while (!storeChoice.equalsIgnoreCase("y") &&
					!storeChoice.equalsIgnoreCase("n"));
			
			storeData = storeChoice.equalsIgnoreCase("y") ? true : false;
			if (storeData) {
				System.out.print("Enter filename: ");
				String fileName = scanner.nextLine();
				
				writeDataToFile(fileName, allPlaceDetails);
			}
			
		}
		else if (mainMenuChoice == 2) {
			System.out.print("Enter a file name: ");
			String fileName = scanner.nextLine();
			allPlaceDetails = getDataFromFile(fileName);
		}
		else { // Should not happen
			throw new IllegalArgumentException(
					"How did this even happen?");
		}
		
		// Ask the user if they would like to go to each
		// places and make a list.
		ArrayList<PlaceDetails> destinations =
				new ArrayList<>();
		for (PlaceDetails pd : allPlaceDetails) {
			System.out.println();
			System.out.println("Would you like to go to the following"
					+ " place?");
			System.out.println(placeDetailsToString(pd));
			
			String yesNoQuit;
			do {
					System.out.print("(y/n or q to stop entering"
							+ " destinations): ");
					yesNoQuit = scanner.nextLine();
			} while (!yesNoQuit.equalsIgnoreCase("y") &&
					!yesNoQuit.equalsIgnoreCase("n") &&
					!yesNoQuit.equalsIgnoreCase("q"));
			
			if (yesNoQuit.equalsIgnoreCase("y")) {
				destinations.add(pd);
				System.out.println(pd.name + " added to destinations");
			}
			else if (yesNoQuit.equalsIgnoreCase("q")) {
				System.out.println();
				break;
			}
			System.out.println();
		}
		
		int days;
		do {
			System.out.print("Enter the number of days on vacation (must be less"
				+ " or equal to " + destinations.size() + "): ");
			days = Integer.parseInt(scanner.nextLine());
		} while (days > destinations.size() || days < 1);
		
		PlaceCluster[] clusters = kMeans(destinations, days);
		ArrayList<PlaceCluster> nonEmptyClusters = new ArrayList<>();
		
		for (int i = 0; i < clusters.length; ++i) {
			ArrayList<PlaceDetails> places = clusters[i].getPlaces();
			
			if (places.size() > 0)
				nonEmptyClusters.add(clusters[i]);
		}
		
		System.out.println("Your input was grouped into "
				+ nonEmptyClusters.size() + " clusters: ");
		
		int i = 0;
		for (PlaceCluster pc : nonEmptyClusters) {
			System.out.println();
			System.out.println("Cluster " + (i++ + 1) + ":");
			
			for (PlaceDetails pd : pc.getPlaces()) {
				System.out.println(pd.name);
			}
		}
	}

	private static class ReviewDescComparator
			implements Comparator<PlaceDetails> {
		@Override
		public int compare(PlaceDetails pd1, PlaceDetails pd2) {
			return pd2.userRatingsTotal - pd1.userRatingsTotal;
		}
	}
	
	private static void printIntroduction() {
		String welcome = "Welcome to Vacation Day Planner\n"
				+ "If used correctly, this tool can be used to plan where to\n"
				+ "visit on a vacation to a specific destination, as well as\n"
				+ "discover new tourist attractions in the process. In order\n"
				+ "to use this program correctly, keep the following in mind:\n"
				+ "\n"
				+ "1. This program is not actually meant to provide you with\n"
				+ "   a list of things to do in the same day, which might\n"
				+ "   sound a bit contradictory. It groups the locations\n"
				+ "   that you would like to go visit into clusters of places\n"
				+ "   that are close to one another. In some cases, you might\n"
				+ "   find there are too many things on the list to reasonably\n"
				+ "   fit into an actual day, so it is up to you to make a\n"
				+ "   reasonable day schedule from the output of the program.\n"
				+ "   There also may be fewer clusters than days. In this case,\n"
				+ "   you either may be able to fit everything into fewer days\n"
				+ "   (and do even more than you planned the remaining days), or\n"
				+ "   you might have to take some items from the cluster and do\n"
				+ "   them one day, then come back and do the remaining items from\n"
				+ "   the cluster the next day. This is not a scheduling app perse,\n"
				+ "   might be at some point in the future. It still is useful if\n"
				+ "   used appropriately.\n";
		
		System.out.print(welcome);
	}

	private static String placeDetailsToString(PlaceDetails pd) {
		// Convert PlacesSearchResult to string
		StringBuilder result = new StringBuilder();
		// Add place name
		result = result.append(pd.name);
		
		// Add address
		if (pd.formattedAddress != null)
			result = result.append(": " + pd.formattedAddress + "\n");
		
		result = result.append("Number of ratings: " + pd.userRatingsTotal +
				" Rating: " + pd.rating + "\n");
		
		if (pd.website != null)
			result = result.append("More details: " + pd.website);
		
		return result.toString();
	}
	
	private static ArrayList<PlaceDetails> getNewData(String apiKey,
			String input, int radius) {
		try {
			GeoApiContext context = new GeoApiContext.Builder()
					.apiKey(apiKey).build();
			PlacesSearchResult[] results = PlacesApi.findPlaceFromText(context,
					input, FindPlaceFromTextRequest.InputType.TEXT_QUERY)
					.await().candidates;
			
			// Only grab first result, if it exists
			// then look the placeID up.
			if (results.length > 0) {
				String placeId = results[0].placeId;
				PlaceDetails details = PlacesApi.placeDetails(context, placeId)
						.await();
				LatLng location = details.geometry.location;
				
				// ArrayList of all results
				ArrayList<PlaceDetails> allPlaceDetails = new ArrayList<>();
				
				// Search nearby
				PlacesSearchResponse places = PlacesApi
						.nearbySearchQuery(context, location)
						.type(PlaceType.TOURIST_ATTRACTION).radius(radius)
						.await();
				boolean moreResults = false;
				do { 
					for (PlacesSearchResult psr : places.results) {
						placeId = psr.placeId;
						details = PlacesApi.placeDetails(context, placeId)
								.await();
						allPlaceDetails.add(details);
					}
					
					if (places.nextPageToken != null) {
						// Page token not valid right away
						Thread.sleep(4000);
						moreResults = true;
						places = PlacesApi.nearbySearchNextPage(context,
								places.nextPageToken).await();
					}
					else
						moreResults = false;
				} while (moreResults);

				// Sort by reviews descending
				Collections.sort(allPlaceDetails, new ReviewDescComparator());
				
				return allPlaceDetails;
			}
		} catch (IOException | ApiException | InterruptedException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private static ArrayList<PlaceDetails> getDataFromFile(String fileName) {
		ArrayList<PlaceDetails> result = new ArrayList<>();
		
		try {
			FileInputStream fis = new FileInputStream(fileName);
			ObjectInputStream ois = new ObjectInputStream(fis);
			
			try {
				while (true) {
					PlaceDetails pd = (PlaceDetails) ois.readObject();
					result.add(pd);
				}
			}
			catch (EOFException e) {
				// No more PlaceDetail records in the file
				return result;
			}
			catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	private static void writeDataToFile(String fileName, ArrayList<PlaceDetails> allPlaceDetails) {
		// TODO Auto-generated method stub
		try {
			FileOutputStream fos = new FileOutputStream(new File(fileName));
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			
			for (PlaceDetails pd : allPlaceDetails) {
				oos.writeObject(pd);
			}
			
			oos.close();
			fos.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static PlaceCluster[] 
			kMeans(ArrayList<PlaceDetails> destinations, int days) {
		// Create the array of clusters
		Map<PlaceDetails, Integer> placeClusters = new HashMap<>();
		PlaceCluster[] clusters = new PlaceCluster[days];
		
		for (int i = 0; i < days; ++i)
			clusters[i] = new PlaceCluster();
		
		// Copy the destination ArrayList so we can shuffle and
		// delete elements without affecting the original
		ArrayList<PlaceDetails> destCopy = new ArrayList<>(destinations);
		
		// Shuffle the list
		Collections.shuffle(destCopy);
		// Partition the dest_copy array into days
		for (int i = 0; i < days; ++i) {
			double start = i * (destCopy.size() / (double) days);
			double end = (i + 1) * (destCopy.size() / (double) days);
			List<PlaceDetails> randomPartition =
					destCopy.subList((int) start, (int) end);
			for (PlaceDetails pd : randomPartition) {
				clusters[i].addPlace(pd);
				placeClusters.put(pd, i);
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
			
			Set<PlaceDetails> keys = placeClusters.keySet();
			for (PlaceDetails pd : keys) {
				int oldClusterIdx = placeClusters.get(pd);
				LatLng pdLatLng = pd.geometry.location;
				int nearestClusterIdx = 0;
				double nearestClusterDist = EuclideanDistance(pdLatLng, clusters[0].getAvgValue());
				
				for (int i = 1; i < days; ++i) {
					double dist = EuclideanDistance(pdLatLng, clusters[i].getAvgValue());
					
					if (dist < nearestClusterDist) {
						nearestClusterDist = dist;
						nearestClusterIdx = i;
					}
				}
				
				if (oldClusterIdx != nearestClusterIdx) {
					done = false;
					placeClusters.put(pd, nearestClusterIdx);
				}
			}
			
			// Reset the cluster
			for (int i = 0; i < days; ++i) {
				clusters[i].reset();
			}
			
			// Put each place in its new cluster
			for (PlaceDetails pd : keys) {
				int idx = placeClusters.get(pd);
				clusters[idx].addPlace(pd);
			}
			
			// Recalculate averages for each cluster
			for (int i = 0; i < days; ++i) {
				clusters[i].recalculateAverage();
			}
		}
		
		return clusters;
	}

	private static double EuclideanDistance(LatLng l1, LatLng l2) {
		return Math.sqrt(Math.pow(l2.lat - l1.lat, 2.0) + 
				Math.pow(l2.lng - l1.lng, 2.0));
	}
}