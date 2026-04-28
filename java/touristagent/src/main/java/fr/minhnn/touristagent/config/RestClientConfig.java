package fr.minhnn.touristagent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    private final TouristApiProperties touristApiProperties;

    public RestClientConfig(TouristApiProperties touristApiProperties) {
        this.touristApiProperties = touristApiProperties;
    }

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .baseUrl(touristApiProperties.getUrl())
                .build();
    }
}
