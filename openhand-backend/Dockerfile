# ---------- Stage 1: Build the Spring Boot jar ----------
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts ./build.gradle.kts
COPY settings.gradle.kts ./settings.gradle.kts
# if you use plain build.gradle/settings.gradle, copy those instead:
# COPY build.gradle ./build.gradle
# COPY settings.gradle ./settings.gradle

# Copy source code
COPY src ./src

# Give execute permission to gradlew (Linux containers)
RUN chmod +x gradlew

# Build the jar
RUN ./gradlew clean bootJar --no-daemon

# ---------- Stage 2: Run the jar ----------
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy jar from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]