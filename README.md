# Google Cloud Platform Tomcat Runtime Image

[![experimental](http://badges.github.io/stability-badges/dist/experimental.svg)](http://github.com/badges/stability-badges)

This repository contains the source for the Google-maintained Tomcat [docker](https://docker.com) image.

# Using the Tomcat image
This image does not have official release, so you will need to build it first.

See [DEVELOPING.md](DEVELOPING.md)

You will need to create a Dockerfile based on the current image and add your application as a war file.

```dockerfile
FROM gcr.io/your-repository/tomcat
COPY your-application.war ROOT.war
```

# Security best practices

## Execute tomcat with a non root user
For security purpose it is recommend to start the tomcat instance using the tomcat user. 

You can do so by adding the following line at the end of your Dockerfile.

```dockerfile
FROM gcr.io/your-repository/tomcat
COPY your-application.war ROOT.war

RUN chown tomcat:tomcat $CATALINA_BASE/webapps/ROOT.war
USER tomcat
```
## Contributing changes

* See [CONTRIBUTING.md](CONTRIBUTING.md)

## Licensing

* See [LICENSE](LICENSE)
