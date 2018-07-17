# Setup
The automated test project requires a running instance of ACS Repo and Search Services at the least.
To run Insight Engine tests, minimum version of Repo is ACS 6.0, additionally running instance of Insight Engine would be necessary too.
For more details about the related projects or their deployment see: [Search-Discovery] (https://git.alfresco.com/search_discovery) and (https://github.com/Alfresco/SearchServices).

# Prerequisites
Java 1.8

Maven 3.2.0

Alfresco Content Services 5.2.2 or above

Search Services 1.2.0 or above or Insight Engine 1.0.0 or above

# Bring the Test Environment up

1. ACS + Search Services Healthcheck: Please ensure that repo admin console > Search Services uses the right port and shows tracking status.
    
2. Solr Healthcheck can be performed using solr admin console at:

    <protocol>://<repo-host-ip>:<solr-port>/solr/#

    e.g. http://localhost:8983/solr/#

# Compile the project
`mvn clean install -DskipTests`

# Run the tests
`mvn clean install`

# Run a specific class of tests
`mvn clean install -Dtest=<class>`

e.g.

`mvn clean install -Dtest=org.alfresco.service.search.e2e.insightEngine.sql.CustomModelTest`
