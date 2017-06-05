# Development Guide

## Building the image

Make sure you have Docker and Maven installed and that the Docker daemon is running.
Then run the following command:

```bash
mvn clean install
```

This will add the Tomcat runtime image to your local docker repository. You can now use the
newly created image as a base in your Dockerfile.

## Testing the image

### Structure tests

Specification: [Runtime common - Structure tests](https://github.com/GoogleCloudPlatform/runtimes-common/tree/master/structure_tests)

These tests inspect the content of the Docker image to ensure the presence of specific files and environment variables.

They are automatically run when the image is built with Maven. 
You can find the details of the tests in [structure.yaml](tomcat/src/test/resources/structure.yaml)

### Integration tests

Specification: [Runtimes common - Integration tests](https://github.com/GoogleCloudPlatform/runtimes-common/tree/master/integration_tests)

The integration tests will deploy a sample application to App Engine Flex and run remote tests against this application to ensure
that the standard requirements of the [language runtime image](https://github.com/GoogleCloudPlatform/runtimes-common/tree/master/integration_tests#tests) are respected.

Before running these tests ensure that:
* [Maven](https://maven.apache.org/download.cgi) is installed
* [Google Cloud SDK](https://cloud.google.com/sdk) is installed

When running those tests you need to indicate which image need to be tested. 
As the tests will be executed remotely the image need to be pushed in a gcr.io repository.
 
A script is available to run those tests:
```bash
RUNTIME_IMAGE=gcr.io/my-project-id/tomcat:tag
gcloud docker -- push $RUNTIME_IMAGE
./scripts/integration_test.sh $RUNTIME_IMAGE
```