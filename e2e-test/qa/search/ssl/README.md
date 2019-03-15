## About

Try to start Alfresco & Search Services with SSL enabled

### How to run it
>this will clean any volumes any docker services that are running
> and starts a new alfresco and search service(s) built with ssl enabled

```shell
$ make clean ssl
```

>There are also configuration on alfresco part - where we use the shared volume of search to take the same keystore data (see [docker-compose.ssl.yml](./docker-compose.ssl.yml) )

> clean everything with `make clean`

### Test it in browser

* open Firefox and import [browser.p12](./browser.p12) certificate (when asked add password: `alfresco`). I think you can also add an exception and pass unsecure connection warning!

| Alfresco        | Solr           | Share  |
| :------------- |:-------------| :-----|
| **SSL:** https://localhost:7070/alfresco | **SSL:** https://localhost:8084/solr | |
| **No-SSL:** http://localhost:8081/alfresco | **No-SSL:** -     |   **No-SSL:** http://localhost:8082/share/   |

>I've also enabled [spellcheck](./search/scripts/enable-spellcheck.sh) feature, so in Share, try to do a search for "[alfrezco](http://localhost:8082/share/page/dp/ws/faceted-search#searchTerm=alfrezco&scope=repo&sortField=null)"

