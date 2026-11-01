# Build the established Java 21 target; Node is tooling, not the app runtime.
FROM maven:3.9.12-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 couple && useradd --uid 10001 --gid couple --create-home couple
WORKDIR /app
COPY --from=build --chown=couple:couple /build/target/private-space-0.1.0-SNAPSHOT.jar /app/app.jar
USER couple
ENV SPRING_PROFILES_ACTIVE=render
# Leave room for native FFmpeg, thread stacks, direct buffers and JVM metadata.
ENV JAVA_TOOL_OPTIONS="-Xms64m -Xmx192m -XX:MaxDirectMemorySize=32m -XX:MaxMetaspaceSize=128m -XX:+ExitOnOutOfMemoryError"
EXPOSE 10000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
