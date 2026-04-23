package fr.minhnn.orchestrator;

import lombok.extern.log4j.Log4j2;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

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
            * **Response Synthesis:** After receiving responses from the remote agents, synthesize the information into a clear and concise answer for the user, ensuring that all relevant details are included.
            
            **Agent Router:**
            
            Available Agents:
            %s
            """;

    @Bean
    public ChatClient routingChatClient(ChatClient.Builder chatClientBuilder,
                                        RemoteAgentConnections remoteAgentConnections) {

        String systemPrompt = String.format(ROUTING_SYSTEM_PROMPT, remoteAgentConnections.getAgentDescriptions());

        log.info("Initializing routing ChatClient with agents: {}", remoteAgentConnections.getAgentNames());

        return chatClientBuilder.defaultSystem(systemPrompt).defaultTools(remoteAgentConnections).build();
    }


}
