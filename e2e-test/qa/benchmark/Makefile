include ../Makefile

ALFRESCO_URL?=http://localhost:8081
export ALFRESCO_URL

update: 
	# this will override existing files that you have already in this folder
	# use it only if you want to get the latest version from github
	@wget -O .env https://raw.githubusercontent.com/Alfresco/alfresco-bm-manager/master/server/docker-compose/.env
	@wget -O docker-compose-drivers.yml https://raw.githubusercontent.com/Alfresco/alfresco-bm-manager/master/server/docker-compose/docker-compose-drivers.yml
	@wget -O docker-compose-drivers.yml https://raw.githubusercontent.com/Alfresco/alfresco-bm-manager/master/server/docker-compose/docker-compose-drivers.yml
	@wget -O docker-compose-manager.yml https://raw.githubusercontent.com/Alfresco/alfresco-bm-manager/master/server/docker-compose/docker-compose-manager.yml

start-manager: ## 1 - start BM Manager
	$(dc) -f docker-compose-manager.yml up -d && \
	$(helpers)/wait-service-to-start.sh http://localhost:9080/alfresco-bm-manager

start-drivers: ## 2 - start BM Drivers
	$(dc) -f docker-compose-drivers.yml pull && docker-compose -f docker-compose-manager.yml -f docker-compose-drivers.yml up -d

start-all: start-manager start-drivers ## 3 - start BM Manager and Driver

clean: ## clean BM Manager and Drivers
	$(dc) -f docker-compose-manager.yml down || true && $(dc) -f docker-compose-drivers.yml down || true

benchmark-prepare: ## 1 - prepare data with users/sites for benchmark
	./prepare-users.sh
	./prepare-data.sh

benchmark-run: ## 2 - here we will actually execute the RestAPI for Search
	./benchmark-search.sh
