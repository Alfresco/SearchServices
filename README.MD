## Alfresco Solr 6 implementation
Alfresco Solr 6
### Get the code

Git:

	git clone https://gitlab.alfresco.com/search_discovery/solr.git

	cd solr

### Use Maven
1. Build

```
mvn clean install
```

### Docker
To build the docker image, first run the maven build then
```
cd packaging
./build.sh
```


### License
Copyright (C) 2016 Alfresco Software Limited

This file is part of an unsupported extension to Alfresco.

Alfresco Software Limited licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
