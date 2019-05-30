# generator-alfresco-docker-compose
> Alfresco Docker Compose Generator

This project generates a collection of Docker Compose Templates to test Repository and Search Services/Insight Engine with different configurations:

* Plain HTTP communications
* TLS/SSL Mutual Authentication communications
* Sharding (dynamic)

## Project structure

Following templates are provided.

```
$ tree generators/app/templates/
generators/app/templates/
├── .env
├── alfresco-https
│   └── Dockerfile
├── docker-compose-ce.yml
├── docker-compose-ee.yml
├── search-https
│   └── Dockerfile
├── sharding-https
│   └── Dockerfile
├── sharding-none
│   └── Dockerfile
├── zeppelin-https
│   └── Dockerfile
└── keystores
    ├── alfresco
    ├── solr
    └── zeppelin
```

* `.env` includes default values for Docker Compose environment variables
* `docker-compose-ce.yml` is the base Docker Compose Template for Alfresco Community deployment
* `docker-compose-ee.yml` is the base Docker Compose Template for Alfresco Enterprise deployment
* `alfresco-https` includes a Dockerfile template to start Alfresco Repository with SSL
* `search-https` includes a Dockerfile template to start Search Services and Insight Engine with SSL
* `zeppelin-https` includes a Dockerfile template to start Zeppelin with SSL
* `sharding-none` includes a Dockerfile template for dynamic Sharding in Plain HTTP
* `sharding-https` includes a Dockerfile template for dynamic Sharding with SSL
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

```
? Which Alfresco version do you want to use? 6.1
? Would you like to use Alfresco enterprise or community? community
? Would you like to use http or https? http
```

## Enterprise

When using Enterprise, some different options can be combined:

* Plain HTTP (http) or TLS/SSL Mutual Auth (https)
* Insight Engine, as Search Services is selected by default
* Deploy Zeppelin app to use JDBC Connector to SOLR
* Use dynamic Sharding with 2 SOLR nodes pre-configured

```
? Which Alfresco version do you want to use? 6.1
? Would you like to use Alfresco enterprise or community? enterprise
? Would you like to use http or https? https
? Would you like to use Insight Engine instead of Search Services? Yes
? Would you like to deploy Zeppelin? Yes
? Would you like to use dynamic Sharding (2 SOLR nodes)? Yes
```

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

SSL

http://localhost:8080/share

http://localhost:8080/alfresco

https://localhost:8443/alfresco

https://localhost:8083/solr

http://localhost:9090/zeppelin


## License

LGPL-3.0 © [Angel Borroy]()
