package fr.minhnn.orchestrator;

import lombok.extern.log4j.Log4j2;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.PostgresChatMemoryRepositoryDialect;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@SpringBootApplication
@Log4j2
public class OrchestratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrchestratorApplication.class, args);
    }

    private static final String ROUTING_SYSTEM_PROMPT = """
            **Role:** You are an expert Routing Delegator. Your primary function is to accurately delegate user inquiries regarding weather or tourist informations to the appropriate specialized remote agents,
            and then synthesize the responses from these agents into a coherent and concise answer for the user.
            
            **Core Directives:**
            
            * **Task Delegation:** Utilize the `sendMessage` function to assign actionable tasks to remote agents.
            * **Contraints**:
                * Only receive and process user inquiries related to weather or tourist information.
                * Otherwise, do not route user inquiries and politely inform the user that you can only assist with weather or tourist information and ask them to rephrase their question accordingly.
                * Ensure that the delegation is clear and concise, providing necessary context for the remote agent to understand the task.
                * If the user inquiry is ambiguous or could be relevant to multiple agents, use your judgment to determine the most appropriate agent based on the content of the inquiry and the capabilities of the agents.
                * DO NOT ask the user for clarification, simply make the best decision based on the information available.
            * **No Pre-Tool Text:** When you need to call a tool, call it IMMEDIATELY without outputting any text beforehand. Do NOT say things like "Let me fetch that for you" or "I'm checking with the agent". Just call the tool directly.
            * **Response Synthesis:** After receiving responses from the remote agents, synthesize the information into a clear and concise answer in a Markdown human-readable format for the user.
            
            **Agent Router:**
            
            Available Agents:
            %s
            """;

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public ChatMemoryRepository chatMemoryRepository(JdbcTemplate jdbcTemplate) {
        return JdbcChatMemoryRepository.builder()
                .jdbcTemplate(jdbcTemplate)
                .dialect(new PostgresChatMemoryRepositoryDialect())
                .build();
    }

    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
            .chatMemoryRepository(chatMemoryRepository)
            .maxMessages(10)
            .build();
    }

    @Bean
    public ChatClient routingChatClient(ChatClient.Builder chatClientBuilder,
                                        RemoteAgentConnections remoteAgentConnections,
                                        ChatMemory chatMemory) {

        String systemPrompt = String.format(ROUTING_SYSTEM_PROMPT, remoteAgentConnections.getAgentDescriptions());

        log.info("Initializing routing ChatClient with agents: {}", remoteAgentConnections.getAgentNames());

        return chatClientBuilder
                .defaultSystem(systemPrompt)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(remoteAgentConnections)
                .build();
    }


}
