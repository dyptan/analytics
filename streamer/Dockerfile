FROM openjdk:8-jre-slim
ADD target/dist/lib /libs/
ADD target/dist/*.jar app.jar
ENTRYPOINT ["java", "-cp", "/libs/*:app.jar", "-Dlog4j.configuration=file:///conf/log4j.properties", "com.dyptan.StreamLauncher"]