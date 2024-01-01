FROM openjdk:11-jre-slim

ADD target/dist/lib /web/lib
ADD target/dist/conf /web/conf
ADD target/dist/web*.jar /web/app.jar

WORKDIR /web/
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-cp","lib/*:conf/:app.jar","com.dyptan.Web"]
EXPOSE 8080