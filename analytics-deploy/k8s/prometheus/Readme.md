## Prometheus deployment

1. Make sure the Mapr FlexDriver is provisioned (see Readme in ../mapr/Flex)

2. Create PVC
`kubectl apply prometheus-pvc.yaml`

3. Install Prometheus with Helm charts
` helm install stable/prometheus --namespace prometheus --name prometheus --values values.yml`