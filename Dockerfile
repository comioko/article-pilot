FROM m.daocloud.io/docker.io/maven:3.9.11-eclipse-temurin-21-alpine AS build

WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp -DskipTests clean package && \
    cp target/ArticlePilot-*.jar target/app.jar

FROM eclipse-temurin:21-jre-alpine

ENV PUPPETEER_SKIP_DOWNLOAD=true \
    PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium \
    MERMAID_PUPPETEER_CONFIG=/app/puppeteer-config.json

RUN apk add --no-cache \
        chromium \
        curl \
        font-noto-cjk \
        freetype \
        harfbuzz \
        nodejs \
        npm \
        nss && \
    npm install --global @mermaid-js/mermaid-cli@11.12.0 && \
    npm cache clean --force && \
    addgroup --system spring && \
    adduser --system --ingroup spring spring

WORKDIR /app

COPY --from=build /workspace/target/app.jar ./app.jar
COPY deploy/puppeteer-config.json ./puppeteer-config.json

RUN chown -R spring:spring /app

USER spring

EXPOSE 8567

HEALTHCHECK --interval=30s --timeout=5s --start-period=600s --retries=10 \
    CMD curl --fail --silent http://localhost:8567/api/health/ > /dev/null || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
