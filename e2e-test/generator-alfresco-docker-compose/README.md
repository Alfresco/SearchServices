# generator-alfresco-docker-compose
> Alfresco Docker Compose Generator

This project generates a collection of Docker Compose Templates to test Repository and Search Services/Insight Engine with different configurations:

* Plain HTTP communications
* TLS/SSL Mutual Authentication communications
* Sharding (dynamic) with different Sharding Methods
* Replication (master/slave)

## Project structure

Following templates are provided.

```
$ tree generators/app/templates/
generators/app/templates/
├── 6.1
│   ├── .env
│   ├── docker-compose-ce.yml
│   └── docker-compose-ee.yml
├── 6.2
│   ├── .env
│   ├── docker-compose-ce.yml
│   └── docker-compose-ee.yml
├── images
│   ├── alfresco
│   │   ├── Dockerfile
│   │   └── model
│   │       ├── empty
│   │       ├── sharding-content-model-context.xml
│   │       └── sharding-content-model.xml
│   ├── search
│   │   └── Dockerfile
│   ├── share
│   │   ├── Dockerfile
│   │   └── model
│   │       ├── empty.xml
│   │       └── sharding-share-config-custom.xml
│   └── zeppelin
│       └── Dockerfile
└── keystores
    ├── alfresco
    ├── client
    ├── solr
    └── zeppelin
```

* `.env` includes default values for Docker Compose environment variables
* `docker-compose-ce.yml` is the base Docker Compose Template for Alfresco Community deployment (for ACS 6.2 and ACS 6.1)
* `docker-compose-ee.yml` is the base Docker Compose Template for Alfresco Enterprise deployment (for ACS 6.2 and ACS 6.1)
* `alfresco` includes a Dockerfile template to start Alfresco Repository
  * `model` includes a default content model (Sharding Explicit Routing or empty). This `empty` file is required for Dockerfile to work, so it should not be deleted.
* `share` includes a Dockerfile template to start Share Web Application
  * `model` includes a default forms model (Sharding Explicit Routing or empty)
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

## ACS Version

Currently supported ACS Versions are `6.2` and `6.1`

This is the first choice to be selected when the generator is executed.

```
? Which ACS version do you want to use?
  6.1
❯ 6.2
```
## AGS Version

Currently supported AGS Version is `latest` (based in ACS 6.1).

If you chose ACS 6.1, a prompt will allow you to use AGS.

```
? Would you like to use AGS? Yes
```

## Community

When using Community, some different options can be combined:

* Plain HTTP (http) or TLS/SSL Mutual Authentication (https)
* Use SOLR Replication in Master/Slave mode (only when using http)

```
? Would you like to use Alfresco enterprise or community? community
? Would you like to use http or https? http
? Would you like to use a SOLR Replication (2 nodes in master-slave)? Yes
```

## Enterprise

When using Enterprise, some options can be added to Community configuration:

* Use dynamic Sharding with 2 SOLR nodes pre-configured (every sharding method is supported)
* Insight Engine, as Search Services is selected by default
* Deploy Zeppelin app to use JDBC Connector to SOLR

```
? Would you like to use dynamic Sharding (2 SOLR nodes)? Yes
? Which Sharding Method do you want to use?
❯ DB_ID
  DB_ID_RANGE
  ACL_ID
  MOD_ACL_ID
  DATE
  PROPERTY
  LAST_REGISTERED_INDEXING_SHARD
  EXPLICIT_ID_FALLBACK_LRIS
  EXPLICIT_ID  
? Would you like to use Insight Engine instead of Search Services? Yes
? Would you like to deploy Zeppelin? Yes
```

## Sharding methods default parameters

**DB_ID_RANGE**
Shard range is 0-800 for the instance 0 and 801-40000 for the instance 1.

**DATE**
Shard property is `cm:created` and grouping is set to 2.

**PROPERTY**
Shard property is `shard:shardId`, belonging to a custom model deployed in the template.

**EXPLICIT_ID** and **EXPLICIT_ID_FALLBACK_LRIS**
Custom content model is deployed to provide a property, named `shard:shardId`, holding the Shard Number (0, 1) where the content is indexed.

This default configuration can be changed in the generated `docker-compose.yml` template.


