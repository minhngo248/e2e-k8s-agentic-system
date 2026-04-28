package fr.minhnn.touristapi.infra;

import com.google.maps.model.LatLng;
import fr.minhnn.touristapi.config.GoogleMapProperties;
import fr.minhnn.touristapi.destination.Destination;
import fr.minhnn.touristapi.destination.GoogleMapService;
import fr.minhnn.touristapi.utils.GoogleMapApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoogleMapServiceImpl implements GoogleMapService {
    private final GoogleMapProperties properties;
    @Override
    public Destination.Coordinates geocode(String text) {
        LatLng latLng = GoogleMapApi.getLatLng(properties.getKey(), text);
        return new Destination.Coordinates(latLng.lat, latLng.lng);
    }
}
