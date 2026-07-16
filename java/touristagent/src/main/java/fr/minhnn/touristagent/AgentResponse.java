package fr.minhnn.touristagent;

import java.util.List;

record DestinationImages(
        String destinationId,
        List<String> imageUrls
) {}

public record AgentResponse(
        String answer,
        List<DestinationImages> images
) {}
