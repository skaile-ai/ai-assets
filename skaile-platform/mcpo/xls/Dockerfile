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

RUN useradd --uid 10001 --create-home --shell /sbin/nologin appuser

WORKDIR /app
COPY --from=build /build/target/excel-mcp-*.jar /app/excel-mcp.jar

USER 10001
ENV LOG_LEVEL=INFO
# EXCEL_MCP_ROOT, EXCEL_MCP_MAX_FILE_BYTES, EXCEL_MCP_MAX_CELLS:
#   set by the deployment, not baked in.

ENTRYPOINT ["java", "-jar", "/app/excel-mcp.jar"]
