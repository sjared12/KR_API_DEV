# Multi-stage build for admin-service
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

# Copy parent POM first and install it
COPY pom.xml ./
RUN mvn install -N

# Copy admin-service module
COPY admin-service ./admin-service

# Build the admin-service module
WORKDIR /workspace/admin-service
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache tzdata curl

WORKDIR /appworkspace

# Copy the built jar from build stage
COPY --from=build /build/admin-service/target/admin-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD curl -f http://localhost:8080/api/services || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
