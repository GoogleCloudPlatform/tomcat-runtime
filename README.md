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

## Contributing changes

* See [CONTRIBUTING.md](CONTRIBUTING.md)

## Licensing

* See [LICENSE](LICENSE)
