FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Copy pom and source
COPY pom.xml .
COPY src src

# Install Maven and build
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests && \
    rm -rf /root/.m2

# Create final image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy jar from builder
COPY --from=0 /app/target/kr-admin-*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-Djava.awt.headless=true", "-Xmx512m", "-Xms256m", "-jar", "app.jar"]
