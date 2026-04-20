# syntax=docker/dockerfile:1

# ---- build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q -e dependency:go-offline
COPY src ./src
RUN mvn -B -q -e -DskipTests package

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

WORKDIR /app
COPY --from=build /build/target/ppt-mcp-server-all.jar /app/ppt-mcp-server.jar

USER 10001
ENV LOG_LEVEL=INFO
ENV SOFFICE_PATH=/usr/bin/soffice
ENV MCPO_ALLOWED_ROOT=/workspace/resources
ENV MCPO_TEMPLATE_DIR=/workspace/resources/.mcpo-ppt/templates
ENV MCPO_DEFAULT_TEMPLATE_CONFIG=/workspace/resources/.mcpo-ppt/default-template.json

ENTRYPOINT ["java", "-jar", "/app/ppt-mcp-server.jar"]