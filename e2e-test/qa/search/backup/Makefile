include ../../Makefile
include .env

# the suffix of the backup taken in time. It can be overriden on runtime: make SUFIX=T1 backup-perform 
SUFIX ?=T0
# CURRENT_DIR is the folder where this Makefile is saved
CURRENT_DIR:=$(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))

# this is used also in compose yml files
export HOST_BACKUP_LOCATION:=$(CURRENT_DIR)/host-bkp

ifeq ($(dc-backup),)
	dc-backup:=$(dc) -f ../docker-compose.yml -f docker-compose.backup.yml
endif

backup-prepare: clean ## 1 - prepare backup for testing
	@echo "Starting Backup Prepare" && \
	$(sudo) rm -rf $(HOST_BACKUP_LOCATION) && \
	mkdir -p $(HOST_BACKUP_LOCATION)/alf_data && \
	mkdir -p $(HOST_BACKUP_LOCATION)/solr/archive && \
	mkdir -p $(HOST_BACKUP_LOCATION)/solr/alfresco && \
	mkdir -p $(HOST_BACKUP_LOCATION)/db && \
	$(sudo) chmod -R 777 $(HOST_BACKUP_LOCATION) && \
	$(dc-backup) up -d
	
backup-perform: ## 2 - perform the backup	of alf_data and db data
	@echo "Starting Backup Perform" && \
	$(sudo) rm -rf $(HOST_BACKUP_LOCATION)_$(SUFIX) && \
	$(sudo) chmod -R 777 $(HOST_BACKUP_LOCATION) && \
	$(dc-backup) stop alfresco && \
	$(dc-backup) exec postgres bash -c 'pg_dump --dbname=postgresql://alfresco:alfresco@127.0.0.1:5432/alfresco' > $(HOST_BACKUP_LOCATION)/db/alfresco.pg && \
	cp -R $(HOST_BACKUP_LOCATION) $(HOST_BACKUP_LOCATION)_$(SUFIX) && \
	$(dc-backup) start alfresco

backup-restore: clean ## 3 - start restoring from backup location
	@echo "Starting Backup Restore" && \
	$(sudo) rm -rf $(HOST_BACKUP_LOCATION) && \
	mkdir -p $(HOST_BACKUP_LOCATION) && \
	cp -rf $(HOST_BACKUP_LOCATION)_$(SUFIX)/alf_data $(HOST_BACKUP_LOCATION)/alf_data && \
	cp -rf $(HOST_BACKUP_LOCATION)_$(SUFIX)/db/ $(HOST_BACKUP_LOCATION)/db/ && \
	cp -rf $(HOST_BACKUP_LOCATION)_$(SUFIX)/solr $(HOST_BACKUP_LOCATION)/solr && \
	$(sudo) chmod -R 777 $(HOST_BACKUP_LOCATION) && \
	$(dc-backup) up -d postgres && sleep 30 && \
	$(dc-backup) exec postgres bash -c 'psql --dbname=postgresql://alfresco:alfresco@127.0.0.1:5432/alfresco < /backup/db/alfresco.pg' && \
	$(dc-backup) up -d

all: show-config ## 0 - executes the entire backup process
	# perform the backup and waits until the server is starting
	# do some change on backed up data
	# then restore from backup and check the content is restored as expected
	make backup-prepare wait && \
	make run-mvn-tests suiteXmlFile=./src/test/resources/search-pre-backup-suite.xml
	
	make backup-perform wait && \
	make run-mvn-tests suiteXmlFile=./src/test/resources/search-on-backup-suite.xml
	
	make backup-restore wait && \
	make run-mvn-tests suiteXmlFile=./src/test/resources/search-post-backup-suite.xml

show-config: ## show compose configuration
	$(dc-backup) config

clean: ## kill containers, remove volumes and data
	$(dc-backup) kill && $(dc-backup) rm -fv
	$(sudo) rm -rf $(HOST_BACKUP_LOCATION)	

tail-logs: ## tails all container logs
	$(dc-backup) logs -f
