# Google Cloud Platform Tomcat Runtime Image

[![experimental](http://badges.github.io/stability-badges/dist/experimental.svg)](http://github.com/badges/stability-badges)

This repository contains the source for the Google-maintained Tomcat [docker](https://docker.com) image.

## Using the Tomcat image



### Running on App Engine Flexible

#### Cloud SDK

Simply choose the Java runtime with Tomcat as the server preference in `app.yaml`.

```yaml
runtime: java
env: flex
runtime_config:
  server: tomcat8
```

Then you can deploy using the `gcloud beta app deploy` [command](https://cloud.google.com/sdk/gcloud/reference/beta/app/deploy). Note that the "beta" command is currently required.

#### Using custom Dockerfile

Specify a custom runtime in `app.yaml`.

```yaml
runtime: custom
env: flex
```

Then create a `Dockerfile` that uses the Tomcat runtime image as specified in [Other platforms](#other-platforms).

### Other platforms

Create a `Dockerfile` based on the image and add your application WAR as `ROOT.war` to the current working directory.

```dockerfile
FROM gcr.io/google-appengine/tomcat
COPY your-application.war ROOT.war
```

## Configuring Tomcat

### Tomcat properties
The Tomcat instance can be configured through the environment variable `TOMCAT_PROPERTIES` which is
a comma-separated list of `name=value` pairs appended to `catalina.properties`.

### Security best practices

#### Execute tomcat with a non-root user
For security purposes it is recommended to start the Tomcat instance using the `tomcat` user. 

You can do so by adding `USER tomcat` at the end of your Dockerfile.

```dockerfile
FROM gcr.io/google-appengine/tomcat
COPY your-application.war ROOT.war

RUN chown tomcat:tomcat $CATALINA_BASE/webapps/ROOT.war
USER tomcat
```

## Optional Features
### Distributed sessions
This image can be configured to store Tomcat sessions in the [Google Cloud Datastore](https://cloud.google.com/datastore/docs) which allows
multiple instances of Tomcat to share sessions.

You can enable this feature by adding `distributed-sessions` to the list of optional modules, which is specified in the `TOMCAT_MODULES_ENABLE` environment variable.

For example on Google App Engine:
 
```yaml
env_variables:
  TOMCAT_MODULES_ENABLE: distributed-sessions
  TOMCAT_PROPERTIES: session.DatastoreStore.sessionMaxInactiveTime=2000,session.DatastoreValve.uriExcludePattern=/_ah/health
```

The distributed sessions module can be configured through the environment variable `TOMCAT_PROPERTIES`.

|  Property | Description  | Default  | 
|---|---|---|
| session.DatastoreStore.namespace    |  Namespace to use in the Datastore.                         |  tomcat-gcp-persistent-session |
| session.DatastoreStore.sessionKind  |  Name of the entity used to store sessions in the Datastore. |  TomcatGCloudSession |
| session.DatastoreStore.sessionMaxInactiveTime |  Defines the maximum time (in seconds) a session can be inactive before being deleted by the expiration process. | 3600 |
| session.DatastoreValve.uriExcludePattern | [Pattern](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html) specifying which Uri to ignore when persisting sessions. | null |

#### Usage outside of Google Cloud Platform
If you are using the runtime outside of GCP, you will want to make sure that your application has access to
the Datastore. In this case, check out the [Google Cloud Authentication](https://developers.google.com/identity/protocols/application-default-credentials) guide.

### Stackdriver Trace
The trace module sends information about requests (such as latency) to the [Stackdriver Trace service](https://cloud.google.com/trace/docs/).

To enable this module add `stackdriver-trace` to the list of enabled modules.

```yaml
env_variables:
  TOMCAT_MODULES_ENABLE: stackdriver-trace
```

The following configuration is available through the the environment variable `TOMCAT_PROPERTIES`.

|  Property | Description  | Default  |
|---|---|---|
| gcp.stackdriver-trace.scheduledDelay | The traces are grouped before being sent to the Stackdriver service, this is the maximum time in seconds a trace can be buffered| 15 |

#### Usage outside of Google Cloud Platform
When you are using this module outside of GCP you need to provide credentials through [Google Cloud Authentication](https://developers.google.com/identity/protocols/application-default-credentials).

### Stackdriver Logging
When the Tomcat runtime is running on Google App Engine flexible environment all output to stdout/stderr is forwarded to Stackdriver Logging
and available in the Cloud Console Log Viewer.

However more detailed and integrated logs are available if the [Stackdriver Logging](https://cloud.google.com/logging/) mechanism is used directly.

To take advantage of this integration, add the [Google Cloud Java Client for Logging](https://github.com/GoogleCloudPlatform/google-cloud-java/tree/master/google-cloud-logging) 
to your dependencies and provide a Java Util Logging configuration file (`logging.properties`) as part of the resources of the application (`classes/logging.properties`) with the following content:

```properties
handlers=com.google.cloud.logging.LoggingHandler

# Optional configuration
.level=FINE
com.google.cloud.logging.LoggingHandler.level=FINE
com.google.cloud.logging.LoggingHandler.log=gae_app.log
com.google.cloud.logging.LoggingHandler.formatter=java.util.logging.SimpleFormatter
java.util.logging.SimpleFormatter.format=%3$s: %5$s%6$s
```

## Development Guide

* See [instructions](DEVELOPING.md) on how to build and test this image.

### Contributing changes

* See [CONTRIBUTING.md](CONTRIBUTING.md)

## Licensing

* See [LICENSE](LICENSE)
