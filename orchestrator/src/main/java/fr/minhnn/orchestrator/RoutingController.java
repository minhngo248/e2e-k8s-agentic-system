package fr.minhnn.orchestrator;

import lombok.extern.log4j.Log4j2;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@Log4j2
public class RoutingController {

    private final ChatClient chatClient;

    public RoutingController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(@RequestBody Map<String, String> request) {
        String userMessage = request.get("message");
        log.info("Received user message: {}", userMessage);

        Flux<String> contentFlux = this.chatClient.prompt()
                .user(userMessage)
                .stream()
                .content()
                .cache();

        return contentFlux
                .map(chunk -> ServerSentEvent.<String>builder().data(chunk).build())
                .doOnComplete(() -> log.info("Streaming completed for message: {}", userMessage))
                .doOnError(error -> log.error("Error during streaming: {}", error.getMessage()));
    }

}
