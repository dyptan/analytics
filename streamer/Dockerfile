FROM openjdk:8-jre-slim
ADD target/dist/lib/ streamer/lib/
ADD target/dist/*.properties streamer/conf/
ADD target/dist/StreamTransformer.jar streaer/StreamTransformer.jar
ADD target/dist/dummy/ streamer/defaultModel/

WORKDIR streamer
ENTRYPOINT ["java", "-cp", "/lib/*:StreamTransformer.jar", "-Dlog4j.configuration=file:///conf/log4j.properties", "com.dyptan.StreamDriver"]