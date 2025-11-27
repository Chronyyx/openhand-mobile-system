FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts ./build.gradle.kts
COPY settings.gradle.kts ./settings.gradle.kts


# Copy source code
COPY src ./src

# Give execute permission to gradlew (Linux containers)
RUN chmod +x gradlew

# Build the jar
RUN ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy jar from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]