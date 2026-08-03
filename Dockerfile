# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------
# Build stage
#
# Uses the maven image rather than ./mvnw on purpose: the wrapper script is
# checked out with CRLF line endings on Windows, which a Linux container
# rejects with "no such file or directory".
# ---------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Dependencies resolve in their own layer so source edits do not re-download
# the world on every build.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
# Tests need a database; CI runs them separately against a real Postgres.
RUN mvn -B clean package -DskipTests

# ---------------------------------------------------------------------
# Runtime stage
# ---------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

# Running as root inside a container means a container escape starts as root.
RUN addgroup -S freshlink && adduser -S freshlink -G freshlink

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
RUN chown freshlink:freshlink /app/app.jar

USER freshlink
EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75"

# Uses the readiness probe, so the container is only marked healthy once the
# datasource and migrations are actually up.
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
