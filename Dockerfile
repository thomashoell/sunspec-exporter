# syntax=docker/dockerfile:1

# The jar is platform independent: build it once on the build platform, so multi-arch images don't need QEMU.
# Pin the images to their digests with .github/scripts/pin-images.py; Dependabot keeps tags and digests up to date.
FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-21 AS builder

# set by the release workflow (semantic-release), shown in /actuator/info
ARG VERSION=0.0.0-SNAPSHOT

WORKDIR /build
COPY pom.xml ./
COPY src ./src
# tests run in CI before the image is built
RUN --mount=type=cache,target=/root/.m2 \
    mvn --batch-mode --no-transfer-progress package -DskipTests -Djacoco.skip=true -Drevision="${VERSION}" \
    && java -Djarmode=tools -jar target/sunspec-exporter.jar extract --layers --destination /out

# Runtime image: JRE only, no shell, runs as UID 65532
FROM gcr.io/distroless/java21-debian13:nonroot

LABEL org.opencontainers.image.source="https://github.com/thomashoell/sunspec-exporter" \
      org.opencontainers.image.description="Prometheus exporter for SunSpec compatible PV inverters (Modbus TCP)"

WORKDIR /app
# one layer per Spring Boot jar layer: releases usually only change the application layer
COPY --from=builder /out/dependencies/ ./
COPY --from=builder /out/spring-boot-loader/ ./
COPY --from=builder /out/snapshot-dependencies/ ./
COPY --from=builder /out/application/ ./

USER 65532:65532
EXPOSE 8080/tcp

# configuration: /etc/sunspec-exporter/config.yaml (see config.yaml.template) or environment variables
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/sunspec-exporter.jar"]
