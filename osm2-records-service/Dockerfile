# syntax=docker/dockerfile:1
# osm2-records-service — eclipse-temurin:21-jre-alpine, non-root UID 1000
FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL org.opencontainers.image.source="https://github.com/[TODO]/osm2"
LABEL org.opencontainers.image.description="osm2-records-service"

# Create non-root user
RUN addgroup -S osm2 && adduser -S osm2 -G osm2 -u 1000

WORKDIR /app

# Copy the fat jar produced by spring-boot-maven-plugin
COPY target/osm2-records-service-*.jar app.jar

RUN chown -R osm2:osm2 /app

USER 1000

EXPOSE 8085

ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
