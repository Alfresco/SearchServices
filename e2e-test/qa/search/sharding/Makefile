include ../../Makefile
include .env

# CURRENT_DIR is the folder where this Makefile is saved
CURRENT_DIR:=$(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))

ifeq ($(sharding),)
	sharding:=$(dc) -f ../docker-compose.yml -f docker-compose.sharding.yml
endif

# https://docs.alfresco.com/6.0/concepts/dynamic-sharding.html
# http://docs.alfresco.com/6.0/tasks/adminconsole-indexserver-sharding.html
sharding: ## start Search Services in sharding mode
	make clean && \
	$(sharding) config && $(sharding) up -d && make wait
	