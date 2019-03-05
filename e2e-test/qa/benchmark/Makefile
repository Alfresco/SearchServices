include ../Makefile

update: 
	# this will override existing data on your machine
	# only use it if you want to get the latest version from github
	@wget -O .env https://raw.githubusercontent.com/Alfresco/alfresco-bm-manager/master/server/docker-compose/.env
	@wget -O docker-compose-drivers.yml https://raw.githubusercontent.com/Alfresco/alfresco-bm-manager/master/server/docker-compose/docker-compose-drivers.yml
	@wget -O docker-compose-drivers.yml https://raw.githubusercontent.com/Alfresco/alfresco-bm-manager/master/server/docker-compose/docker-compose-drivers.yml
	@wget -O docker-compose-manager.yml https://raw.githubusercontent.com/Alfresco/alfresco-bm-manager/master/server/docker-compose/docker-compose-manager.yml

start-manager: ## 1 - start BM Manager
	$(dc) -f docker-compose-manager.yml up -d && \
	$(helpers)/wait-service-to-start.sh http://localhost:9080/alfresco-bm-manager

start-drivers: ## 2 - start BM Drivers
	$(dc) -f docker-compose-drivers.yml pull && docker-compose -f docker-compose-manager.yml -f docker-compose-drivers.yml up -d

all: start-manager start-drivers ## 0 - start BM Manager and Driver

clean: ## clean BM Manager and Drivers
	$(dc) -f docker-compose-manager.yml -f docker-compose-drivers.yml kill && \
	$(dc) -f docker-compose-manager.yml -f docker-compose-drivers.yml rm -fv