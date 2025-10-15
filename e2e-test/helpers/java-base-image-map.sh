#!/usr/bin/env bash

if [ -n "$JAVA_RUNTIME_VERSION" ]; then
  declare -A JAVA_IMAGE_MAP=(
    [21]="alfresco/alfresco-base-java:jre21-rockylinux9@sha256:71bbe6f5f7ac280f2c181561f1ba49230673484bb1dd44bbf17499abbf930491"
    [25]="alfresco/alfresco-base-java:jre25-rockylinux9@sha256:7cc61edc5444e0eb69cfbf5d51716666ab8605fe9c8fdba75407e9484185fcd9"
  )
  JAVA_BASE_IMAGE="${JAVA_IMAGE_MAP[$JAVA_RUNTIME_VERSION]}"
  export JAVA_BASE_IMAGE
fi
