#!/bin/bash

export repo=diptan
default_modules=("beam-consumer" "scio-exporter" "spark-exporter" "zio-producer" "spring-web")

# Use provided arguments if any, otherwise use default modules
if [ $# -eq 0 ]; then
    modules=("${default_modules[@]}")
else
    modules=("$@")
fi

for module in "${modules[@]}"
do
    echo "Processing module: $module"
    mvn clean package -DskipTests -pl $module
    docker build --rm -f $module/Dockerfile -t $repo/$module:latest $module
    docker push $repo/$module:latest
done