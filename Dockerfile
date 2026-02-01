# Multi-stage build for EventTicket System
# Using Java 25 with Alpine Linux for minimal image size
# Alpine images are ~10x smaller than standard Debian-based images

FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /app

# Install Maven (Alpine uses apk package manager)
RUN apk add --no-cache maven

# Copy Maven configuration
COPY pom.xml .

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application with Java 25
RUN mvn clean package -DskipTests

# Runtime stage - Optimized Alpine image (~50MB vs ~200MB)
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Install curl for healthcheck (Alpine minimal image doesn't include it)
RUN apk add --no-cache curl

# Create non-root user for security (Alpine uses addgroup/adduser)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy compiled JAR from builder
COPY --from=builder /app/target/*.jar app.jar

# JVM configuration for containers with Virtual Threads enabled
# Note: ZGenerational was removed in Java 24+, ZGC is generational by default
ENV JAVA_OPTS="\
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseZGC \
    -Djava.security.egd=file:/dev/./urandom"

# Expose application port
EXPOSE 8080

# Health check using curl
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Entry point with Virtual Threads support
# For hot reload: Add spring.config.location to read from mounted volume
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.config.additional-location=file:/app/resources/"]
