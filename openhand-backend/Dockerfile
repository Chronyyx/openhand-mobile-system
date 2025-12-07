FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts ./build.gradle.kts
COPY settings.gradle.kts ./settings.gradle.kts

# Fix windows line endings for gradlew
RUN sed -i 's/\r$//' gradlew

# Give execute permission to gradlew
RUN chmod +x gradlew

# Download dependencies (cached if gradle files don't change)
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src ./src

# Build the jar
RUN ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy jar from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]