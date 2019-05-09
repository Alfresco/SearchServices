#! /bin/sh

# This script generates certificates for Repository and SOLR SSL Communication:
#
# * CA Entity to issue all required certificates (alias alfresco.ca)
# * Server Certificate for Alfresco (alias ssl.repo)
# * Server Certificate for SOLR (alias ssl.repo.client)
#
# Sample "openssl.cnf" file is provided for CA Configuration.
#
# Once this script has been executed successfully, following resources are generated in ${KEYSTORES_DIR} folder:
#
# .
# ├── alfresco
# │   ├── ssl-keystore-passwords.properties
# │   ├── ssl-truststore-passwords.properties
# │   ├── ssl.keystore
# │   └── ssl.truststore
# ├── client
# │   └── browser.p12
# └── solr
#     ├── ssl-keystore-passwords.properties
#     ├── ssl-truststore-passwords.properties
#     ├── ssl.repo.client.keystore
#     └── ssl.repo.client.truststore
#
# "alfresco" files must be copied to "alfresco/keystore" folder
# "solr" files must be copied to "keystore"
# "client" files can be used from a browser to access the server using HTTPS in port 8443

# Dependencies:
# * openssl version (LibreSSL 2.6.5)
# * keytool from openjdk version "11.0.2"

# PARAMETERS

# Distinguished name of the CA
CA_DNAME="/C=GB/ST=UK/L=Maidenhead/O=Alfresco Software Ltd./OU=Unknown/CN=Custom Alfresco CA"
# Distinguished name of the Server Certificate for Alfresco
REPO_CERT_DNAME="/C=GB/ST=UK/L=Maidenhead/O=Alfresco Software Ltd./OU=Unknown/CN=Custom Alfresco Repository"
# Distinguished name of the Server Certificate for SOLR
SOLR_CLIENT_CERT_DNAME="/C=GB/ST=UK/L=Maidenhead/O=Alfresco Software Ltd./OU=Unknown/CN=Custom Alfresco Repository Client"

# RSA key length
KEY_SIZE=1024

# Default password for every store and key
PASS=kT9X6oe68t

# Encryption secret key passwords
ENC_STORE_PASS=password
ENC_METADATA_PASS=password

# Folder where keystores, truststores and cerfiticates are generated
KEYSTORES_DIR=keystores
ALFRESCO_KEYSTORES_DIR=keystores/alfresco
SOLR_KEYSTORES_DIR=keystores/solr
CLIENT_KEYSTORES_DIR=keystores/client

# SCRIPT

# Remove previous working directories and certificates
rm -rf ca
rm -rf ${KEYSTORES_DIR}
rm repository.*
rm solr.*
rm ssl.*

# Generate a new CA Entity
mkdir ca

mkdir ca/certs ca/crl ca/newcerts ca/private
chmod 700 ca/private
touch ca/index.txt
echo 1000 > ca/serial

openssl genrsa -aes256 -passout pass:$PASS -out ca/private/ca.key.pem $KEY_SIZE
chmod 400 ca/private/ca.key.pem

openssl req -config openssl.cnf \
      -key ca/private/ca.key.pem \
      -new -x509 -days 7300 -sha256 -extensions v3_ca \
      -out ca/certs/ca.cert.pem \
      -subj "$CA_DNAME" \
      -passin pass:$PASS
chmod 444 ca/certs/ca.cert.pem

# Generate Server Certificate for Alfresco (issued by just generated CA)
openssl req -newkey rsa:$KEY_SIZE -nodes -out repository.csr -keyout repository.key -subj "$REPO_CERT_DNAME"
openssl ca -config openssl.cnf -extensions server_cert -passin pass:$PASS -batch -notext -in repository.csr -out repository.cer
openssl pkcs12 -export -out repository.p12 -inkey repository.key -in repository.cer -password pass:$PASS -certfile ca/certs/ca.cert.pem

# Server Certificate for SOLR (issued by just generated CA)
openssl req -newkey rsa:$KEY_SIZE -nodes -out solr.csr -keyout solr.key -subj "$SOLR_CLIENT_CERT_DNAME"
openssl ca -config openssl.cnf -extensions server_cert -passin pass:$PASS -batch -notext -in solr.csr -out solr.cer
openssl pkcs12 -export -out solr.p12 -inkey solr.key -in solr.cer -certfile ca.cer -password pass:$PASS -certfile ca/certs/ca.cert.pem

# Create folders for truststores, keystores and certificates
mkdir ${KEYSTORES_DIR}
mkdir ${ALFRESCO_KEYSTORES_DIR}
mkdir ${SOLR_KEYSTORES_DIR}
mkdir ${CLIENT_KEYSTORES_DIR}

