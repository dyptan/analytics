FROM openjdk:11-jre-slim
COPY target/*allinone.jar /app/app.jar
COPY src/main/resources/application.conf /app

WORKDIR /app
ENTRYPOINT ["java", "-jar", "app.jar"]
EXPOSE 8083