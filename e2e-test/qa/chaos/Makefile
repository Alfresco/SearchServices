include ../Makefile
include configs/cluster.env
export $(shell sed 's/=.*//' configs/cluster.env)

CHART_NAME := alfresco-incubator/alfresco-content-services

ifeq ($(DEPLOY_ACS_NAME),)
	DEPLOY_ACS_NAME:=$(shell cat $(ACTIVE_DEPLOY) | grep DEPLOY_ACS_NAME | cut -d '=' -f2 )
endif

ifeq ($(DEPLOY_INGRESS_NAME),)
	DEPLOY_INGRESS_NAME:=$(shell cat $(ACTIVE_DEPLOY) | grep DEPLOY_INGRESS_NAME | cut -d '=' -f2)
endif

cluster-bucket-create: ## 1 - (k8s) creates S3 bucket for KOPS cluster as defined in cluster.env
	aws s3 mb $(KOPS_STATE_STORE)
	aws s3api put-bucket-versioning --bucket $(S3_BUCKET_NAME) --versioning-configuration Status=Enabled

cluster-setup:
	kops create cluster \
	--ssh-public-key $(SSH_PUBLIC_KEY_LOCATION) \
	--name $(KOPS_CLUSTER_NAME) \
	--zones $(AWS_REGION)a \
	--cloud aws \
	-v 10 \
	--kubernetes-version "$(KUBERNETES_VERSION)" \
	--node-count 4 \
	--master-size m4.xlarge \
	--node-size m4.xlarge

cluster-install: cluster-setup ## 2 - (k8s) install the cluster as defined in cluster.env
	kops update cluster --name ${KOPS_CLUSTER_NAME} --yes		

cluster-validate: ## 3 - (k8s) validate the cluster as defined in cluster.env
	kops validate cluster --name ${KOPS_CLUSTER_NAME}

cluster-dashboard-install: ## 4 - (k8s) install k8s dashboard 
	kubectl create clusterrolebinding permissive-binding \
	--clusterrole=cluster-admin \
	--user=admin \
	--user=kubelet \
	--group=system:serviceaccounts

	kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/master/aio/deploy/recommended/kubernetes-dashboard.yaml
#	kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/master/src/deploy/recommended/kubernetes-dashboard.yaml

	@echo Access the K8S Dashboard at: https://api.$(KOPS_CLUSTER_NAME)/$(K8S_DASHBOAR_URL)
	@echo Login with admin token taken after running "make cluster-info"

cluster-prepare: ## 5 - (k8s) prepare environment adding helm, tiller, secrets, etc
	kubectl create -f configs/tiller-rbac-config.yaml
	helm init --service-account tiller
	kubectl create -f secrets/quay-registry-secret.yaml --namespace default

	# Manual Operation
	@echo Now run the following:
	@echo $ kops edit cluster --state $(KOPS_STATE_STORE)
	@echo \nApply https://github.com/Alfresco/acs-deployment/blob/master/docs/k8s-pod-security-policies.md
	@echo $ kops update cluster --yes --state $(KOPS_STATE_STORE)
	@echo $ kops rolling-update cluster --yes --state $(KOPS_STATE_STORE)

	@echo $ kubectl get psp
	@echo $ kubectl edit psp kube-system

	# EFS Storage
	@echo https://github.com/Alfresco/alfresco-dbp-deployment#6-efs-storage-note-only-for-aws
	@echo Don't forget to open inbound traffic in security group to allow NFS traffic

cluster-get-info: ## 6 - (k8s) display cluster info/passwords
	kubectl cluster-info
	@echo Kube secret for admin user:
	@kops get secrets kube --type secret -oplaintext
	@echo Admin secret for admin user:
	@kops get secrets admin --type secret -oplaintext	

cluster-master-ssh: ## (k8s) ssh to master node	
	ssh -o StrictHostKeyChecking=no -i $(SSH_PRIVATE_KEY_LOCATION) admin@api.$(KOPS_CLUSTER_NAME)

cluster-node-ssh: ## (k8s) ssh to master node
	kubectl get nodes -o wide
	@read -p "Enter Node External IP:" externalIP && echo $$externalIP > NODE_IP
	ssh -o StrictHostKeyChecking=no -i $(SSH_PRIVATE_KEY_LOCATION) admin@$(shell cat NODE_IP)

cluster-pods-get: ## (k8s) show available pods
	kubectl get pods --namespace=$(NAMESPACE)

cluster-pod-ssh: cluster-pods-get ## (k8s) ssh to POD
	@read -p "Enter POD NAME:" podname && kubectl exec -it $$podname --namespace=$(NAMESPACE) -- /bin/bash
	
cluster-delete: ## (k8s) delete the cluster defined in cluster.env
	kops delete cluster --state=$(KOPS_STATE_STORE) --yes

