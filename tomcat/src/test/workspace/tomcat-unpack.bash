#!/bin/bash

# Test that ROOT.war is unpacked so cloud debugging can use the class files within
rm -fr $CATALINA_BASE/webapps/ROOT $CATALINA_BASE/webapps/ROOT.war
trap "rm -rf $CATALINA_BASE/webapps/ROOT $CATALINA_BASE/webapps/ROOT.war" EXIT
mkdir $CATALINA_BASE/webapps/ROOT/WEB-INF -p
echo original > $CATALINA_BASE/webapps/ROOT/WEB-INF/web.xml
cd $CATALINA_BASE/webapps/ROOT
jar cf ../ROOT.war *
cd ..

rm -fr $CATALINA_BASE/webapps/ROOT
source /setup-env.d/50-tomcat.bash

if [ "$(cat $CATALINA_BASE/webapps/ROOT/WEB-INF/web.xml)" != "original" ]; then
  echo FAILED not unpacked when no ROOT
  exit 1
fi

echo updated > $CATALINA_BASE/webapps/ROOT/WEB-INF/web.xml
source /setup-env.d/50-tomcat.bash
if [ "$(cat $CATALINA_BASE/webapps/ROOT/WEB-INF/web.xml)" != "updated" ]; then
  echo FAILED unpacked when war older
  exit 1
fi

sleep 1

touch $CATALINA_BASE/webapps/ROOT.war
source /setup-env.d/50-tomcat.bash
if [ "$(cat $CATALINA_BASE/webapps/ROOT/WEB-INF/web.xml)" != "original" ]; then
  echo FAILED not unpacked when war newer
  exit 1
fi

echo "OK"