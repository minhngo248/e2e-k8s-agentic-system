FROM debian:bookworm-slim

# Install required runtime dependencies for native image
# and create non-root user for security
RUN apt-get update && apt-get install -y --no-install-recommends \
    libstdc++6 \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -g 1001 appgroup \
    && useradd -u 1001 -g appgroup -s /bin/bash appuser

# Set working directory
WORKDIR /app

# Copy the native executable from build stage
COPY target/mcpservertourist /app/application

# Create export directory with set permissions
RUN chown -R appuser:appgroup /app \
    && chmod +x /app/application

# Switch to non-root user
USER appuser

# Run the native application
ENTRYPOINT ["./application", "--spring.profiles.active=prod"]