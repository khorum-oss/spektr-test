# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Copy module build files
COPY app/build.gradle.kts app/
COPY dsl/build.gradle.kts dsl/

# Create stub modules required by settings.gradle.kts (we only build app)
RUN mkdir -p examples examples/common examples/test-common examples/ghost-book examples/ghost-book/test-api \
    examples/haunted-house-tracker examples/haunted-house-tracker/test-api && \
    printf 'tasks.bootJar { enabled = false }\ntasks.jar { enabled = true }\n' > examples/build.gradle.kts && \
    printf 'tasks.bootJar { enabled = false }\ntasks.jar { enabled = true }\n' > examples/common/build.gradle.kts && \
    printf 'tasks.bootJar { enabled = false }\ntasks.jar { enabled = true }\n' > examples/test-common/build.gradle.kts && \
    printf 'tasks.bootJar { enabled = false }\n' > examples/ghost-book/build.gradle.kts && \
    touch examples/ghost-book/test-api/build.gradle.kts && \
    printf 'tasks.bootJar { enabled = false }\n' > examples/haunted-house-tracker/build.gradle.kts && \
    touch examples/haunted-house-tracker/test-api/build.gradle.kts

# Download dependencies (cached layer)
RUN chmod +x gradlew && ./gradlew :app:dependencies --no-daemon || true

# Copy source code
COPY dsl/src dsl/src
COPY app/src app/src

# Build the application
RUN ./gradlew :app:bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create directory for endpoint JARs
RUN mkdir -p /app/endpoint-jars

# Copy the built application
COPY --from=build /app/app/build/libs/*.jar app.jar

# Expose port
EXPOSE 8080

# Set default environment variables
ENV ENDPOINT_JARS_DIR=/app/endpoint-jars
ENV JAVA_OPTS=""

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --endpoint-jars.dir=$ENDPOINT_JARS_DIR"]