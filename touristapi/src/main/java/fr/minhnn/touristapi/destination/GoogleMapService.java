package fr.minhnn.touristapi.destination;

public interface GoogleMapService {
    /**
     * Get latitude and longitude for a given destination.
     * @param text Address or place name
     *
     */
     Destination.Coordinates geocode(String text);
}
