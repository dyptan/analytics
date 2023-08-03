#!/bin/bash
export repo = europe-west9-docker.pkg.dev/synthetic-verve-390913/analytics
for module in "$@"

do
mvn clean package -DskipTests -pl $module
docker build --rm -f $module/Dockerfile -t $repo/$module:latest $module
docker push $repo/$module:latest
done



