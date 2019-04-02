# About

Start Alfresco services and scale SOLR to multiple instances, behind a LB.

# Steps

* **a)** Start Alfresco

```
docker-compose up -d
```

* **b)** Scale SOLR to 2 instances

```
docker-compose scale solr=2
```

>it's possible at this time to restart `alfresco` service if there are not results returned by LB
```
  docker-compose restart alfresco
  ```