package fr.minhnn.orchestrator;

import lombok.extern.log4j.Log4j2;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Log4j2
public class RoutingController {

    private final ChatClient chatClient;

    public RoutingController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping("/chat")
    public String chat(@RequestBody String userMessage) {
        log.info("Received user message: {}", userMessage);

        String response = this.chatClient.prompt().user(userMessage).call().content();

        log.info("Response: {}", response);
        return response;
    }

}
