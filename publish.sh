#!/bin/bash

publish_or_not=false
if [[ $1 == "-p" ]]
then
publish_or_not=true
shift 1
fi


for module in "$@"

do

mvn clean package -DskipTests -pl $module

docker build --rm -f $module/Dockerfile -t diptan/$module:latest $module

if [[ $publish_or_not ]]
then
docker push diptan/$module
fi

done
# docker run -p 8081:8081 --name trainer diptan/trainer 
# docker run -p 8080:8080 --name web diptan/web 
# docker run --name treamer diptan/streamer


