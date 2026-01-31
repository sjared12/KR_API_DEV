# syntax=docker/dockerfile:1

# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN apk add --no-cache maven && \
    mvn dependency:go-offline -DskipTests

COPY src ./src
RUN mvn clean package -DskipTests -q

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ENV CONFIG_PATH=/data/config.json
ENV PORT=8080
RUN mkdir -p /data && \
    adduser -D -u 1000 appuser && \
    chown -R appuser:appuser /data

COPY --from=build /app/target/*.jar app.jar
RUN chown appuser:appuser /app/app.jar

USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]