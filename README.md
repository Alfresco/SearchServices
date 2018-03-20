## Alfresco Search Services Implementation

Alfresco Search Services using Alfresco and Apache Solr

### Get the code

Git:

<code>
git clone https://github.com/Alfresco/SearchServices.git
</code>

### Use Maven
Build project:

<code>
mvn clean install
</code>

All the resources needed for the docker image will be available under packaging/target/docker-resources/

### Docker
To build the docker image:

<code>
cd packaging/target/docker-resources/

docker build -t searchservices:develop .
</code>

To run the docker image:

<code>
docker run -p 8983:8983 searchservices:develop
</code>

docker-compose files can be used to start up Search Services with Alfresco and Share. There are two docker-composes files available. Depending on the version you want to start either change to 5.x or 6.x. E.g.

<code>
cd packaging/target/docker-resources/6.x

docker-compose up 
</code>

This will start up Alfresco, Postgres, Share and SearchServices. You can access the applications using the following URLs:

 * Alfresco: http://localhost:8081/alfresco
 * Share: http://localhost:8082/share
 * Solr: http://localhost:8083/solr
 
If you start version 5.x instead you can also access the API Explorer:

 * API Explorer: http://localhost:8084/api-explorer

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
