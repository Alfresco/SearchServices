# How to execute it

**a)** you can run Alfresco && Search Services using default configuration from `docker-compose.yml` file
```shell
$  docker-compose up
```

**b)** you can start Alfresco && Search Services with custom configuration that will override the default `docker-compose.yml` with the services found in `docker-compose.custom.yml` file

**1)** build the custom image, overriding the original docker-compose.yml file
```shell
$ make SCRIPTS_FOLDER=spellcheck build
```
* `SCRIPTS_FOLDER`: You can pass any folder found in ./custom

This command will:
* build the new `quay.io/alfresco/search-services-custom:${SEARCH_TAG}` image based on [./custom/Dockerfile](./custom/Dockerfile) file that will:
  * override the `search` service from default [docker-compose.yml](./docker-compose.yml) file
  * start the solr in background
  * execute all shell scripts inside $DIST_DIR/scripts (make sure you `chmod +x <file>` to each of shell scripts)
  * and tail on $DIST_DIR/logs/solr.log"

>The advantage of this approach is that we can define scripts inside `./custom/<my-setting` folder with any scripts that we want to enable/disable `my-settings`.

**2)** start the compose with the new custom image built
```shell
$ make up
```

**3)** stop && remove any container

```shell
$ make clean
```
