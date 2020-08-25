package Vacation.Day.Planner;

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
import java.util.Scanner;

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
				break;
			}
			System.out.println();
		}
	}

	private static class ReviewDescComparator
			implements Comparator<PlaceDetails> {
		@Override
		public int compare(PlaceDetails pd1, PlaceDetails pd2) {
			return pd2.userRatingsTotal - pd1.userRatingsTotal;
		}
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
}