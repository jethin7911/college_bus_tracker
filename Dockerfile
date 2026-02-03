# ─── Stage 1: Build the jar using a Maven image ───────────────────

FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Copy pom first and download dependencies (improves build speed via caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and package the application
COPY src/ src/
RUN mvn clean package -DskipTests -q

# ─── Stage 2: Run the jar (The tiny production image) ─────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built jar from the builder stage
COPY --from=builder /build/target/bustracker-0.0.1-SNAPSHOT.jar app.jar

# Standard PORT variable support for cloud platforms
ENV PORT=8080
EXPOSE 8080

# Optimized Java startup flags
ENTRYPOINT ["java", "-Xms64m", "-Xmx384m", "-XX:+UseSerialGC", "-jar", "app.jar"]