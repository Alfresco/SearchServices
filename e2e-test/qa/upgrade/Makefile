include ../Makefile
include .env

# CURRENT_DIR is the folder where this Makefile is saved
CURRENT_DIR:=$(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))

SEARCH_IMAGE ?=quay.io/alfresco/search-services
SEARCH_TAG ?=latest # default version that will be used in tasks

as-previous: clean ## 1 - install the previous version: $ make SEARCH_IMAGE=quay.io/alfresco/search-services SEARCH_TAG=1.2.1 as-previous
	rm -rf ./solr-data && \
	rm -rf ./solr-contentstore && \
	rm -f ./image-digests.txt && \
	export SEARCH_TAG=$(SEARCH_TAG) && \
	export SEARCH_IMAGE=$(SEARCH_IMAGE) && \
	$(dc) pull && \
	echo "\n====Previous====" > image-digests.txt && \
	$(dc) config --resolve-image-digests >> image-digests.txt && \
	$(dc) up -d && \
	docker ps 

as-current: ## 2 - upgrade previous to this version $ make SEARCH_IMAGE=quay.io/alfresco/search-services SEARCH_TAG=2.0.x as-current
	$(dc) kill search && \
	$(dc) rm -f search && \
	export SEARCH_TAG=$(SEARCH_TAG) && \
	export SEARCH_IMAGE=$(SEARCH_IMAGE) && \
	$(dc) pull search && \
	echo "\n====Current====" >> image-digests.txt && \
	$(dc) config --resolve-image-digests >> image-digests.txt && \
	$(dc) up -d search  && \
	docker ps 
# 
# Run the following commands if you need to test the upgrade e2e
#
#make SEARCH_IMAGE=quay.io/alfresco/search-services SEARCH_TAG=1.2.1 as-previous wait
#make run-mvn-tests suiteXmlFile=./src/test/resources/search-pre-upgrade-suite.xml

#make SEARCH_IMAGE=quay.io/alfresco/search-services SEARCH_TAG=2.0.x as-current wait
#make run-mvn-tests suiteXmlFile=./src/test/resources/search-post-upgrade-suite.xml