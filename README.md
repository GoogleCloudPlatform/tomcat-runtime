# Google Cloud Platform Tomcat Runtime Image

[![experimental](http://badges.github.io/stability-badges/dist/experimental.svg)](http://github.com/badges/stability-badges)

This repository contains the source for the Google-maintained Tomcat [docker](https://docker.com) image.

# Using the Tomcat image

Create a `Dockerfile` based on the image and add your application WAR as `ROOT.war` to the current working directory.

```dockerfile
FROM gcr.io/google-appengine/tomcat
COPY your-application.war ROOT.war
```

## Running on App Engine Flexible

### Using the `gcloud beta app deploy` command

Simply choose the Java runtime with Tomcat as the server preference in `app.yaml`.

```yaml
runtime: java
env: flex
runtime_config:
  server: tomcat8
```

Then you can deploy using the `gcloud beta app deploy` [command](https://cloud.google.com/sdk/gcloud/reference/beta/app/deploy). Note that the "beta" command is currently required.

### Using custom Dockerfile

Specify a custom runtime in `app.yaml`.

```yaml
runtime: custom
env: flex
```

Then create a `Dockerfile` that uses the Tomcat runtime image.

```dockerfile
FROM gcr.io/google-appengine/tomcat
COPY your-application.war ROOT.war
```

## Configuration
The Tomcat instance can be configured through the environment variable `TOMCAT_PROPERTIES` which is
a comma-separated list of `name=value` pairs appended to `catalina.properties`.

# Security best practices

## Execute tomcat with a non-root user
For security purposes it is recommended to start the Tomcat instance using the `tomcat` user. 

You can do so by adding `USER tomcat` at the end of your Dockerfile.

```dockerfile
FROM gcr.io/google-appengine/tomcat
COPY your-application.war ROOT.war

RUN chown tomcat:tomcat $CATALINA_BASE/webapps/ROOT.war
USER tomcat
```

# Distributed sessions
This image can be configured to store Tomcat sessions in the [Google Cloud Datastore](https://cloud.google.com/datastore/docs) which allows
multiple instances of Tomcat to share sessions.

You can enable this feature by adding `distributed-sessions` to the list of enabled modules.
For example on Google App Engine:
 
```yaml
env_variables:
  TOMCAT_MODULES_ENABLE: distributed-sessions
```

## Configuration
The distributed sessions module can be configured through the environment variable `TOMCAT_PROPERTIES`.

|  Property | Description  | Default  | 
|---|---|---|
| session.DatastoreStore.namespace    |  Namespace to use in the Datastore.                         |  tomcat-gcp-persistent-session |
| session.DatastoreStore.sessionKind  |  Name of the entity used to store sessions in the Datastore. |  TomcatGCloudSession |
| session.DatastoreStore.sessionMaxInactiveTime |  Defines the maximum time (in seconds) a session can be inactive before being deleted by the expiration process. | 3600 |

## Running outside of Google Cloud
If you are using the runtime outside of Google Cloud, you will want to make sure that your application has access to
the Datastore. In this case, check out the [Google Cloud Authentication](https://developers.google.com/identity/protocols/application-default-credentials) guide.

# Development Guide

* See [instructions](DEVELOPING.md) on how to build and test this image.

## Contributing changes

* See [CONTRIBUTING.md](CONTRIBUTING.md)

# Licensing

* See [LICENSE](LICENSE)
