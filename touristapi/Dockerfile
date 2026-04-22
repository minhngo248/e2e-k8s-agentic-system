# Define version variable
ARG VERSION=0.0.1-SNAPSHOT

# Stage 1: Build stage
FROM maven:3.9-eclipse-temurin-25 AS builder

# Set version argument in build stage
ARG VERSION

# Set working directory
WORKDIR /build

# Copy Maven wrapper and pom.xml first for better layer caching
COPY mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY pom.xml ./

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Runtime stage
FROM eclipse-temurin:25-jre

# Define label
LABEL org.opencontainers.image.source=https://github.com/minhngo248/e2e-k8s-agentic-system

# Set version argument in runtime stage
ARG VERSION

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
COPY --from=builder /build/target/touristapi-${VERSION}.jar /app/application.jar

# Create export directory with set permissions
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Run the application
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
