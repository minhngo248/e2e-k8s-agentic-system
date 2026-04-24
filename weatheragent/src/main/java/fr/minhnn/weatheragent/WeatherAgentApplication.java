package fr.minhnn.weatheragent;

import java.util.List;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.spec.*;
import org.springaicommunity.a2a.server.executor.DefaultAgentExecutor;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class WeatherAgentApplication {

    private static final String WEATHER_SYSTEM_INSTRUCTION = """
			You are a specialized weather forecast assistant.
			Your primary function is to utilize the provided tools to retrieve and relay weather information in response to user queries.
			You must rely exclusively on these tools for data and refrain from inventing information.
			Ensure that all responses are in Markdown human-readable format. If the user query is ambiguous, use your best judgment
			to determine the most likely intent and provide a relevant response based on the available tools.
			""";

    public static void main(String[] args) {
        SpringApplication.run(WeatherAgentApplication.class, args);
    }

    @Bean
    public AgentCard agentCard(@Value("${server.port:8080}") int port,
                               @Value("${server.servlet.context-path:/}") String contextPath) {

        return new AgentCard.Builder().name("Weather Agent")
                .description("Helps with weather")
                .url("http://localhost:" + port + contextPath)
                .version("1.0.0")
                .capabilities(new AgentCapabilities.Builder()
                        .streaming(false)
                        .build()
                )
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of(new AgentSkill.Builder().id("weather_search")
                        .name("Search weather")
                        .description("Helps with weather in city, or states")
                        .tags(List.of("weather"))
                        .examples(List.of("weather in LA, CA"))
                        .build()))
                .protocolVersion("0.3.0")
                .build();
    }

    @Bean
    public AgentExecutor agentExecutor(ChatClient.Builder chatClientBuilder, WeatherTools weatherTools) {

        ChatClient chatClient = chatClientBuilder.clone()
                .defaultSystem(WEATHER_SYSTEM_INSTRUCTION)
                .defaultTools(weatherTools)
                .build();

        return new DefaultAgentExecutor(chatClient, (chat, requestContext) -> {
            String userMessage = DefaultAgentExecutor.extractTextFromMessage(requestContext.getMessage());
            return chat.prompt().user(userMessage).call().content();
        });
    }

}