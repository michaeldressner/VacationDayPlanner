package VacationDayPlanner;

import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import com.google.maps.FindPlaceFromTextRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.ImageResult;
import com.google.maps.PlacesApi;
import com.google.maps.StaticMapsApi;
import com.google.maps.StaticMapsRequest;
import com.google.maps.StaticMapsRequest.Markers;
import com.google.maps.errors.ApiException;
import com.google.maps.model.LatLng;
import com.google.maps.model.PlaceDetails;
import com.google.maps.model.PlaceType;
import com.google.maps.model.PlacesSearchResponse;
import com.google.maps.model.PlacesSearchResult;
import com.google.maps.model.RankBy;
import com.google.maps.model.Size;

public class Main {
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		int mainMenuChoice, radius;
		String apiKey, input;
		ArrayList<PlacesSearchResult> allPlaces;
		GeoApiContext context = null;
		
		printIntroduction();
		
		System.out.println("Choose one of the following options: ");
		System.out.println("1. Run a new dataset from the web (uses a lot of"
				+ " API calls but saves the data in the process)");
		System.out.println("2. Run a dataset from a file");
		//System.out.println("3. Manually enter latitude/longitude and place "
		//		+ "names");
		do {
			System.out.print("Enter your choice: ");
			mainMenuChoice = Integer.parseInt(scanner.nextLine());
		} while (mainMenuChoice < 1 || mainMenuChoice > 2);
		
		if (mainMenuChoice == 1) {
			System.out.print("Enter your API key: ");
			apiKey = scanner.nextLine();
			context = new GeoApiContext.Builder()
					.apiKey(apiKey).build();
			
			System.out.print("Enter a place: ");
			input = scanner.nextLine();
			
			// Note: converts to meters by multiplying by 1609
			System.out.print("Enter the search radius in miles (max 31): ");
			radius = Integer.parseInt(scanner.nextLine()) * 1609;
			
			PlaceType[] placeTypes = {PlaceType.AMUSEMENT_PARK,
					PlaceType.AQUARIUM, PlaceType.ART_GALLERY,
					PlaceType.MUSEUM, PlaceType.PARK,
					PlaceType.STADIUM, PlaceType.TOURIST_ATTRACTION,
					PlaceType.ZOO};
			allPlaces = new ArrayList<>();
			
			System.out.println();
			for (int i = 0; i < placeTypes.length; ++i) {
				System.out.println("Fetching " + placeTypes[i].name() + "s...");
				ArrayList<PlacesSearchResult> results = getNewData(context,
						input, radius, placeTypes[i]);
				allPlaces = mergeDataLists(allPlaces, results);
			}
			
			Collections.sort(allPlaces, new ReviewDescComparator());
			
			promptAndStoreData(scanner, allPlaces);
		}
		else if (mainMenuChoice == 2) {
			System.out.print("Enter a file name: ");
			String fileName = scanner.nextLine();
			input = fileName;
			allPlaces = getDataFromFile(fileName);
		}
		else { // Should not happen
			scanner.close();
			throw new IllegalArgumentException(
					"How did this even happen?");
		}
		
		ArrayList<PlacesSearchResult> destinations =
				new ArrayList<>();
		
		if (mainMenuChoice == 1 || mainMenuChoice == 2) {
			// Ask the user if they would like to go to each
			// places and make a list.
			for (PlacesSearchResult psr : allPlaces) {
				System.out.println();
				System.out.println("Would you like to go to the following"
						+ " place?");
				System.out.println(placesSearchResultToString(psr));
				
				String yesNoQuit;
				do {
						System.out.print("(y/n or q to stop entering"
								+ " destinations): ");
						yesNoQuit = scanner.nextLine();
				} while (!yesNoQuit.equalsIgnoreCase("y") &&
						!yesNoQuit.equalsIgnoreCase("n") &&
						!yesNoQuit.equalsIgnoreCase("q"));
				
				if (yesNoQuit.equalsIgnoreCase("y")) {
					destinations.add(psr);
					System.out.println(psr.name + " added to destinations");
				}
				else if (yesNoQuit.equalsIgnoreCase("q")) {
					System.out.println();
					break;
				}
				System.out.println();
			}
		}
		
		int days;
		do {
			System.out.print("Enter the number of days on vacation (must be less"
				+ " or equal to " + destinations.size() + "): ");
			days = Integer.parseInt(scanner.nextLine());
		} while (days > destinations.size() || days < 1);
		
		PlaceCluster[] clusters = PlaceCluster.kMeans(destinations, days);
		
		for (int i = 0; i < clusters.length; ++i) {
			System.out.println();
			System.out.println("Day " + (i + 1) + ":");
			
			for (PlacesSearchResult psr : clusters[i].getPlaces()) {
				System.out.println(psr.name);
			}
		}
		
		System.out.println();
		boolean genImg;
		String genChoice;
		do {
			System.out.print("Would you like to generate an image file?"
					+ " (y/n): ");
			genChoice = scanner.nextLine();
		} while (!genChoice.equalsIgnoreCase("y") &&
				!genChoice.equalsIgnoreCase("n"));
		
		genImg = genChoice.equalsIgnoreCase("y") ? true : false;
		
