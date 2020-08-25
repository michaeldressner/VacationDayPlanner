package PlaceFileGenerator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Scanner;

import com.google.maps.FindPlaceFromTextRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PlacesApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.LatLng;
import com.google.maps.model.PlaceDetails;
import com.google.maps.model.PlacesSearchResult;

public class GeneratePlaceFile {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		String input;
		
		System.out.print("Enter your API key: ");
		String apiKey = scanner.nextLine();
		GeoApiContext context = new GeoApiContext.Builder()
				.apiKey(apiKey).build();
		
		System.out.print("Enter a file name: ");
		String fileName = scanner.nextLine();
		try {
			FileOutputStream fos = new FileOutputStream(new File(fileName));
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			
			do {
				System.out.print("Enter a place to add to the file (q to quit): ");
				input = scanner.nextLine();
				
				if (!input.equalsIgnoreCase("q")) {
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
							oos.writeObject(details);
							System.out.println(details.name +
									" has been written to the place file");
						}
						else {
							System.out.println("No match found");
						}
						
						System.out.println();
					} catch (ApiException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} while (!input.equalsIgnoreCase("q"));
			
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
