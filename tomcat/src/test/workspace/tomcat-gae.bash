#!/bin/bash

PLATFORM="gae"

# Test that the configuration for gcp is added into tomcat config
source /setup-env.d/50-tomcat.bash
if [ "$(cat /config/gcp.xml)" != "$(cat ${CATALINA_BASE}/conf/gcp.xml)" ]; then
  echo "gcp.xml should be copied from /config to catalina base"
  exit 1
fi

echo "OK"