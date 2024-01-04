#!/bin/bash
#used on GCP
#export repo=europe-west9-docker.pkg.dev/synthetic-verve-390913/analytics
export repo=diptan
for module in "$@"

do
mvn clean package -DskipTests -pl $module
docker build --rm -f $module/Dockerfile -t $repo/$module:1.0 $module
docker push $repo/$module:latest
done



