# ─── Stage 1: Build the jar ───────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build

# Copy pom first, download deps (cached if pom didn't change)
COPY pom.xml .
RUN mvn dependency:resolve -q 2>&1 | tail -5

# Copy source code and build
COPY src/ src/
RUN mvn clean package -DskipTests -q

# ─── Stage 2: Run the jar ─────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy only the final jar from Stage 1
COPY --from=builder /build/target/bustracker-0.0.1-SNAPSHOT.jar app.jar

# Railway injects PORT automatically. The app reads it via ${PORT} in application.properties.
# No need to hardcode EXPOSE — Railway handles routing.

ENTRYPOINT ["java", "-Xms64m", "-Xmx384m", "-XX:+UseSerialGC", "-jar", "app.jar"]