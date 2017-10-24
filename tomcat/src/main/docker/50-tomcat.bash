#!/bin/bash

# Unpack a WAR app (if present) beforehand so that Stackdriver Debugger
# can load it. This should be done before the JVM for Tomcat starts up.
export ROOT_WAR=$CATALINA_BASE/webapps/ROOT.war
export ROOT_DIR=$CATALINA_BASE/webapps/root
if [ -e "$ROOT_WAR" ]; then
  # Unpack it only if $ROOT_DIR doesn't exist or the root is older than the war.
  if [ -e "$ROOT_WAR" -a \( \( ! -e "$ROOT_DIR" \) -o \( "$ROOT_DIR" -ot "$ROOT_WAR" \) \) ]; then
    rm -fr $ROOT_DIR
    unzip $ROOT_WAR -d $ROOT_DIR
    chown -R tomcat:tomcat $ROOT_DIR
  fi
fi

# If we are deploying on a GAE platform, enable the GCP module
if [ "$PLATFORM" == "gae" ]; then
  TOMCAT_MODULES_ENABLE="$TOMCAT_MODULES_ENABLE,gcp"
fi

if [ -n "$TOMCAT_MODULES_ENABLE" ]; then
  echo "$TOMCAT_MODULES_ENABLE" | tr ',' '\n' | while read module; do
    if [ -r "/config/${module}.xml" ]; then
      cp "/config/${module}.xml" "${CATALINA_BASE}/conf/${module}.xml"
    else
      echo "The configuration for the module ${module} does not exist" >&2
    fi
  done
fi

# Add all the user defined properties to catalina.properties
if [ -n "$TOMCAT_PROPERTIES" ]; then
  echo >> ${CATALINA_BASE}/conf/catalina.properties
  echo "$TOMCAT_PROPERTIES" | tr ',' '\n' | while read property; do
    echo ${property} >> ${CATALINA_BASE}/conf/catalina.properties
  done
fi

if [ -n "$TOMCAT_LOGGING_PROPERTIES" ]; then
  echo "$TOMCAT_LOGGING_PROPERTIES" | tr ',' '\n' | while read property; do
    echo ${property} >> ${CATALINA_BASE}/conf/logging.properties
  done
fi