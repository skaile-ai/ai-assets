# syntax=docker/dockerfile:1

# ---- build stage ----
# Uses the Maven Wrapper pinned in .mvn/wrapper/maven-wrapper.properties so image
# builds and local `./mvnw` builds resolve the same Maven version. The base image
# supplies a JDK + network egress for the wrapper to download its pinned Maven.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY mvnw .
COPY .mvn ./.mvn
COPY pom.xml .
RUN ./mvnw -B -q -e dependency:go-offline
COPY src ./src
RUN ./mvnw -B -q -e -DskipTests package

# ---- runtime stage ----
FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
 && apt-get -y upgrade \
 && apt-get install -y --no-install-recommends \
      libreoffice-core \
      libreoffice-impress \
      fontconfig \
      fonts-liberation \
      fonts-noto \
      fonts-noto-cjk \
      fonts-noto-color-emoji \
 && fc-cache -fv \
 && rm -rf /var/lib/apt/lists/*

RUN useradd --uid 10001 --create-home --shell /sbin/nologin appuser

# A world-writable scratch dir for LibreOffice's user profile and Java's tmpdir.
# Callers commonly run with `--user 1000:1000` (matches typical host uid for bind
# mounts); that uid has no /etc/passwd entry, so `$HOME` falls back to `/` and
# soffice exits 77 before doing anything. Pointing HOME + java.io.tmpdir at a
# writable dir keeps the server working regardless of the runtime uid.
RUN mkdir -p /workspace/.runtime && chmod 1777 /workspace/.runtime

WORKDIR /app
COPY --from=build /build/target/ppt-mcp-server-all.jar /app/ppt-mcp-server.jar

USER 10001
ENV HOME=/workspace/.runtime
ENV LOG_LEVEL=INFO
ENV SOFFICE_PATH=/usr/bin/soffice
ENV MCPO_ALLOWED_ROOT=/workspace/resources
ENV MCPO_TEMPLATE_DIR=/workspace/resources/.mcpo-ppt/templates
ENV MCPO_DEFAULT_TEMPLATE_CONFIG=/workspace/resources/.mcpo-ppt/default-template.json

ENTRYPOINT ["java", "-Djava.io.tmpdir=/workspace/.runtime", "-jar", "/app/ppt-mcp-server.jar"]