#!/bin/bash

# If we are deploying on a GAE platform, copy gcp configuration
if [ "$PLATFORM" == "gae" ]; then
  cp /config/gcp.xml ${CATALINA_BASE}/conf/gcp.xml
fi

if [ "$ENABLE_DISTRIBUTED_SESSIONS" == "true" ]; then
  cp /config/distributed-session.xml ${CATALINA_BASE}/conf/distributed-session.xml

  if [ -n "$DISTRIBUTED_SESSIONS_NAMESPACE" ]; then
     echo "session.DatastoreStore.namespace=${DISTRIBUTED_SESSIONS_NAMESPACE}" \
          >> ${CATALINA_BASE}/conf/catalina.properties
  fi
fi
