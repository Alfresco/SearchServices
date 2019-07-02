# About

Start Search Service with a custom configuration

# Steps

* **a)** under `custom` folder create a new folder that will hold all settings 
>checkout [spellcheck](.spellcheck) folder for example

>add here any shell scripts that will enable/disable a particular setting

* **b)** build the new image setting SCRIPTS_FOLDER to you folder already created
```shel
make SCRIPTS_FOLDER=spellcheck build
```
>notice that out [docker-compose.custom.yml](.custom/docker-compose.custom.yml) file is using a [Dockerfile](.custom/Dockerfile) to built you new image.
> at runtime, all shell scripts from your folder are executed and the settings are applied.

* **c)** the image is built locally, now start it up
```shel
make start
```

# Environment Settings
Pay attention at the values that exist in [.env](.env) file. These settings will be picked up in custom docker-compose.*.yml file(s)
