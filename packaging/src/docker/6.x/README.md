# Docker Compose Reference for ACS 6.x

This project includes a catalog of Docker Compose files for ACS 6.x Enterprise and Community versions:

* `community`

  * `docker-compose.yml` Template for ACS Community 6.1 and Search Services 1.3 with plain HTTP communication between Repository and SOLR

  * `docker-compose-ssl.yml` Template for ACS Community 6.1 and Search Services 1.3 with Mutual TLS communication between Repository and SOLR

* `enterprise`

  * `docker-compose.yml` Template for ACS Enterprise 6.1 and Insight Engine 1.1 with plain HTTP communication between Repository and SOLR

  * `docker-compose-ssl.yml` Template for ACS Enterprise 6.1 and Insight Engine 1.1 with Mutual TLS communication between Repository and SOLR

Using **enterprise** Docker Images requires `quay.io` credentials. Every customer or partner can ask for credentials at [Alfresco Support](https://support.alfresco.com).
