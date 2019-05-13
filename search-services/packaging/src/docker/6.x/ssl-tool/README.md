# Generation Tool for custom SSL Certificates

This `run.sh` script generates certificates for Repository and SOLR SSL Communication:

* CA Entity to issue all required certificates (alias alfresco.ca)
* Server Certificate for Alfresco (alias ssl.repo)
* Server Certificate for SOLR (alias ssl.repo.client)

Sample `openssl.cnf` file is provided for CA Configuration.

## Execution

```bash
$ cd ssl-tool
$ ./run.sh
```

## Deployment

Once this script has been executed successfully, following resources are generated in ${KEYSTORES_DIR} folder:

```
keystores
├── alfresco
│   ├── keystore
│   ├── keystore-passwords.properties
│   ├── ssl-keystore-passwords.properties
│   ├── ssl-truststore-passwords.properties
│   ├── ssl.keystore
│   └── ssl.truststore
├── client
│   └── browser.p12
└── solr
    ├── ssl-keystore-passwords.properties
    ├── ssl-truststore-passwords.properties
    ├── ssl.repo.client.keystore
    └── ssl.repo.client.truststore
```

* `alfresco` files must be copied to "alfresco/keystore" folder in Docker Compose template project (any existing file must be overwritten)
* `solr` files must be copied to "solr6/keystore" folder and "zeppelin/keystore" folder (for Enterprise) in Docker Compose template project (any existing file must be overwritten)
* `client` files can be used from a browser to access the server using HTTPS in port 8443

## Dependencies

* **openssl** version (LibreSSL 2.6.5)
* **keytool** from openjdk version "11.0.2"
