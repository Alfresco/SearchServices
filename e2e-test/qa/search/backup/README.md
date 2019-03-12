# About

Testing the Backup of SearchService product

**Build Plan:** https://bamboo.alfresco.com/bamboo/browse/SAD-QAB

![](docs/backup.png?raw=true)

# Steps

* **a)** prepare the backup
```shel
make backup-prepare wait
```
>more details on Makefile [task](Makefile#L27).

* **b)** create some data manually or using automated tests found on this project
```shel
make run-mvn-tests suiteXmlFile=./src/test/resources/search-pre-backup-suite.xml
```

* **c)** perform the backup of data
```shel
make backup-perform wait
```
* **d)** now you can also update the data/remove it from TS, or even remove the entire volumes
```shel
make run-mvn-tests suiteXmlFile=./src/test/resources/search-on-backup-suite.xml
# or
make clean
```
* **e)** at any time you can restore the backup
```shel
make backup-restore wait
```
* **f)** now you can check the data from point **b)** is corectly recovered
```shel
make run-mvn-tests suiteXmlFile=./src/test/resources/search-post-backup-suite.xml
```

# All in one
At any time you can run the `make all` taks that will execute all the above commands for you

```shel
make all
```

# Environment Settings
Pay attention at the values that exist in [.env](.env) file. These settings will be picked up in custom docker-compose.*.yml file(s)
