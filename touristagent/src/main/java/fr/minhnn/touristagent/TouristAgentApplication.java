package fr.minhnn.touristagent;

import fr.minhnn.touristagent.config.TouristApiProperties;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springaicommunity.a2a.server.executor.DefaultAgentExecutor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

import java.util.List;

@SpringBootApplication
@Log4j2
@RequiredArgsConstructor
public class TouristAgentApplication {
    private static final String SYSTEM_INSTRUCTION = """
            You are a tourist agent. Your task is to recommend tourist destinations to users based on their preferences and location.
            You have access to a database of tourist destinations, which you can query using the following API:
            
            GET /api/v1/destinations?types={types}&latitude={latitude}&longitude={longitude}&radiusKm={radiusKm}
            
            The API returns a list of tourist destinations that match the specified types and are located within the specified radius from the given latitude and longitude.
            
            When a user asks for recommendations, you should ask them about their preferences (e.g., types of destinations they like) and their current location (latitude and longitude).
            Then, you should query the API with the appropriate parameters and return a list of recommended tourist destinations to the user.
            """;

    private final RestClient restClient;
    private final TouristApiProperties touristApiProperties;

    public static void main(String[] args) {
        SpringApplication.run(TouristAgentApplication.class, args);
    }

    @Bean
    public AgentCard agentCard(@Value("${server.port:8080}") int port,
                               @Value("${server.servlet.context-path:/}") String contextPath) {

        return new AgentCard.Builder().name("Tourist Agent")
                .description("An agent that provides tourist destination recommendations based on user preferences and location.")
                .url("http://localhost:" + port + contextPath)
                .version("1.0.0")
                .capabilities(new AgentCapabilities.Builder().streaming(false).pushNotifications(true).build())
                .defaultInputModes(List.of("text", "text/plain"))
                .defaultOutputModes(List.of("text", "text/plain"))
                .skills(List.of(new AgentSkill.Builder().id("tourist_destination_search")
                        .name("Tourist Destination Search")
                        .description("Search for tourist destinations based on user preferences and location.")
                        .tags(List.of("tourism", "destination", "recommendation"))
                        .examples(List.of("Find me some beach destinations near me.", "What are some good tourist spots in Paris?"))
                        .build()))
                .protocolVersion("0.3.0")
                .build();
    }

    @Bean
    public AgentExecutor agentExecutor(ChatClient.Builder chatClientBuilder) {

        ChatClient chatClient = chatClientBuilder.clone()
                .defaultSystem(SYSTEM_INSTRUCTION)
                .build();

        return new DefaultAgentExecutor(chatClient, (chat, requestContext) -> {
            String userMessage = DefaultAgentExecutor.extractTextFromMessage(requestContext.getMessage());
            return chat.prompt().tools(new TouristTool(restClient, touristApiProperties)).user(userMessage).call().content();
        });
    }
}
