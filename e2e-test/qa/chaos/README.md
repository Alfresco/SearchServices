# About

This is an simple implementation of custom chaos-monkey deployment using https://git.alfresco.com/search_discovery/search-deployment-scripts/tree/master/kops-cluster as a base platform; for this 
the base readme is keept in the bottom of this readme.

# Prerequisites

* have kubernetes cluster deployed using the bottom documentation (in this ca we skip the search acs deplyment for now as it is not chaos ready)
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



# KOPS CLUSTER SETUP

* how can I create a KOPS cluster in AWS
* how can I deploy ACS with ASS in my cluster
* all using simple make commands

:exclamation: **_I've collected/compressed in this script the knowledge scattered accross:_**
* https://github.com/Alfresco/alfresco-anaxes-shipyard 
* https://github.com/Alfresco/acs-deployment
* https://github.com/Alfresco/alfresco-search-deployment
* https://github.com/Alfresco/alfresco-infrastructure-deployment
* kops/kubectl commands
* other utilities

# Prerequisites K8

* [IAM Group setup](https://github.com/kubernetes/kops/blob/master/docs/aws.md#setup-your-environment) with the following policies:
  * AmazonEC2FullAccess
  * IAMFullAccess
  * AmazonS3FullAccess
  * AmazonVPCFullAccess
  * AmazonElasticFileSystemFullAccess
  * AmazonRoute53FullAccess

* a user that is added in this group

* a SSH key generated
  ```shell
  $ ssh-keygen -t rsa -b 4096 -C "anaxes_bastion" 
  ```
* [EFS Storage](https://docs.aws.amazon.com/efs/latest/ug/creating-using-create-fs.html) created with his Security Group updated (Edit Inbound Rules and add Rule to accept NFS TCP from Anywhere)

* `quay-registry-secret.yaml` created under [./secrets](./secrets) folder, according to [anaxes guidelines](https://github.com/Alfresco/alfresco-anaxes-shipyard/blob/c7d50a124901a2f19b67b31fc49f2c77c729b4ed/SECRETS.md)  

* update [.configs/cluster.env](./configs/cluster.env) 
  * to use the SSH keys above
  * to use the EFS_SERVER defined
  * to use KOPS_NAME as your username (to be unique)
  * to use DOCKER_SECRET_NAME as the secret defined above

# Usage

1) from this "kops-cluster" folder run `$ make` to see available commands

2) checkout first the [.configs/cluster.env](./configs/cluster.env) - all commands are using these variables. 

>:exclamation: if you already have a cluster defined, you can use these scripts, just change the KOPS_STATE_STORE to point to your bucket and you are all set!

3) you can execute each task one by one in the order displayed (1,2,3...)

![](.docs/intro.gif?raw=true)

4) or, execute the "all" task `$ make acs-all-install` that will do this automatically for you

# Features

## Cluster Related
a) gives you the ability to prepare the step to create a KOPS cluster in AWS based on [.configs/cluster.env](./configs/cluster.env)
  * it will create a S3 bucket for you: `make cluster-bucket-create`
  * it will install the KOPS cluster: `make cluster-install`
  * it will validate the cluster
  * it will install the K8S dashboard for you: `make cluster-dashboard-install`
  * it will install the helm, tiller or defined the secrets automatically: `make cluster-prepare`

b) gives you the ability to SSH to your cluster MASTER node: `make cluster-master-ssh`

c) gives you the ability to SSH to your cluster WORKERS node(s): `make cluster-node-ssh`
  * you will be prompted with the list of available nodes
  *  you will be asked on what node to connect to

d) gives you the ability to SSH to a particular POD:
  *  you will be prompted with the list of available nodes
  *  you will be asked on what POD_NAME to connect to

![](.docs/ssh-to-pod.gif?raw=true)

## ACS Related
>**Hint**: follow the numbered tasks displayed when you run: `make`

> :exclamation: The tasks are runnig on the last namespace created, on the last random helm chart deployed. 

>You can run any task related to ACS individually according to your development needs. 

> Example:
> * if namespace is created maybe you need only to update the chart (make acs-chart-upload) and create new route54 entry (make acs-route-create)

a) gives you the ability to define the NAMESPACE for your ACS deployment
  *  create the namespace with the ingress, security, etc automatically: `make acs-namespace-create`
  *  display the Load Balancer IP when is ready: `make acs-ingress get`

b) gives you the ability to deploy a helm chart (using random name) to namespace `make acs-chart-upload`

c) gives you the ability to define a Route53 CNAME entry: `make acs-route-create`

d) cleanup chart (`make acs-chart-delete`), namespace (`make acs-namespace-delete`) or all in one task: `make acs-all-delete`

e) prepare, install and deploy the app all in one task: `make acs-all-install`

![](.docs/all-acs-install.gif?raw=true)
