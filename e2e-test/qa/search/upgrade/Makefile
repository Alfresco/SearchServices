include ../../Makefile
include .env

# CURRENT_DIR is the folder where this Makefile is saved
CURRENT_DIR:=$(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))

ifeq ($(dc-upgrade),)
	dc-upgrade:=$(dc) -f ../docker-compose.yml -f docker-compose.upgrade.yml
endif

## ---- UPGRADE
set_version ?=latest # default version that will be used in tasks

as-previous: clean ## 1 - install the previous version: $ make set_version=1.2.1 as-previous
	rm -rf ./solr-data && \
	rm -rf ./solr-contentstore && \
	rm -f ./image-digests.txt
	export SEARCH_TAG=$(set_version) && \
	$(dc-upgrade) pull && \
	echo "\n====Previous====" > image-digests.txt && \
	$(dc-upgrade) config --resolve-image-digests >> image-digests.txt && \
	$(dc-upgrade) up -d && \
	docker ps

as-current: ## 2 - upgrade previous to this version $ make set_version=2.0.x as-current
	$(dc-upgrade) kill search && \
	$(dc-upgrade) rm -f search && \
	export SEARCH_TAG=$(set_version) && \
	$(dc-upgrade) pull search && \
	echo "\n====Current====" >> image-digests.txt && \
	$(dc-upgrade) config --resolve-image-digests >> image-digests.txt && \
	$(dc-upgrade) up -d search  && \
	docker ps	

# 
# Run the following commands if you need to test the upgrade e2e
#
#make set_version=1.2.1 as-previous wait
#make run-mvn-tests suiteXmlFile=./src/test/resources/search-pre-upgrade-suite.xml
#make set_version=2.0.x as-current wait
#make run-mvn-tests suiteXmlFile=./src/test/resources/search-post-upgrade-suite.xml