		if (genImg) {
			if (context == null) {
				System.out.print("Enter your API key: ");
				apiKey = scanner.nextLine();
				context = new GeoApiContext.Builder()
						.apiKey(apiKey).build();
			}
			
			// Lets say 500 x 500 for now
			Size size = new Size(500, 500);
			
			// Add the markers
			ArrayList<Markers> markerGroups = new ArrayList<>();
			for (int i = 0; i < clusters.length; ++i) {
				ArrayList<PlacesSearchResult> places = clusters[i].getPlaces();
				Markers markers = new Markers();
				
				// Random color code
				Random random = new Random();
		        int nextInt = random.nextInt(0xffffff + 1);
		        String color = String.format("0x%06x", nextInt);
		        
				markers.color(color);
				
				for (PlacesSearchResult psr : places) {
					markers.addLocation(psr.geometry.location);
				}
				
				markerGroups.add(markers);
			}
			
			StaticMapsRequest request = StaticMapsApi.newRequest(context,
					size);
			
			for (Markers markers : markerGroups) {
				request = request.markers(markers);
			}
			
			try {
				ImageResult imgResult = request.await();
				System.out.print("Enter a file name for the image: ");
				String fileName = scanner.nextLine();
				FileOutputStream fos = new FileOutputStream(fileName + ".png");
				OutputStream out = new BufferedOutputStream(fos);
				out.write(imgResult.imageData);
				fos.close();
				out.close();
			} catch (ApiException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		scanner.close();
		if (context != null)
			context.shutdown();
	}

	private static ArrayList<PlacesSearchResult> mergeDataLists(
			ArrayList<PlacesSearchResult> l1,
			ArrayList<PlacesSearchResult> l2) {
			Set<PlacesSearchResult> mergedSet = new TreeSet<>(
				new Comparator<PlacesSearchResult>() {
					@Override
					public int compare(PlacesSearchResult psr1,
							PlacesSearchResult psr2) {
						return psr1.placeId.compareTo(psr2.placeId);
					}
					
				});
		ArrayList<PlacesSearchResult> result = new ArrayList<>();
		
		mergedSet.addAll(l1);
		mergedSet.addAll(l2);
		
		result.addAll(mergedSet);
		return result;
	}

	private static class ReviewDescComparator
			implements Comparator<PlacesSearchResult> {
		@Override
		public int compare(PlacesSearchResult pd1, PlacesSearchResult pd2) {
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
				+ "1. This program groups the locations\n"
				+ "   that you would like to go visit into clusters of places\n"
				+ "   that are close to one another. In some cases, you might\n"
				+ "   find there are too many things on the list to reasonably\n"
				+ "   fit into an actual day, so it is up to you to make a\n"
				+ "   reasonable day schedule from the output of the program.\n"
				+ "   Although this is not a scheduling app perse, it\n"
				+ "   might be at some point in the future. It still is useful if\n"
				+ "   used appropriately.\n";
		
		System.out.print(welcome);
	}
	
	private static String placesSearchResultToString(PlacesSearchResult psr) {
		// Convert PlacesSearchResult to string
		StringBuilder result = new StringBuilder();
		// Add place name
		result = result.append(psr.name + "\n");
		
		// Add address
		if (psr.formattedAddress != null)
			result = result.append(": " + psr.formattedAddress + "\n");
		
		result = result.append("Number of ratings: " + psr.userRatingsTotal +
				" Rating: " + psr.rating + "\n");

		return result.toString();
	}
	
	private static ArrayList<PlacesSearchResult> getNewData(
			GeoApiContext context, String input, int radius, PlaceType type) {
		try {
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
				ArrayList<PlacesSearchResult> allPlaces = new ArrayList<>();
				
				// Search nearby
				PlacesSearchResponse places = PlacesApi
						.nearbySearchQuery(context, location)
						.type(type).radius(radius).rankby(RankBy.PROMINENCE)
						.await();
				boolean moreResults = false;
				do { 
					for (PlacesSearchResult psr : places.results) {
						allPlaces.add(psr);
					}
					
					if (places.nextPageToken != null) {
						// Page token not valid right away
						Thread.sleep(2000);
						moreResults = true;
						places = PlacesApi.nearbySearchNextPage(context,
								places.nextPageToken).await();
					}
					else
						moreResults = false;
				} while (moreResults);
				
				return allPlaces;
			}
		} catch (IOException | ApiException | InterruptedException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private static ArrayList<PlacesSearchResult> getDataFromFile(String fileName) {
		ArrayList<PlacesSearchResult> result = new ArrayList<>();
		
		try {
			FileInputStream fis = new FileInputStream(fileName);
			ObjectInputStream ois = new ObjectInputStream(fis);
			
			try {
				while (true) {
					PlacesSearchResult psr = (PlacesSearchResult) ois.readObject();
					result.add(psr);
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
	
	/**
	 * Returns file name
	 * @param allPlaces 
	 */
	private static void promptAndStoreData(Scanner scanner,
			ArrayList<PlacesSearchResult> allPlaces) {
		boolean storeData;
		String storeChoice, fileName;
		do {
			System.out.print("Would you like to store this data to a file"
					+ " (y/n): ");
			storeChoice = scanner.nextLine();
		} while (!storeChoice.equalsIgnoreCase("y") &&
				!storeChoice.equalsIgnoreCase("n"));
		
		storeData = storeChoice.equalsIgnoreCase("y") ? true : false;
		if (storeData) {
			System.out.print("Enter filename: ");
			fileName = scanner.nextLine();
			
			writeDataToFile(fileName, allPlaces);
		}
	}
	
	private static void writeDataToFile(String fileName,
			ArrayList<PlacesSearchResult> allPlaces) {
		try {
			FileOutputStream fos = new FileOutputStream(new File(fileName));
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			
			for (PlacesSearchResult psr : allPlaces) {
				oos.writeObject(psr);
			}
			
			oos.close();
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}