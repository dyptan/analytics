FROM openjdk:8-jre-slim
ADD target/dist/lib/ libs/
ADD target/dist/conf/ conf/
ADD target/dist/StreamTransformer.jar StreamTransformer.jar
ADD target/dist/dummy/ defaultModel/
ENTRYPOINT ["java", "-cp", "/libs/*:StreamTransformer.jar", "-Dlog4j.configuration=file:///conf/log4j.properties", "com.dyptan.StreamLauncher"]