package VacationDayPlanner;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import java.util.Scanner;

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
		ArrayList<Place> allPlaces = new ArrayList<>();
		ArrayList<Place> destinations = new ArrayList<>();
		GeoApiContext context = null;
		
		printIntroduction();
		
		System.out.println("Choose one of the following options: ");
		System.out.println("1. Run a new dataset from the web (uses a lot of"
				+ " API calls but saves the data in the process)");
		System.out.println("2. Run a dataset from a file");
		System.out.println("3. Manually enter latitude/longitude and place "
				+ "names (no API key required)");
		
		int mainMenuChoice;
		do {
			System.out.print("Enter your choice: ");
			mainMenuChoice = Integer.parseInt(scanner.nextLine());
		} while (mainMenuChoice < 1 || mainMenuChoice > 2);
		
		if (mainMenuChoice == 1) {
			String apiKey = stringPrompt(scanner, "Enter your API key: ");
			context = new GeoApiContext.Builder()
					.apiKey(apiKey).build();
			
			System.out.print("Enter a place: ");
			String input = scanner.nextLine();
			
			// Note: converts to meters by multiplying by 1609
			System.out.print("Enter the search radius in miles (max 31): ");
			int radius = Integer.parseInt(scanner.nextLine()) * 1609;
			
			PlaceType[] placeTypes = {PlaceType.AMUSEMENT_PARK,
					PlaceType.AQUARIUM, PlaceType.ART_GALLERY,
					PlaceType.MUSEUM, PlaceType.PARK,
					PlaceType.STADIUM, PlaceType.TOURIST_ATTRACTION,
					PlaceType.ZOO};
			
			System.out.println();
			for (int i = 0; i < placeTypes.length; ++i) {
				System.out.println("Fetching " + placeTypes[i].name() + "s...");
				ArrayList<Place> results = getNewData(context, input, radius,
						placeTypes[i]);
				allPlaces = Place.mergeDataLists(allPlaces, results);
			}
			
			Collections.sort(allPlaces, new ReviewDescComparator());
			
			String fileName;
			boolean storeData = yesNoPrompt(scanner,
					"Would you like to store this data to a file?");
			
			if (storeData) {
				System.out.print("Enter filename: ");
				fileName = scanner.nextLine();
				
				Place.writeDataToFile(fileName, allPlaces);
			}
		}
		else if (mainMenuChoice == 2) {
			String fileName = stringPrompt(scanner, "Enter a file name: ");
			allPlaces = Place.getDataFromFile(fileName);
		}
		else if (mainMenuChoice == 3) {
			boolean quit;
			do {
				String placeName = stringPrompt(scanner,
						"Enter the place name (or \"quit\"): ");
				if (placeName.equalsIgnoreCase("quit")) break;
				
				
			} while (true);
		}
		else { // Should not happen
			scanner.close();
			throw new IllegalArgumentException(
					"How did this even happen?");
		}
		
		if (mainMenuChoice == 1 || mainMenuChoice == 2) {
			// Ask the user if they would like to go to each
			// places and make a list.
			for (Place p : allPlaces) {
				System.out.println();
				System.out.println("Would you like to go to the following"
						+ " place?");
				System.out.println(p.toString());
				
				String yesNoQuit;
				do {
						System.out.print("(y/n or q to stop entering"
								+ " destinations): ");
						yesNoQuit = scanner.nextLine();
				} while (!yesNoQuit.equalsIgnoreCase("y") &&
						!yesNoQuit.equalsIgnoreCase("n") &&
						!yesNoQuit.equalsIgnoreCase("q"));
				
				if (yesNoQuit.equalsIgnoreCase("y")) {
					destinations.add(p);
					System.out.println(p.getName() + " added to destinations");
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
			
			for (Place p : clusters[i].getPlaces()) {
				System.out.println(p.getName());
			}
		}
		
		System.out.println();
		boolean genImg = yesNoPrompt(scanner,
				"Would you like to generate an image file?");
		
		if (genImg) {
			if (context == null) {
				System.out.print("Enter your API key: ");
				String apiKey = scanner.nextLine();
				context = new GeoApiContext.Builder()
						.apiKey(apiKey).build();
			}
			
			// Lets say 700 x 700 for now
			Size size = new Size(700, 700);
			
			// Add the markers
			ArrayList<Markers> markerGroups = new ArrayList<>();
			for (int i = 0; i < clusters.length; ++i) {
				ArrayList<Place> places = clusters[i].getPlaces();
				Markers markers = new Markers();
				
				// Random color code
				Random random = new Random();
		        int nextInt = random.nextInt(0xffffff + 1);
		        String color = String.format("0x%06x", nextInt);
		        
				markers.color(color);
				
				for (Place p : places) {
					Location loc = p.getLocation();
					markers.addLocation(new LatLng(loc.getLat(), loc.getLng()));
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
	
	private static ArrayList<Place> getNewData(
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
				
				ArrayList<Place> placeList = new ArrayList<>();
				
				for (PlacesSearchResult psr : allPlaces) {
					placeList.add(new Place(psr.placeId, psr.name,
							psr.geometry.location.lat,
							psr.geometry.location.lng, psr.userRatingsTotal,
							psr.rating, psr.vicinity));
				}
				
				return placeList;
			}
		} catch (IOException | ApiException | InterruptedException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	private static class ReviewDescComparator implements Comparator<Place> {
		@Override
		public int compare(Place pd1, Place pd2) {
			return pd2.getNumRatings() - pd1.getNumRatings();
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
	
	private static boolean yesNoPrompt(Scanner scanner, String prompt) {
		String choice;
		do {
			System.out.print(prompt + " (y/n): ");
			choice = scanner.nextLine();
		} while (!choice.equalsIgnoreCase("y") &&
				!choice.equalsIgnoreCase("n"));
		
		return choice.equalsIgnoreCase("y") ? true : false;
	}
	
	private static String stringPrompt(Scanner scanner, String prompt) {
		String input;
		do {
			System.out.print(prompt);
			input = scanner.nextLine();
		} while (input.equals(""));
		
		return input;
	}
}