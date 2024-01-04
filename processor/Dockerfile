FROM openjdk:23-slim-bullseye

ADD target/*jar-with-dependencies.jar /app/app.jar

WORKDIR /app/
ENTRYPOINT ["java", "-jar","app.jar"]