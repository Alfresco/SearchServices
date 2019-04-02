include ../../Makefile
include .env

# CURRENT_DIR is the folder where this Makefile is saved
CURRENT_DIR:=$(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))

ifeq ($(dc-custom),)
	dc-custom:=$(dc) -f ../docker-compose.yml -f docker-compose.custom.yml
endif


## ---- CUSTOM
build: ## 1 - build a custom image: $ make SCRIPTS_FOLDER=spellcheck build	
ifndef SCRIPTS_FOLDER
	@echo SCRIPTS_FOLDER not defined "Usage: make SCRIPTS_FOLDER=spellcheck build"
	exit 1
endif
	$(dc-custom) build  --force-rm --no-cache --pull --build-arg SCRIPTS_FOLDER=$(SCRIPTS_FOLDER)

start: ## 2 - starts the custom image built: $ make start
	$(dc-custom) up -d && make wait
