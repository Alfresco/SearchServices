include ../../Makefile
include .env

# CURRENT_DIR is the folder where this Makefile is saved
CURRENT_DIR:=$(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))

ifeq ($(dc-custom),)
	dc-custom:=$(dc) -f ../docker-compose.yml -f docker-compose.ssl.yml
endif

ssl: ## 0 - starts search service with SSL enabled
	$(dc-custom) build && \
	$(dc-custom) up -d && \
	make wait

wait:
	$(helpers)/wait-service-to-start.sh
	
clean:
	$(dc-custom) kill && $(dc-custom) rm -fv

logs:
	$(dc-custom) logs -f