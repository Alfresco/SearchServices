#!/usr/bin/env sh
# Usage: whitesource-package-scan.sh <clean>
#   - this will copy the distribution zip to release area
#   - will unzip it and scan if using WhiteSource File System Agent https://goo.gl/ohg4Rv 
#   - and will clean up the scan folder if <clean> string is passed as parameter
# Example:
#   $ whitesource-package-scan.sh       -> perform the scan
#   $ whitesource-package-scan.sh clean -> will cleanup the scan folder

echo `basename $0` called on `date` with arguments: "$@"
set -exu

POM_VERSION=$(grep version pom.xml | grep -v -e '<?xml|~'| head -n 1 |awk -F '[><]' '{print $3}')
RELEASE_FOLDER=/data/releases/SearchServices/${bamboo_release_version}
DISTRIBUTION_NAME=alfresco-search-services-${POM_VERSION}.zip
DISTRIBUTION_ZIP_PATH=${RELEASE_FOLDER}/${DISTRIBUTION_NAME}
DISTRIBUTION_ZIP_SCAN_PATH=${RELEASE_FOLDER}/scan
CLEANUP="${1:-do-not-clean}"

if [ ${CLEANUP} = "clean" ]; then
    echo "Cleaning up scan folder..."
    ssh tomcat@pbam01.alfresco.com rm -rf ${DISTRIBUTION_ZIP_SCAN_PATH}
    ssh tomcat@pbam01.alfresco.com rm -rf ${DISTRIBUTION_ZIP_PATH}
else
    echo "Copy distribution to release area..."
    ssh tomcat@pbam01.alfresco.com mkdir -p ${RELEASE_FOLDER}
    scp target/${DISTRIBUTION_NAME} tomcat@pbam01.alfresco.com:${RELEASE_FOLDER}

    #unzip distribution
    ssh tomcat@pbam01.alfresco.com unzip ${DISTRIBUTION_ZIP_PATH} -d ${DISTRIBUTION_ZIP_SCAN_PATH}

    #whitesource scanning using file agent: https://goo.gl/ohg4Rv
    ssh tomcat@pbam01.alfresco.com sh /etc/bamboo/whitesource-agent.sh -d ${DISTRIBUTION_ZIP_SCAN_PATH} -project distribution-zip -product SearchServices-${bamboo_release_version}
fi
