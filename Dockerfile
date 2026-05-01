# Stage 1: Build
FROM gradle:9.5.0-jdk25-corretto AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build -x test --no-daemon

# Stage 2: Run
FROM amazoncorretto:25.0.3-alpine3.23
COPY --from=build /home/gradle/src/build/libs/server.jar /app/server.jar
ENTRYPOINT ["java","-jar","/app/server.jar"]
