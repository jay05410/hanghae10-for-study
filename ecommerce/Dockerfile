# Build stage (optional - JAR already built in CI)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy JAR file
COPY build/libs/*.jar app.jar

# JVM options for container environment
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
