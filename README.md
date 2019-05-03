## Alfresco Search Services Implementation

Alfresco Search Services using Alfresco and Apache Solr

### Get the code

Git:

```bash
git clone https://github.com/Alfresco/SearchServices.git
```

### Use Maven
Build project:

```bash
mvn clean install
```

All the resources needed for the docker image will be available under packaging/target/docker-resources/

### Start Alfresco Search Services from source
To run Alfresco Search Services locally first build the zip file using:

```bash
mvn clean install
```

Extract the zip file and launch Alfresco Search Services using:

```bash
cd packaging/target
unzip alfresco-search-services-*.zip
cd alfresco-search-services/solr
./bin/solr start -Dcreate.alfresco.defaults=alfresco,archive
```

If you also start an ACS instance then index will be populated.  By default Alfresco Search Services runs on port 8983, but this can be set by supplying e.g. `-p 8083` to the "solr start" command.

To set up remote debugging (on port 5005) start Alfresco Search Services with the following command and then connect using your IDE:

```bash
./bin/solr start -a "-Dcreate.alfresco.defaults=alfresco,archive -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"
```

To stop Alfresco Search Services:

```bash
./bin/solr stop
```

### Docker
To build the docker image:

```bash
cd packaging/target/docker-resources/
docker build -t searchservices:develop .
```
To Build Master or slave image locally
```bash
docker build -t searchservices-slave:develop --build-arg REPLICATION_TYPE=slave .

docker build -t searchservices-master:develop --build-arg REPLICATION_TYPE=master .
```
Based on the Argument REPLICATION_TYPE, the solrconfig has been configured while building image via bash script.

To run the docker image:

```bash
docker run -p 8983:8983 searchservices:develop

docker run -p 8983:8983 searchservices-master:develop

docker run -p 8984:8984 searchservices-slave:develop
```

To pass an environment variable:

```bash
docker run -e SOLR_JAVA_MEM=“-Xms4g -Xmx4g” -p 8983:8983 searchservices:develop
```

To pass several environment variables (e.g. SOLR\_ALFRESCO\_HOST, SOLR\_ALFRESCO\_PORT, SOLR\_SOLR\_HOST, SOLR\_SOLR\_PORT, SOLR\_CREATE\_ALFRESCO\_DEFAULTS, SOLR\_HEAP, etc.):

```bash
docker run -e SOLR_ALFRESCO_HOST=localhost -e SOLR_ALFRESCO_PORT=8080 -p 8983:8983 searchservices:develop
```

docker-compose files can be used to start up Search Services with Alfresco and Share. There are two docker-composes files available. Depending on the version you want to start either change to 5.x or 6.x. E.g.

```bash
cd packaging/target/docker-resources/6.x
docker-compose up
```

This will start up Alfresco, Postgres, Share and SearchServices. You can access the applications using the following URLs:

 * Alfresco: http://localhost:8081/alfresco
 * Share: http://localhost:8082/share
 * Solr: http://localhost:8083/solr
 * Solr-slave: http://localhost:8084/solr
 
If you start version 5.x instead you can also access the API Explorer:

 * API Explorer: http://localhost:8084/api-explorer

### Docker Master-Slave setup
We have seperate docker compose file for slave. To setup Master slave setup
```bash
docker-compose -f docker-compose.yml -f ./master-slave/docker-compose.slave.yml up
```
The slave running behind the load balancer under 8084, so we can spin up multiple slave with the same port. To deploy multiple slaves

```bash
docker-compose -f docker-compose.yml -f ./master-slave/docker-compose.slave.yml up --scale search_slave=2
```
### License
Copyright (C) 2005 - 2017 Alfresco Software Limited

This file is part of the Alfresco software.
If the software was purchased under a paid Alfresco license, the terms of
the paid license agreement will prevail.  Otherwise, the software is
provided under the following open source license terms:

Alfresco is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Alfresco is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