#
# ALFRESCO
#

# Include CA and SOLR certificates in Alfresco Truststore
keytool -import -trustcacerts -noprompt -alias alfresco.ca -file ca/certs/ca.cert.pem \
-keystore ${ALFRESCO_KEYSTORES_DIR}/ssl.truststore -storetype JCEKS -storepass $PASS

keytool -importcert -noprompt -alias ssl.repo.client -file solr.cer \
-keystore ${ALFRESCO_KEYSTORES_DIR}/ssl.truststore -storetype JCEKS -storepass $PASS

# Include Alfresco Certificate in Alfresco Keystore
# Also adding CA Certificate for historical reasons
keytool -importkeystore \
-srckeystore repository.p12 -destkeystore ${ALFRESCO_KEYSTORES_DIR}/ssl.keystore \
-srcstoretype PKCS12 -deststoretype JCEKS \
-srcstorepass $PASS -deststorepass $PASS \
-srcalias 1 -destalias ssl.repo \
-srckeypass $PASS -destkeypass $PASS \
-noprompt

keytool -importcert -noprompt -alias ssl.alfresco.ca -file ca/certs/ca.cert.pem \
-keystore ${ALFRESCO_KEYSTORES_DIR}/ssl.keystore -storetype JCEKS -storepass $PASS

# Generate Encryption Secret Key
keytool -genseckey -alias metadata -keypass $ENC_METADATA_PASS -storepass $ENC_STORE_PASS -keystore ${ALFRESCO_KEYSTORES_DIR}/keystore \
-storetype JCEKS -keyalg DESede

# Create Alfresco stores password files
ECHO "aliases=alfresco.ca
keystore.password=$PASS
alfresco.ca.password=$PASS" > ${ALFRESCO_KEYSTORES_DIR}/ssl-truststore-passwords.properties

ECHO "aliases=ssl.alfresco.ca,ssl.repo
keystore.password=$PASS
ssl.repo.password=$PASS
ssl.alfresco.ca.password=$PASS" > ${ALFRESCO_KEYSTORES_DIR}/ssl-keystore-passwords.properties

ECHO "aliases=metadata
keystore.password=$ENC_METADATA_PASS
metadata.keyData=
metadata.algorithm=DESede
metadata.password=$ENC_STORE_PASS" > ${ALFRESCO_KEYSTORES_DIR}/keystore-passwords.properties

#
# SOLR
#

# Include CA and Alfresco certificates in SOLR Truststore
keytool -import -trustcacerts -noprompt -alias ssl.alfresco.ca -file ca/certs/ca.cert.pem \
-keystore ${SOLR_KEYSTORES_DIR}/ssl.repo.client.truststore -storetype JCEKS -storepass $PASS

keytool -importcert -noprompt -alias ssl.repo -file repository.cer \
-keystore ${SOLR_KEYSTORES_DIR}/ssl.repo.client.truststore -storetype JCEKS -storepass $PASS

# Include SOLR Certificate in SOLR Keystore
# Also adding CA Certificate for historical reasons
keytool -importkeystore \
-srckeystore solr.p12 -destkeystore ${SOLR_KEYSTORES_DIR}/ssl.repo.client.keystore \
-srcstoretype PKCS12 -deststoretype JCEKS \
-srcstorepass $PASS -deststorepass $PASS \
-srcalias 1 -destalias ssl.repo.client \
-srckeypass $PASS -destkeypass $PASS \
-noprompt

keytool -importcert -noprompt -alias alfresco.ca -file ca/certs/ca.cert.pem \
-keystore ${SOLR_KEYSTORES_DIR}/ssl.repo.client.keystore -storetype JCEKS -storepass $PASS

# Create SOLR stores password files
ECHO "aliases=alfresco.ca
keystore.password=$PASS
alfresco.ca.password=$PASS" > ${SOLR_KEYSTORES_DIR}/ssl-truststore-passwords.properties

ECHO "aliases=ssl.alfresco.ca,ssl.repo
keystore.password=$PASS
ssl.repo.password=$PASS
ssl.alfresco.ca.password=$PASS" > ${SOLR_KEYSTORES_DIR}/ssl-keystore-passwords.properties

#
# CLIENT
#

# Create client certificate
keytool -importkeystore -srckeystore ${ALFRESCO_KEYSTORES_DIR}/ssl.keystore -srcstorepass $PASS -srcstoretype JCEKS -srcalias ssl.repo \
-srckeypass $PASS -destkeystore ${CLIENT_KEYSTORES_DIR}/browser.p12 -deststoretype pkcs12 -deststorepass $PASS \
-destalias ssl.repo -destkeypass $PASS
