package fr.minhnn.touristagent;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springaicommunity.a2a.server.executor.DefaultAgentExecutor;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "agent")
@Setter
@Getter
@Slf4j
public class A2AConfiguration {
    private String name;
    private String description;
    private String version;

    @Bean
    public AgentCard agentCard(@Value("${server.port:8080}") int port,
                               @Value("${server.servlet.context-path:/}") String contextPath) {
        return new AgentCard.Builder()
                .name(name)
                .description(description)
                .url("http://localhost:" + port + contextPath)
                .version(version)
                .capabilities(new AgentCapabilities.Builder().streaming(false).build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of(
                        new AgentSkill.Builder()
                                .id("find_destinations")
                                .name("Find Tourist Destinations")
                                .description("Find tourist destinations by types and location")
                                .tags(List.of("tourism", "destinations", "search"))
                                .examples(List.of(
                                        "Find beaches and mountain destinations near me",
                                        "What historical sites are near coordinates 48.8566, 2.3522?"
                                ))
                                .build(),
                        new AgentSkill.Builder()
                                .id("weather_search")
                                .name("Search Weather")
                                .description("Get weather information for a location by coordinates")
                                .tags(List.of("weather", "climate"))
                                .examples(List.of(
                                        "What's the weather in Paris?",
                                        "Get temperature for coordinates 40.7128, -74.0060"
                                ))
                                .build()
                ))
                .protocolVersion("0.3.0")
                .build();
    }

    @Bean
    public AgentExecutor agentExecutor(ChatClient.Builder chatClientBuilder,
                                       WeatherTool weatherTool, JsonMapper jsonMapper) {
        String systemPrompt = """
                **Role:** You are a helpful and knowledgeable tourist agent. Your task is to provide information about tourist attractions, local events, and travel tips to users based on their queries.
                **Instructions:**
                - Recommend relevant tourist attractions, events, and travel tips based on user queries, tourist information, and weather conditions.
                - You MUST use the provided tools to fetch the weather data.
                - DO NOT answer questions that are not related to tourist attractions, and weather.
                - Respond politely "I'm sorry, I don't know the answer to that." if you don't know the answer or the question is not related to tourist attractions, weather, or travel.
                """;

        ChatClient chatClient = chatClientBuilder.clone()
                .defaultSystem(systemPrompt)
                //.defaultAdvisors(
                //        MessageChatMemoryAdvisor.builder(chatMemory).build()
                //)
                .defaultTools(weatherTool)
                .build();

        return new DefaultAgentExecutor(chatClient, (chat, requestContext) -> {
            String userMessage = DefaultAgentExecutor.extractTextFromMessage(requestContext.getMessage());
            log.info("Processing A2A message: {}", userMessage);
            AgentResponse agentResponse = chat.prompt().user(userMessage).call().entity(AgentResponse.class);
            try {
                return jsonMapper.writeValueAsString(agentResponse);
            } catch (Exception e) {
                log.error("Error serializing agent response", e);
                return "{\"error\": \"Failed to construct tourist response\"}";
            }
        });
    }
}
