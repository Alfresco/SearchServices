# How to execute it

a) you can run Alfresco && Search Services using default configuration from `docker-compose.yml` file

```shell
$  docker-compose up
```

b) you can start Alfresco && Search Services with custom configuration that will override the default `docker-compose.yml` with the services found in `docker-compose.custom.yml` file

b-1) start with default scripts of docker-compose.custom.yml
```shell
$  docker-compose -f docker-compose.yml -f docker-compose.custom.yml up --build --force-recreate
```

* `--build`: because I want to build the custom search image with my scripts.
* `--force-recreate`: I don't want recreate the custom container, not using from catche.

b-2) start with new scripts of docker-compose.custom.yml

```shell
$  docker-compose -f docker-compose.yml -f docker-compose.custom.yml up --build-arg SCRIPTS_FOLDER=my-precious --build --force-recreate
```
*  `--build-arg SCRIPTS_FOLDER=my-precious` : will start a custom search image with all the scripts from `custom/my-precious` folder


>Hint:
>I also use this command to cleanup the volumes before building it.
>```shell
>$ docker-compose -f docker-compose.custom.yml rm -fv
>```


This command will:
* build the new `quay.io/alfresco/search-services-custom:${SEARCH_TAG}` image based on [./custom/Dockerfile](./custom/Dockerfile) file that will:
  * override the `search` service from default [docker-compose.yml](./docker-compose.yml) file
  * start the solr in background
  * execute all shell scripts inside $DIST_DIR/scripts (make sure you `chmod +x <file>` to each of shell scripts)
  * and tail on $DIST_DIR/logs/solr.log"

>The advantage of this approach is that we can define scripts inside `./custom<my-settings` folder with any scripts that we want to enable/disable `my-setting`.
