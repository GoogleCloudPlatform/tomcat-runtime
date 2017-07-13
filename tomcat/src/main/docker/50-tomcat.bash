#!/bin/bash

# If we are deploying on a GAE platform, enable the GCP module
if [ "$PLATFORM" == "gae" ]; then
  TOMCAT_MODULE_ENABLE="$TOMCAT_MODULE_ENABLE,gcp"
fi

if [ -n "$TOMCAT_MODULE_ENABLE" ]; then
  echo "$TOMCAT_MODULE_ENABLE" | tr ',' '\n' | while read module; do
    if [ -r "/config/${module}.xml" ]; then
      cp "/config/${module}.xml" "${CATALINA_BASE}/conf/${module}.xml"
    fi
  done
fi

# Add all the user defined properties to catalina.properties
if [ -n "$TOMCAT_PROPERTIES" ]; then
  echo "$TOMCAT_PROPERTIES" | tr ',' '\n' | while read property; do
    echo ${property} >> ${CATALINA_BASE}/conf/catalina.properties
  done
fi
