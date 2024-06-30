FROM openjdk:21-slim-bullseye
COPY target/dist/Main.jar /app/app.jar

ADD target/dist/lib /app/lib
WORKDIR /app

ENTRYPOINT ["java", "--add-exports", "java.base/sun.nio.ch=ALL-UNNAMED", "-jar", "app.jar"]
EXPOSE 8082 4040