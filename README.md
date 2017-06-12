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

Create a `Dockerfile` as described above and specify custom runtime in `app.yaml`.

```yaml
runtime: custom
env: flex
```

# Security best practices

## Execute tomcat with a non root user
For security purposes it is recommended to start the Tomcat instance using the `tomcat` user. 

You can do so by adding `USER tomcat` at the end of your Dockerfile.

```dockerfile
FROM gcr.io/google-appengine/tomcat
COPY your-application.war ROOT.war

RUN chown tomcat:tomcat $CATALINA_BASE/webapps/ROOT.war
USER tomcat
```

## Development Guide

* See [instructions](DEVELOPING.md) on how to build and test this image.

## Contributing changes

* See [CONTRIBUTING.md](CONTRIBUTING.md)

## Licensing

* See [LICENSE](LICENSE)
