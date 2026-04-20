# Multi-stage build
FROM gradle:8.6-jdk17 AS builder

WORKDIR /app
COPY build.gradle settings.gradle gradle.properties* ./
COPY gradle ./gradle
COPY src ./src
RUN gradle build -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose port (documentación, no afecta runtime)
EXPOSE 8081

# ✅ Healthcheck con puerto dinámico
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:${PORT:-8080}/actuator/health || exit 1

# ✅ ENTRYPOINT con expansión de $PORT para Spring Boot
ENTRYPOINT exec java -jar -Dserver.port=$PORT app.jar