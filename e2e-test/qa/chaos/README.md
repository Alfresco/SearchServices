# About

This is an simple implementation of custom chaos-monkey deployment using https://git.alfresco.com/search_discovery/search-deployment-scripts/tree/master/kops-cluster as a base platform; for this 
the base readme is keept in the bottom of this readme.

# Prerequisites

* have kubernetes cluster deployed
* build nodejs app for testing purposes (just an app that will display time)
  
  ```shell
  cd ../stupid-server/
  docker build -t docker build -t quay.io/lsuciu/stupid-server .
  docker push
  ```
* build chaos-monkey image
  
  ```shell
  cd ../kubernetes-pod-chaos-monkey/
  docker build -t quay.io/lsuciu/chaos:latest .
  docker push
  ```
* deploy the application
  
  ```shell
  kubectl create -f namespace.yaml
  kubectl create -f deployment.yaml
  kubectl create -f service.yaml
  ```
* edit chaos.yaml with your values for the env variables (KILL_NR -> number of pods to be killed simultaneous, DELAY_SEC -> number of seconds between repeating the kill cycle)
  
  ```shell
    env:
    - name: NAMESPACE
      value: "stupid-server"
    - name: KILL_NR
      value: "3"
    - name: DELAY_SEC   
      value: "20"
  ```
* deploy the chaos 
  
  ```shell
  kubectl create -f chaos.yaml
  ```
