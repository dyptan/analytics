FROM openjdk:8-jre-slim
ADD target/dist/lib /trainer/lib
ADD target/dist/conf /trainer/conf
ADD target/dist/Trainer.jar /trainer/bin/

WORKDIR /trainer/
ENTRYPOINT ["java", "-cp", "lib/*:conf/:bin/Trainer.jar", "com.dyptan.TrainerLauncher"]
EXPOSE 8081