# Docker Compose Reference for ACS 6.x

This project includes default configuration for ACS Community 6.1 and Search Services 1.2 using Plain HTTP communication between Repository and SOLR

## Running Docker Compose

Docker can be started using default command.

```bash
$ docker-compose up --build
```

Alfresco will be available at:

http://localhost:8082/alfresco

http://localhost:8080/share

http://localhost:8083/solr

Plain HTTP Communication from SOLR is targeted inside Docker Network to http://alfresco:8080/alfresco


**Mutual TLS/SSL Communication**

Folder `ssl` includes default TLS/SSL Communication between SOLR and Repository.
