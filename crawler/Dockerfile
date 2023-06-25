FROM openjdk:11-jre-slim
COPY target/*allinone.jar /app/app.jar
COPY application.conf /app

WORKDIR /app
ENTRYPOINT ["java", "-cp", "app.jar", "com.dyptan.crawler.Combined"]
EXPOSE 8083