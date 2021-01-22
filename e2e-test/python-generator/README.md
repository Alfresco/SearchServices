# About
A python script to generator docker-compose files suitable for use with the automated tests in
the [Search and Insight E2E tests](https://git.alfresco.com/search_discovery/insightengine/tree/master/e2e-test).

# Installation
The script uses Python 3, which can be installed from [python.org](https://www.python.org/downloads/) or using
a package manager. You will also need the [yaml library](https://pypi.org/project/PyYAML/), which can be installed
using Pip if it is not already present:
```
pip3 install pyyaml
```

# Using the script
The script provides some help with the `-h` option:

```
python3 BuildScripts/generator/generator.py -h
usage: generator.py [-h] [-a ALFRESCO]...
...
```

For use with ACS 6.2.x the legacy transformers can be included:
```
python3 BuildScripts/generator/generator.py --alfresco=alfresco/alfresco-content-repository:6.2.0 --transformer=AIOTransformers
```

For ACS 6.0.x and ACS 6.1.x no external transformer is needed:
```
python3 BuildScripts/generator/generator.py --alfresco=alfresco/alfresco-content-repository:6.1.0
```

For ACS 6.0.0.x and earlier ActiveMQ can be excluded:
```
python3 BuildScripts/generator/generator.py --alfresco=alfresco/alfresco-content-repository:6.0.0.3 --excludeAMQ
```

For ACS 5.2.x the legacy LibreOffice transformer can be used:
```
python3 BuildScripts/generator/generator.py --alfresco=quay.io/alfresco/alfresco-content-repository-52:5.2.5 --postgres=postgres:9.4  --excludeAMQ --transformer=LibreOffice
```

# Starting the containers
To start the containers you also need to build the images - for example:
```
docker-compose up --build --force-recreate
```
