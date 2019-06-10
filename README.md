## Alfresco Search Services and Insight Engine

This repository includes the source code for Alfresco Search Services and Insight Engine products.

**Alfresco Search Services** provides searching capabilities to Alfresco Content Services by leveraging [Apache SOLR](https://lucene.apache.org/solr/) core features. This product is used for both Enterprise and Community releases of Alfresco Content Services and it lives in [GitHub](https://github.com/Alfresco/SearchServices).

The official documentation for this product can be found at [Alfresco Search Services](https://docs.alfresco.com/search-community/concepts/search-home.html).

**Insight Engine** works together with *Alfresco Search Services* to provide extended capabilities to Alfresco Content Services (like a JDBC connector and Zeppelin integration). This product is licensed only in Enterprise mode, so the source code lives in our private *GitLab* and it's only available for Enterprise customers.

The official documentation for this product can be found at [Alfresco Search and Insight Engine](https://docs.alfresco.com/sie/concepts/Search-Insight-Engine-overview.html).


### Alfresco Search Services

Getting the code using a Git client.

```bash
$ git clone https://github.com/Alfresco/SearchServices.git
```

Build the project using Maven.

```bash
$ cd SearchServices/search-services
$ mvn clean install
```

All the resources required to run Alfresco Search Services will be available under `search-services/packaging/target` folder, including the distribution ZIP for local installations.

The Docker Image source code is available at `search-services/packaging/src/docker`. The building for the Docker Image is available in the public repository of Alfresco at Docker Hub:

[https://hub.docker.com/r/alfresco/alfresco-search-services](https://hub.docker.com/r/alfresco/alfresco-search-services)

*Note* The root `pom.xml` living in this folder is used for packaging Search Services and Insight Engine together. This file includes URLs not available for Community users, but it allows the `search-services` module without accessing to these resources.

More details are available at [search-services](/search-services) folder.

### Insight Engine

**Following resources will not be available for Community users**

More details are available at [insight-engine](/insight-engine) folder.
