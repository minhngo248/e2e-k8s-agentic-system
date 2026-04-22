package fr.minhnn.touristapi.utils;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import fr.minhnn.touristapi.exceptions.BadRequestException;

public class GoogleMapApi {
    private static final Integer defaultIndex = 0;
    public static LatLng getLatLng(String apiKey, String address) {
        GeoApiContext apiContext = new GeoApiContext.Builder().apiKey(apiKey).build();
        try {
            GeocodingResult[] results = GeocodingApi.geocode(apiContext, address).await();

            return results[defaultIndex].geometry.location;
        } catch (Exception ex) {
            throw new BadRequestException("Cannot get LatLng from address: " + address);
        }
    }
}
