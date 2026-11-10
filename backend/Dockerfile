# Build context is the repository root (see docker-compose.yml) because the pom reads ../database/migrations.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY backend ./backend
COPY database ./database
WORKDIR /app/backend
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
    && useradd -m appuser
WORKDIR /app
COPY --from=build /app/backend/target/enterpriseops-backend-0.1.0.jar app.jar
USER appuser
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=5 CMD curl -fs http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
