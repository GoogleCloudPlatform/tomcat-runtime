# Development Guide

## Building the image

Make sure you have docker and maven installed and that the docker daemon is running 
and run the following command:

```bash
mvn clean install
```

This will add the tomcat runtime image to your local docker repository. You can now use the 
newly created image as a base in your Dockerfile.

## Testing the image

### Structure tests

Specification: [Runtime common - Structure tests](https://github.com/GoogleCloudPlatform/runtimes-common/tree/master/structure_tests)

This tests inspect the content of the image to ensure the presence of specific files and environment variables. 

Those tests are automatically run when the image is build with maven. You can launch them with the command:

```bash
mvn clean verify
```

### Integration tests

Specification: [Runtimes common - Integration tests](https://github.com/GoogleCloudPlatform/runtimes-common/tree/master/integration_tests)

The integration tests will deploy a sample application to App Engine and run remote test against this application to ensure
that the standard requirement of [language runtime image](https://github.com/GoogleCloudPlatform/runtimes-common/tree/master/integration_tests#tests) are respected.

Before running this tests ensure that:
* Maven is installed
* Google Cloud SDK is installed

A script is available to run those tests:
```bash
./scripts/integration_test.sh
```