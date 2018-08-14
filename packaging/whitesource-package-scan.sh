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

nicebranch=`echo "$bamboo_planRepository_1_branch" | sed 's/\//_/'`

if [ "${nicebranch}" = "master" ] || [ "${nicebranch#release}" != "${nicebranch}" ]
then  
    POM_VERSION=$(grep version pom.xml | grep -v -e '<?xml|~'| head -n 1 |awk -F '[><]' '{print $3}')
    RELEASE_FOLDER=/data/releases/SearchServices/${POM_VERSION}
    DISTRIBUTION_NAME=alfresco-search-services-${POM_VERSION}.zip
    DISTRIBUTION_ZIP_PATH=${RELEASE_FOLDER}/${DISTRIBUTION_NAME}
    DISTRIBUTION_ZIP_SCAN_PATH=${RELEASE_FOLDER}/scan
    CLEANUP="${1:-do-not-clean}"

    if [ ${CLEANUP} = "clean" ]; then
        echo "Cleaning up scan folder..."
        ssh -q tomcat@pbam01.alfresco.com [[ -d ${RELEASE_FOLDER} ]] && ssh tomcat@pbam01.alfresco.com rm -rf ${RELEASE_FOLDER} || echo "Nothing to cleanup"                
    else
        echo "Copy distribution to release area..."
        ssh tomcat@pbam01.alfresco.com mkdir -p ${RELEASE_FOLDER}
        scp target/${DISTRIBUTION_NAME} tomcat@pbam01.alfresco.com:${RELEASE_FOLDER}

        #unzip distribution
        ssh tomcat@pbam01.alfresco.com unzip ${DISTRIBUTION_ZIP_PATH} -d ${DISTRIBUTION_ZIP_SCAN_PATH}

        #whitesource scanning using file agent: https://goo.gl/ohg4Rv
        ssh tomcat@pbam01.alfresco.com sh /etc/bamboo/whitesource-agent.sh -d ${DISTRIBUTION_ZIP_SCAN_PATH} -project distribution-zip -product SearchServices-${bamboo_release_version}
    fi

else
    echo "WhiteSource scann will be executed only from master or release branches. Skipping for '${nicebranch}'"
fi