cluster-proxy: ## (k8s) enable k8s proxy
	@kubectl proxy &
	@echo Opening K8S Dashboard localy via proxy, click Skip on login page! 
	open http://localhost:8001/$(K8S_DASHBOAR_URL)

# this will create the ACS namespace
# as you see I'm copying the docker secret from default namespace
acs-namespace-create: ## 1 - (ACS) create namespace that includes ingress & roles for PSP
	$(eval DEPLOY_ACS_NAME:=bau)
	$(eval DEPLOY_INGRESS_NAME:=ingress-ass)
	$(eval NAMESPACE:=$(NAMESPACE))

	$(shell echo DEPLOY_ACS_NAME=$(DEPLOY_ACS_NAME) > $(ACTIVE_DEPLOY))
	$(shell echo DEPLOY_INGRESS_NAME=$(DEPLOY_INGRESS_NAME) >> $(ACTIVE_DEPLOY))
	$(shell echo NAMESPACE=$(NAMESPACE) >> $(ACTIVE_DEPLOY))

	kubectl create namespace $(NAMESPACE)
	kubectl get secret $(DOCKER_SECRET_NAME) -o json --namespace default | jq '.metadata.namespace = "$(NAMESPACE)"' | kubectl create -f  -
	
	# create a role in a namespace which grant PSP usage
	# link "default" service account in the namespace to the role
	# this is required to set up nginx ingress -> you need to have $ make cluster-prepare executed once
	kubectl -n $(NAMESPACE) create role $(NAMESPACE):psp --verb=use --resource=podsecuritypolicy --resource-name=kube-system
	kubectl -n $(NAMESPACE) create rolebinding $(NAMESPACE):psp:default --role=$(NAMESPACE):psp --serviceaccount=$(NAMESPACE):default
	kubectl -n $(NAMESPACE) create rolebinding $(NAMESPACE):psp:ass-nginx-ingress --role=$(NAMESPACE):psp --serviceaccount=$(NAMESPACE):ass-nginx-ingress

	# define the namespace in ingressvalues.yaml
	@sed -i -e "s/namespace:.*/namespace: $(NAMESPACE)/" configs/ingressvalues.yaml
	@sed -i -e "s/external-dns.alpha.kubernetes.io\/hostname:.*/external-dns.alpha.kubernetes.io\/hostname: $(NAMESPACE).YourDNSZone/" configs/ingressvalues.yaml
	@rm -f configs/ingressvalues.yaml-e

	helm install stable/nginx-ingress \
	--name $(DEPLOY_INGRESS_NAME) \
	--version 0.14.0 \
	--set controller.scope.enabled=true \
	--set controller.scope.namespace=$(NAMESPACE) \
	--set rbac.create=true -f configs/ingressvalues.yaml \
	--namespace $(NAMESPACE)

	helm ls

acs-ingress-get: ## 2 - (ACS) get Ingress Status / Load Balancer IP address
	$(eval LOAD_BALANCER_IP := $(shell kubectl --namespace $(NAMESPACE) get services -o jsonpath='{.items[*].status.loadBalancer.ingress[0].hostname}'))
	@echo The Ingress Load Balancer is found at: $(LOAD_BALANCER_IP)
	
acs-helm-repo-add:
	helm repo add alfresco-incubator http://kubernetes-charts.alfresco.com/incubator
	helm repo add alfresco-stable http://kubernetes-charts.alfresco.com/stable
	helm repo update

acs-chart-upload: acs-helm-repo-add ## 3 - (ACS) deploy helm charts		
	helm install $(CHART_NAME) \
	--name $(DEPLOY_ACS_NAME) \
	--set externalProtocol="https" \
	--set externalHost="$(DEPLOY_ACS_NAME)-$(ROUTE_TAG).$(DOMAIN_NAME)" \
	--set externalPort="443" \
	--set repository.adminPassword="$(ACS_ADMIN_PASSWORD_MD5)" \
	--set postgresql.postgresPassword="admin" \
	--set persistence.reclaimPolicy=Recycle \
	--set alfresco-infrastructure.persistence.efs.enabled=true \
	--set alfresco-infrastructure.persistence.efs.dns="$(EFS_SERVER)" \
	--set alfresco-search.common.resources.requests.memory="2500Mi",alfresco-search.common.resources.limits.memory="2500Mi" \
	--set alfresco-search.common.environment.SOLR_JAVA_MEM="-Xms2000M -Xmx2000M" \
	--set postgresql.persistence.subPath="$(NAMESPACE)/alfresco-content-services/database-data" \
	--set persistence.repository.data.subPath="$(NAMESPACE)/alfresco-content-services/repository-data" \
	--set alfresco-search.master.persistence.search.data.subPath="$(NAMESPACE)/alfresco-content-services/solr-data" \
	--set alfresco-search.slave.persistence.search.data.subPath="$(NAMESPACE)/alfresco-content-services/solr-data-slave" \
	--set alfresco-search.common.type="insight-engine" \
	--set alfresco-search.common.registryPullSecrets="$(DOCKER_SECRET_NAME)" \
	--set alfresco-search.common.ingress.enabled=true \
	--set alfresco-search.common.ingress.basicAuth="YWRtaW46JGFwcjEkVEhjSS9NMDUkczVoQk1oVS8vLkJOekRIZXl6cW9HLg==" \
	--set alfresco-search.common.ingress.whitelist_ips="0.0.0.0/0" \
	--set alfresco-insight-zeppelin.enabled=true \
	--set alfresco-search.alfresco-insight-zeppelin.registryPullSecrets="$(DOCKER_SECRET_NAME)" \
	--set registryPullSecrets="$(DOCKER_SECRET_NAME)" \
	--set alfresco-search.slave.enabled=true \
	--set networkpolicysetting.enabled=false \
	--namespace=$(NAMESPACE)

