## About

Starting automatically Benchmark infrastructure:
* [BM Manager](https://github.com/Alfresco/alfresco-bm-manager) 
* [BM Drivers](https://github.com/Alfresco/alfresco-bm-manager/tree/master/docs/bm-driver)
  * running create [users](https://github.com/Alfresco/alfresco-bm-load-users)
  * running create [data](https://github.com/Alfresco/alfresco-bm-load-data)
  * running [rest test](https://github.com/Alfresco/alfresco-bm-rest-api)

## Prerequisites
* ACS deployed in AWS (EKS or Docker)
* Benchmark knowledge (check the official documentation above)

## How to use it?
### a) start BM Manager and Drivers locally

```shell
$ make start-all
```
>check the official documentation how to use the benchmark

### b) prepare data for benchmarking
> we need to create users/sites and load some content in order to tests the search performance

```shell
$ make ALFRESCO_URL=<http://alserver:port> benchmark-prepare
```
>this will call some shell scripts

### c) run the actual benchmark tests
>you can tweak the benchmark tests as you want in the shell script executed by this task

```shell
$ make ALFRESCO_URL=<http://alserver:port> benchmark-run
```

### `!` Bamboo Build
We defined a sad benchmark build: [SAD-BEN](https://bamboo.alfresco.com/bamboo/browse/SAD-BEN) that will execute the steps above automatically for you.

If you run it customized, you can override the `ALFRESCO_URL` variable to point to a deployed ACS instance (default to: "http://ec2-34-245-148-19.eu-west-1.compute.amazonaws.com:8081") 

At the end of the tests you will have access to Excel reports (the actual benchmark results)