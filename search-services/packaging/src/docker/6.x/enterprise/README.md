# Docker Templates for Community Edition

This project includes default configuration for ACS Enterprise 6.1 and Insight Engine 1.1.

Different Docker Compose templates are provided:

* `docker-compose.yml` to use Plain HTTP communication between Repository and SOLR

* `docker-compose-ssl.yml` to use Mutual TLS communication between Repository and SOLR

Every *truststores*, *keystores* and *certificates* are copied from sources.

## Components

* **alfresco** includes a `Dockerfile` with *Tomcat Connector* configuration and *Keystore* folder mapping as it's required for Connector. Default stores and certificates from source code (`alfresco-repository`) have been copied in keystore folder.

* **docker-compose-ssl.yml** includes a Docker Composition for ACS 6.1 and Insight Engine 1.1 using Mutual TLS

* **docker-compose.yml** includes a Docker Composition for ACS 6.1 and Insight Engine 1.1 using Plain HTTP

* **solr6** includes a `Dockerfile` to set *https* communications and to provide a volume for the keystore. The keystore folder includes default certificates for SOLR server copied from source code (`alfresco-search`)

```
├── alfresco
│   ├── Dockerfile
│   └── keystore
│       ├── keystore
│       ├── keystore-passwords.properties
│       ├── ssl-keystore-passwords.properties
│       ├── ssl-truststore-passwords.properties
│       ├── ssl.keystore
│       └── ssl.truststore
├── docker-compose-ssl.yml
├── docker-compose.yml
└── solr6
    ├── Dockerfile
    └── keystore
        ├── ssl-keystore-passwords.properties
        ├── ssl-truststore-passwords.properties
        ├── ssl.repo.client.keystore
        └── ssl.repo.client.truststore
```


## Running Docker Compose

This project includes resources to start the platform in Plain HTTP or Mutual TLS (SSL).

**Plain HTTP**

Docker can be started using default command.

```bash
$ docker-compose up --build
```

Alfresco will be available at:

http://localhost:8080/alfresco

http://localhost:8080/share

http://localhost:8083/solr

Plain HTTP Communication from SOLR is targeted inside Docker Network to http://alfresco:8080/alfresco


**Mutual TLS (SSL)**

Docker can be started selecting SSL Docker Compose file.

```bash
$ docker-compose -f docker-compose-ssl.yml up --build
```

Alfresco will be available at:

http://localhost:8080/alfresco

https://localhost:8443/alfresco

http://localhost:8080/share

https://localhost:8083/solr

SSL Communication from SOLR is targeted inside Docker Network to https://alfresco:8443/alfresco
