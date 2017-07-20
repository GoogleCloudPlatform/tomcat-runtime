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

### JUnit Units tests

You can quickly tests the project locally using the JUnit tests, to do so run:
```bash
mvn clean test
```

### JUnit GCP Integration tests

This tests the interaction of the different components with the GCP Services. 
The tests are run outside of a Docker container. 

In order to run the tests you need to install the [Cloud SDK](https://cloud.google.com/sdk/docs/)
and select an active project with `gcloud init`.

You can run these tests with maven using the profile `gcp-integration-test`, for example:
```bash
mvn clean verify -P gcp-integration-test
```

### Runtimes common Structure tests

Specification: [Runtime common - Structure tests](https://github.com/GoogleCloudPlatform/runtimes-common/tree/master/structure_tests)

These tests inspect the content of the Docker image to ensure the presence of specific files and environment variables.

They are automatically run when the image is built with Maven. 
You can find the details of the tests in [structure.yaml](tomcat/src/test/resources/structure.yaml)

### Runtimes common Integration tests

Specification: [Runtimes common - Integration tests](https://github.com/GoogleCloudPlatform/runtimes-common/tree/master/integration_tests)

The integration tests will deploy a sample application to App Engine Flex and run remote tests against this application to ensure
that the standard requirements of the [language runtime image](https://github.com/GoogleCloudPlatform/runtimes-common/tree/master/integration_tests#tests) are respected.

Before running these tests ensure that:
* [Maven](https://maven.apache.org/download.cgi) is installed
* [Google Cloud SDK](https://cloud.google.com/sdk) is installed

When running those tests you need to indicate which image needs to be tested.
As the tests will be executed remotely, the image needs to be pushed to a gcr.io repository.
 
A script is available to run those tests:
```bash
RUNTIME_IMAGE=gcr.io/my-project-id/tomcat:tag
gcloud docker -- push $RUNTIME_IMAGE
./scripts/integration_test.sh $RUNTIME_IMAGE
```