## Deploying custom content models and forms

Custom content models and Share Form configurations can be added to deployment folders.

**Content models**

Custom content models can be copied to Repository deployment folder by using bootstrap approach. 

Following XML files must be created in `alfresco/model/` folder in the Docker Compose template generated:

* `content-model.xml` including an XML Alfresco Content Model file. Sample model is available in [images/alfresco/model/sharding-content-model.xml](generators/app/templates/images/alfresco/model/sharding-content-model.xml)
* `content-model-context.xml` including an XML Spring Bean file with the `dictionaryBootstrap` bean. Sample Spring Bean declaration is available in [images/alfresco/model/sharding-content-model-context.xml](generators/app/templates/images/alfresco/model/sharding-content-model-context.xml)

If *Sharding* is selected, these files will be available in deployment folder.

**Share forms**

Custom content forms can be added to Share configuration by modifying `share/model/share-config-custom-dev.xml` file in the Docker Compose template generated.

Sample configuration is available in [images/share/model/sharding-share-config-custom.xml](generators/app/templates/images/share/model/sharding-share-config-custom.xml)

If *Sharding* is selected, a default `share-config-custom-dev.xml` file with required forms configuration for Sharding custom model will be available in deployment folder. Add your configuration to this file.

## Configuration catalog

| Version    | Comms | Replication | Sharding | Explicit | Insight | Zeppelin |
| -          | -     | -           | -        | -        | -       | -        |
| community  | http  | -           | -        | -        | x       | x        |
| community  | http  | true        | x        | x        | x       | x        |
| community  | http  | false       | true     | false    | x       | x        |
| community  | http  | false       | true     | true     | x       | x        |
| community  | https | x           | -        | -        | x       | x        |
| enterprise | http  | -           | -        | -        | (*)     | (*)      |
| enterprise | http  | true        | x        | x        | (*)     | (*)      |
| enterprise | http  | false       | true     | false    | (*)     | (*)      |
| enterprise | http  | false       | true     | true     | (*)     | (*)      |
| enterprise | https | x           | -        | -        | (*)     | (*)      |
| enterprise | https | x           | true     | false    | (*)     | (*)      |
| enterprise | https | x           | true     | true     | (*)     | (*)      |

Both `community` and `enterprise` ACS deployments can be used with the same options, but `enterprise` may also use Insight Engine (replacing Search Services) and Insight Zeppelin services.

## Passing parameters from command line

Default values for options can be specified in the command line, using a `--name=value` pattern. When an options is specified in the command line, the question is not prompted to the user, so you can generate a Docker Compose template with no user interaction.

```
$ yo alfresco-docker-compose --acsVersion=6.2 --alfrescoVersion=community --httpMode=http --clustering=true
```

**Parameter names reference**

`--acsVersion`: default 6.2, but could be set to 6.1
`--ags:`: only available when acsVersion=6.1
`--alfrescoVersion`: community or enterprise
`--httpMode`: http or https
`--clustering`: true or false
`--insightEngine`: true or false
`--zeppelin`: true or false
`--sharding`: true or false
`--shardingMethod`: DB_ID, DB_ID_RANGE, ACL_ID, MOD_ACL_ID, DATE, PROPERTY, LAST_REGISTERED_INDEXING_SHARD, EXPLICIT_ID_FALLBACK_LRIS, EXPLICIT_ID

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

*HTTP*

http://localhost:8080/share

http://localhost:8082/alfresco

http://localhost:8083/solr

When using SOLR Replication, additionally

http://localhost:8084/solr


*SSL*

http://localhost:8080/share

http://localhost:8082/alfresco

https://localhost:8443/alfresco

https://localhost:8083/solr


**Enterprise URLs**

*HTTP*

http://localhost:8080/share

http://localhost:8080/alfresco

http://localhost:8083/solr

http://localhost:9090/zeppelin

When using SOLR Replication or Sharding, additionally

http://localhost:8084/solr


*SSL*

http://localhost:8080/share

http://localhost:8080/alfresco

https://localhost:8443/alfresco

https://localhost:8083/solr

http://localhost:9090/zeppelin

When using SOLR Sharding, additionally

https://localhost:8084/solr
