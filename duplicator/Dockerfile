FROM openjdk:21-slim-bullseye

COPY target/*with-dependencies.jar /app/app.jar

WORKDIR /app/
ENTRYPOINT ["java", "-jar", "app.jar"]