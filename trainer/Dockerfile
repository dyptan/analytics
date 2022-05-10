FROM openjdk:8-jre-slim
ADD target/dist/lib /trainer/libs
ADD target/dist/trainer.properties /trainer/conf
ADD target/dist/Trainer.jar /trainer/bin/

WORKDIR /trainer/
ENTRYPOINT ["java", "-cp", "libs/*:conf/:bin/Trainer.jar", "com.dyptan.TrainerLauncher"]
EXPOSE 8081