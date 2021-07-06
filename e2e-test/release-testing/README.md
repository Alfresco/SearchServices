# release-testing
> Tools and Docker Compose templates for Release Testing

This project includes a collection of Docker Compose Templates to test Repository and Search Services/Insight Engine with different configurations.

Test execution information is available in [Test Execution internal wiki page](https://alfresco.atlassian.net/wiki/spaces/ENG/pages/398881/Test+Execution).

## Project structure

Following folders, including Docker Compose templates, are provided.

```
% tree -L 1
.
├── insight-engine
├── insight-engine-sharding
├── insight-engine-sharding-replica
├── insight-engine-zeppelin
├── mtls
├── search-services
├── search-services-backup
├── search-services-disable-tracking
├── search-services-replication
└── search-services-upgrade
```

Configurations have been generated mainly with [generator-alfresco-docker-compose](../generator-alfresco-docker-compose) project.

## Docker Compose templates

Before executing the test you have init the docker-compose env file. To perform this operation run the init.sh script followed by the Alfresco version do you have to test, e.g.:

```
$ init.sh 6.6.2
```

To create new configuration you need to create a file in this folder with the version as name and the .env extension.

After this command execution, every Docker Compose folder includes a `.env` file with versions settings like below:

```
ALFRESCO_TAG=7.1.0-A8
ALFRESCO_CE_TAG=7.1.0-A8
SHARE_TAG=7.1.0-A8
POSTGRES_TAG=11.4
TRANSFORM_ROUTER_TAG=1.2.0
TRANSFORM_CORE_AIO_TAG=2.2.1
TRANSFORMER_TAG=2.1.0
SHARED_FILE_STORE_TAG=0.7.0
ACTIVE_MQ_TAG=5.15.8
DIGITAL_WORKSPACE_TAG=1.5.0
ACS_NGINX_TAG=3.0.1
ACS_COMMUNITY_NGINX_TAG=1.0.0
SEARCH_TAG=2.0.2-RC1
ZEPPELIN_TAG=latest
ACA_TAG=master-latest

#To test versions before 7, please leave this property empty
JAVA_TOOL_OPTIONS= "-Dencryption.keystore.type=JCEKS
                -Dencryption.cipherAlgorithm=DESede/CBC/PKCS5Padding
                -Dencryption.keyAlgorithm=DESede
                -Dencryption.keystore.location=/usr/local/tomcat/shared/classes/alfresco/extension/keystore/keystore
                -Dmetadata-keystore.password=mp6yc0UD9e
                -Dmetadata-keystore.aliases=metadata
                -Dmetadata-keystore.metadata.password=oKIWzVdEdA
                -Dmetadata-keystore.metadata.algorithm=DESede"
```

Docker Compose templates may need some modifications in order to be adapted to new configurations.

Once started, services are available in following URLs:

* http://localhost:8080/alfresco
* http://localhost:8080/share
* http://localhost:8083/solr
* http://localhost:8084/solr (when using Sharding or Replica)
* http://localhost:9090/zeppelin

When using mTLS for Repository and SOLR communication, the endpoint for SOLR servers should be changed to:

* https://localhost:8083
* https://localhost:8084 (when using Sharding or Replica)

Tests requiring additional instructions include an additional `README.md` file.

Tests with persistent storage create a local folder named `data` with the *repository*, *db* and *solr* data.

## Test Catalog

Search via Share: Basic, Advanced, Faceted, Live Search, Highlighting, Language

* [search-services](search-services)
* [insight-engine](insight-engine)

Search configurations: sharding diff types

* [insight-engine-sharding](insight-engine-sharding)

Search configurations: master slave including master-slave contentStore replication + docker image

* [search-services-replication](search-services-replication)

Search configurations: sharding with replication + A test for enable.alfresco.tracking=false

* [insight-engine-sharding-replica](insight-engine-sharding-replica)
* [search-services-disable-tracking](search-services-disable-tracking)

Upgrades

* [search-services-upgrade](search-services-upgrade)

Backup

* [search-services-backup](search-services-backup)

JDBC driver with DBVisualizer

* [insight-engine](insight-engine)

Zeppelin

* [insight-engine-zeppelin](insight-engine-zeppelin)

SSL between ACS and Solr

* [mtls](mtls)
