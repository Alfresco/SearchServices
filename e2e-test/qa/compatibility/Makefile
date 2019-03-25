include ../Makefile
include .env

# CURRENT_DIR is the folder where this Makefile is saved
CURRENT_DIR:=$(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))

start: ## 0 - starts search service with SSL enabled	
	$(dc) config && $(dc) up -d && \
	make wait

wait:
	$(helpers)/wait-service-to-start.sh
	
clean:
	$(dc) kill && $(dc) rm -fv

logs:
	$(dc) logs -f