acs-route-create: acs-ingress-get ## 4 - (ACS) create Route53 entry
	$(eval ROUTE_53_CONFIG_FILE:=configs/route53-entry.json)
	$(eval ROUTE_ENTRY_NAME:=$(DEPLOY_ACS_NAME)-$(ROUTE_TAG).$(DOMAIN_NAME))
			
	@sed -i -e "s/\"Action\":.*/\"Action\": \"CREATE\",/" $(ROUTE_53_CONFIG_FILE)
	@sed -i -e "s/\"Name\":.*/\"Name\": \"$(ROUTE_ENTRY_NAME)\",/" $(ROUTE_53_CONFIG_FILE)
	@sed -i -e "s/\"Value\":.*/\"Value\": \"$(LOAD_BALANCER_IP)\"/" $(ROUTE_53_CONFIG_FILE)
	
	@rm -f configs/*.json-e
	aws route53 change-resource-record-sets \
	--hosted-zone-id $(HOSTED_ZONE_ID) \
	--change-batch file://$(ROUTE_53_CONFIG_FILE)
	
	open http://$(ROUTE_ENTRY_NAME)/share
	@echo Route created: $(ROUTE_ENTRY_NAME) give a couple of seconds to wormup and refresh the page!


acs-route-open: ## 5 - (ACS) open the Route54 entry
	$(eval ROUTE_ENTRY_NAME:=$(DEPLOY_ACS_NAME)-$(ROUTE_TAG).$(DOMAIN_NAME))    	
	open http://$(ROUTE_ENTRY_NAME)/share

acs-chart-delete: ## 6 - (ACS) remove the deployment and cleanup namespace
	helm delete --purge $(DEPLOY_ACS_NAME)

acs-namespace-delete: ## 7 - (ACS) delete the current namespace
	helm delete --purge $(DEPLOY_INGRESS_NAME)
	kubectl delete ns $(NAMESPACE)
	helm ls

acs-route-delete: acs-ingress-get ## 8 - (ACS) delete Route53 entry
	$(eval ROUTE_53_CONFIG_FILE:=configs/route53-entry.json)
	$(eval ROUTE_ENTRY_NAME:=$(DEPLOY_ACS_NAME)-$(ROUTE_TAG).$(DOMAIN_NAME))		
	@sed -i -e "s/\"Action\":.*/\"Action\": \"DELETE\",/" $(ROUTE_53_CONFIG_FILE)
	@sed -i -e "s/\"Name\":.*/\"Name\": \"$(ROUTE_ENTRY_NAME)\",/" $(ROUTE_53_CONFIG_FILE)
	@sed -i -e "s/\"Value\":.*/\"Value\": \"$(LOAD_BALANCER_IP)\"/" $(ROUTE_53_CONFIG_FILE)
	@rm -f configs/*.json-e	
	aws route53 change-resource-record-sets \
	--hosted-zone-id $(HOSTED_ZONE_ID) \
	--change-batch file://$(ROUTE_53_CONFIG_FILE)

acs-all-delete: acs-ingress-get acs-chart-delete acs-namespace-delete acs-route-delete ## (ACS) cleanup the namespace and delete the deployed chart(s)

acs-all-install: acs-namespace-create acs-chart-upload ## (ACS) create the namespace and upload the chart
	$(shell echo ../.cmd/waitUntilPodsAvailable.sh $(NAMESPACE))
	$(shell make acs-route-create || exit 0)

acs-efs-describe: ## (ACS) describe the EFS file system
	aws efs describe-file-systems --file-system-id $(shell echo $(EFS_SERVER) | cut -d'.' -f1 )
