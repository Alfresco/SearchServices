# generator-alfresco-docker-compose
> Alfresco Docker Compose Generator

This project generates a collection of Docker Compose Templates to test Repository and Search Services/Insight Engine with different configurations:

* Plain HTTP communications
* TLS/SSL Mutual Authentication communications
* Sharding (dynamic)
* Replication (master/slave)

## Project structure

Following templates are provided.

```
$ tree generators/app/templates/6.1
├── .env
├── alfresco
│   └── Dockerfile
├── docker-compose-ce.yml
├── docker-compose-ee.yml
├── search
│   └── Dockerfile
└── zeppelin
|   └── Dockerfile
└── keystores
    ├── alfresco
    ├── solr
    └── zeppelin
```

* `.env` includes default values for Docker Compose environment variables
* `docker-compose-ce.yml` is the base Docker Compose Template for Alfresco Community deployment
* `docker-compose-ee.yml` is the base Docker Compose Template for Alfresco Enterprise deployment
* `alfresco` includes a Dockerfile template to start Alfresco Repository
* `search` includes a Dockerfile template to start Search Services and Insight Engine
* `zeppelin` includes a Dockerfile template to start Zeppelin with SSL
* `keystores` includes every truststore and keystore required for SSL configuration


## Installation

First, install [Yeoman](http://yeoman.io) and link `generator-alfresco-docker-composel` using [npm](https://www.npmjs.com/) (we assume you have pre-installed [node.js](https://nodejs.org/)).

```bash
$ npm install -g yo
$ npm link
```

Then generate your new project:

```bash
$ mkdir my-custom-docker-compose
$ yo alfresco-docker-compose
```

## Community

When using Community, Plain HTTP or TLS/SSL Mutual Auth can be selected.

Additionally, if Plain HTTP is selected, SOLR Replication can be added.

```
? Which Alfresco version do you want to use? 6.1
? Would you like to use Alfresco enterprise or community? community
? Would you like to use http or https? http
? Would you like to use a SOLR Cluster (2 nodes in master-slave)? Yes
```

## Enterprise

When using Enterprise, some different options can be combined:

* Plain HTTP (http) or TLS/SSL Mutual Auth (https)
* Use SOLR Replication in Master/Slave mode (only for http)
* Insight Engine, as Search Services is selected by default
* Deploy Zeppelin app to use JDBC Connector to SOLR
* Use dynamic Sharding with 2 SOLR nodes pre-configured (only when not using SOLR Replication)

```
? Which Alfresco version do you want to use? 6.1
? Would you like to use Alfresco enterprise or community? enterprise
? Would you like to use http or https? https
? Would you like to use Insight Engine instead of Search Services? Yes
? Would you like to deploy Zeppelin? Yes
? Would you like to use dynamic Sharding (2 SOLR nodes)? Yes
? Would you like to use a SOLR Cluster (2 nodes in master-slave)? Yes
```

## Passing parameters from command line

Default values for options can be specified in the command line, using a `--name=value` pattern. When an options is specified in the command line, the question is not prompted to the user, so you can generate a Docker Compose template with no user interaction.

```
$ yo alfresco-docker-compose --acsVersion=6.1 --alfrescoVersion=community --httpMode=http --clustering=true
```

**Parameter names reference**

`--acsVersion`: currently only accepting 6.1
`--alfrescoVersion`: community or enterprise
`--httpMode`: http or https
`--clustering`: true or false
`--insightEngine`: true or false
`--zeppelin`: true or false
`--sharding`: true or false


## Using Docker Compose

Once the files have been generated, just start Docker Compose.

```
$ docker-compose up --build --force-recreate
```

You can shutdown it at any moment using following command.

```
$ docker-compose down
```

**Community URLs**

HTTP

http://localhost:8080/share

http://localhost:8082/alfresco

http://localhost:8083/solr

When using SOLR Replication, additionally

http://localhost:8084/solr


SSL

http://localhost:8080/share

http://localhost:8082/alfresco

https://localhost:8443/alfresco

https://localhost:8083/solr

**Enterprise URLs**

HTTP

http://localhost:8080/share

http://localhost:8080/alfresco

http://localhost:8083/solr

http://localhost:9090/zeppelin

When using SOLR Replication, additionally

http://localhost:8084/solr


SSL

http://localhost:8080/share

http://localhost:8080/alfresco

https://localhost:8443/alfresco

https://localhost:8083/solr

http://localhost:9090/zeppelin


## License

LGPL-3.0 © [Angel Borroy]()
