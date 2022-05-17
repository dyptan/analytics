#!/bin/bash

for module in "$@"

do
mvn clean package -DskipTests -pl $module
docker build --rm -f $module/Dockerfile -t diptan/$module:latest $module
done



