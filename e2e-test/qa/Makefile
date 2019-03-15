# master Makefile that contains common tasks for other Makefiles in this QA folder
# execute any on the task that are found here from your terminal
UNAME := $(shell uname)
SHELL := /bin/bash
export COMPOSE_INTERACTIVE_NO_CLI=1

# CURRENT_DIR is the folder where this Makefile is saved
CURRENT_DIR:=$(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))

# the root dir where E2E maven project resides
ROOT_DIR:=$(shell dirname $(CURRENT_DIR))


help: ## main: output this help	
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)
	
.DEFAULT_GOAL := help

# $(helpers)/wait-service-to-start.sh
ifeq ($(helpers),)
	helpers:=$(ROOT_DIR)/qa/.helpers
endif

# $(dc) ls
ifeq ($(dc),)
	dc:=docker-compose
endif

# on linux use sudo
ifeq ($(UNAME), Linux)
	sudo := sudo
endif

# define the maven version
ifeq ($(mvn),)
	ifeq ($(bamboo_working_directory),)#if on local environment
		mvn:=mvn
	else
		export M2_HOME=
		export JAVA_HOME=/opt/jdk-11
		mvn:=/opt/maven-3.3/bin/mvn
	endif
endif

# open a shell to search containers
shell: ## main: open a shell to search service: $ make shell
	@$(dc) exec search sh

# kill all containers on default docker-compose.yml file
clean: ## main: kill containers
	$(dc) kill && $(dc) rm -fv	

# just waits for a alfresco and solr to start
wait: ## main: wait for alfresco service && solr to startup	
	$(helpers)/wait-service-to-start.sh && \
	$(helpers)/wait-service-to-start.sh http://localhost:8083/solr

standard: ## main: start with standard docker-compose.yml file
	make clean && $(dc) config && $(dc) up -d && make wait

run-mvn-tests: ## run e2e maven suite: $make run-mvn-tests suiteXmlFile=src/path-to-suite.xml
ifndef suiteXmlFile
	@echo suiteXmlFile not specified! Usage: make run-mvn-tests suiteXmlFile=src/path-to-suite.xml
	exit 1
endif
	cd $(ROOT_DIR) && $(mvn) test -DsuiteXmlFile=$(suiteXmlFile)
