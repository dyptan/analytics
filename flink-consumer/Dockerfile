FROM openjdk:23-slim-bullseye

COPY target/*with-dependencies.jar /app/app.jar

WORKDIR /app/
ENTRYPOINT ["java", "-Djava.security.manager=allow", "-jar", "/app/app.jar"]