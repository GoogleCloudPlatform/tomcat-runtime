#!/bin/bash

# If we are deploying on a GAE platform, enable the GCP module
if [ "$PLATFORM" == "gae" ]; then
  TOMCAT_MODULES_ENABLE="$TOMCAT_MODULES_ENABLE,gcp"
fi

if [ -n "$TOMCAT_MODULES_ENABLE" ]; then
  echo "$TOMCAT_MODULES_ENABLE" | tr ',' '\n' | while read module; do
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

if [ -n "$TOMCAT_LOGGING_PROPERTIES" ]; then
  echo "$TOMCAT_LOGGING_PROPERTIES" | tr ',' '\n' | while read property; do
    echo ${property} >> ${CATALINA_BASE}/conf/logging.properties
  done
fi