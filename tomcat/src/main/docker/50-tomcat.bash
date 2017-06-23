#!/bin/bash

# If we are deploying on a GAE platform, copy gcp configuration
if [ "$PLATFORM" = "gae" ]; then
  cp /config/gcp.xml ${CATALINA_BASE}/conf/gcp.xml
fi