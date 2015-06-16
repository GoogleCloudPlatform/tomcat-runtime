Base image for an App Engine custom runtime using Apache Tomcat 8.
This project is still in initial development and features may change at any time.
Please use the [issue tracker](https://github.com/GoogleCloudPlatform/appengine-container-tomcat/issues)
for feedback and suggestions.

To use this pre-release version you will need to build the container image yourself. Make sure
you have Apache Maven installed and Docker is running, then run:

```
$ mvn package docker:package
```

This will compile the support libraries and create a local Docker image tagged `appengine-tomcat`

To use this image and set your application as the ROOT Context, add the following Dockerfile
to the root of an exploded web application:

```Dockerfile
FROM appengine-tomcat
ADD . ROOT
```

This initial version simply sets up logging and ports for a simple application. Additional work
is required to support features such as:

 * Session persistence with replication across servers.
 * Container-managed authentication.
 * Container-managed authorization.
 * API authorization using the system role account.
 * Sending JVM and Tomcat statistics to Google Cloud Monitoring (collectd).
 * Direct integration with Google Cloud Logging (fluentd).

Please see the [issue tracker](https://github.com/GoogleCloudPlatform/appengine-container-tomcat/issues) for more details.

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
