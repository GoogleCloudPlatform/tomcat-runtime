Base image for App Engine managed VM using Apache Tomcat 8.

Sample Dockerfile to add an application as the ROOT Context:

```Dockerfile
FROM appengine-tomcat
ADD . ROOT
```

This initial version simply sets up logging and ports for a simple application. Additional work
is required to support:

 * Session persistence with replication across servers.
 * Container-managed authentication.
 * Container-managed authorization.
 * API authorization using the system role account.
 * Sending JVM and Tomcat statistics to Google Cloud Monitoring (collectd).
 * Direct integration with Google Cloud Logging (fluentd).

Changes made to the default configuration:

 * Moved log files to /var/log/app_engine/custom_logs
 * Removed log configuration for manager and host-manager webapps.
 * Removed Listener entries for trapping memory leaks during redeployment.
 * Removed AJP Listener.
 * Use common Executor and raise maxThreads to 250
 * Single HTTP11 Connector
 * Removed UserRealm and local user database.
 * Added AppEngine request header valve.
 * Removed WatchedResource entries to disable webapp reloading.
 * Turn off "development" mode for JSPs.
 * Removed all default webapps and set WORKDIR to the webapps directory.
