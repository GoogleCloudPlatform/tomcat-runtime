#!/bin/bash

PLATFORM="gae"

# Test that the configuration for gcp is added into tomcat config
source /setup-env.d/50-tomcat.bash
if [ "$(cat /config/gcp.xml)" != "$(cat ${CATALINA_BASE}/conf/gcp.xml)" ]; then
  echo "gcp.xml should be copied from /config to catalina base"
  exit 1
fi

# Test the JAVA_OPTS for cloud debugging
EXPECTED_DBG_AGENT="-agentpath:/opt/cdbg/cdbg_java_agent.so=--log_dir=/var/log/app_engine,--alsologtostderr=true,--cdbg_extra_class_path=${CATALINA_BASE}/webapps/ROOT/WEB-INF/classes:${CATALINA_BASE}/webapps/ROOT/WEB-INF/lib"
ACTUAL_DBG_AGENT="$(export GAE_INSTANCE=instance; /docker-entrypoint.bash env | grep DBG_AGENT | cut -d '=' -f 1 --complement)"

if [ "$ACTUAL_DBG_AGENT" != "$EXPECTED_DBG_AGENT" ]; then
  echo "DBG_AGENT='$(echo ${ACTUAL_DBG_AGENT})'"
  exit 1
fi

echo "OK"