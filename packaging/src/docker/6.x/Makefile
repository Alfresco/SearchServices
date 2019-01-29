help: ## output this help	
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)	

.DEFAULT_GOAL := help

ifeq ($(SCRIPTS_FOLDER),)
	SCRIPTS_FOLDER:=spellcheck
endif

.PHONY: custom
build: ## build a custom image with files from SCRIPTS_FOLDER: $make SCRIPTS_FOLDER=spellcheck build
	docker-compose -f docker-compose.yml -f docker-compose.custom.yml build  --force-rm --no-cache --pull --build-arg SCRIPTS_FOLDER=$(SCRIPTS_FOLDER)

up: ## starts the custom image built: $make up
	docker-compose -f docker-compose.yml -f docker-compose.custom.yml up

shell: ## opens a shell inside the 'search' container
	docker-compose exec search sh

clean: ## kill containers $ make clean
	docker-compose kill && docker-compose rm -fv
