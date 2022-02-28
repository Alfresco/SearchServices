# About

Testing the Upgrade of SearchService product

**Build Plan:** https://bamboo.alfresco.com/bamboo/browse/SAD-QAUP

![](docs/upgrade.png?raw=true)

# Steps

* **a)** start the initial version
```shel
make set_version=1.2.1 as-previous wait
```
>notice that new folders will appear on you "upgrade" folder with data from container(s)

* **b)** create some data manually or using automated tests found on this project
```shel
make run-mvn-tests suiteXmlFile=./src/test/resources/search-pre-upgrade-suite.xml
```
* **c)** now upgrade to new version
```shel
make set_version=2.0.x as-current wait
```
* **d)** and test that upgrade data exist
```shel
make run-mvn-tests suiteXmlFile=./src/test/resources/search-post-upgrade-suite.xml
```

# Environment Settings
Pay attention at the values that exist in [.env](.env) file. These settings will be picked up in custom docker-compose.*.yml file(s)
