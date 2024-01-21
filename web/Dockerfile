FROM openjdk:23-slim-bullseye

ADD target/dist/*.jar /app/app.jar

WORKDIR /app/
ENTRYPOINT ["java", "-jar","app.jar"]
EXPOSE 8080