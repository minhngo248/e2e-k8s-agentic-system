package fr.minhnn.weatheragent;

import java.util.List;

import lombok.extern.log4j.Log4j2;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Log4j2
public class WeatherTools {

    private final RestClient restClient;

    public WeatherTools() {
        this.restClient = RestClient.create();
    }

    public record WeatherResponse(double latitude, double longitude,
                                   double generationtime_ms, int utc_offset_seconds,
                                   String timezone, String timezone_abbreviation,
                                   double elevation, DailyUnits daily_units, Daily daily) {

        public record DailyUnits(String time, String weather_code,
                                 String temperature_2m_max, String temperature_2m_min) {
        }

        public record Daily(List<String> time, List<Integer> weather_code,
                           List<Double> temperature_2m_max, List<Double> temperature_2m_min) {
        }
    }

    @Tool(description = "Get temperature data for a specific location and time range")
    public WeatherResponse getTemperature(@ToolParam(description = "The location latitude") double latitude,
                                          @ToolParam(description = "The location longitude") double longitude,
                                          @ToolParam(description = "The past days to include in the response") int pastDays,
                                          @ToolParam(description = "The forecast days to include in the response") int forecastDays,
                                          @ToolParam(description = "The city name") String city) {

        WeatherResponse response = restClient
                .get()
                .uri("https://api.open-meteo.com/v1/forecast?latitude={latitude}&longitude={longitude}&daily=weather_code,temperature_2m_max,temperature_2m_min&past_days={pastDays}&forecast_days={forecastDays}",
                        latitude, longitude, pastDays, forecastDays)
                .retrieve()
                .body(WeatherResponse.class);

        log.info("Retrieved weather data for city: {}, latitude: {}, longitude: {}, pastDays: {}, forecastDays: {}, days: {}",
                city, latitude, longitude, pastDays, forecastDays,
                response != null && response.daily() != null ? response.daily().time().size() : 0);

        return response;
    }
}
