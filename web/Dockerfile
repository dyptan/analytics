FROM openjdk:23-slim-bullseye

ADD target/dist/conf /web/conf
ADD target/dist/web*.jar /web/app.jar

WORKDIR /web/
ENTRYPOINT ["java", "-jar","app.jar"]
#ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-cp","lib/*:conf/:app.jar","com.dyptan.Web"]
EXPOSE 8080