FROM openjdk:8-jre-slim
ADD target/dist/lib /libs/
ADD target/dist/*.jar app.jar
ADD model/ /model
ENTRYPOINT ["java", "-cp", "/libs/*:app.jar", "-Dlog4j.configuration=file:///conf/log4j.properties", "com.dyptan.StreamLauncher"]