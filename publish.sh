#!/bin/bash

for module in "$@" 
do

mvn clean package -DskipTests -pl $module

docker build --rm -f $module/Dockerfile -t diptan/$module:latest $module

docker push diptan/$module
done
# docker run -p 8081:8081 --name trainer diptan/trainer 
# docker run -p 8080:8080 --name web diptan/web 
# docker run --name treamer diptan/streamer


