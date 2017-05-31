# Building the image

Make sure you have docker and maven installed and that the docker daemon is running 
and run the following command:
```bash
mvn clean install
```

This will add the tomcat runtime image to your local docker repository. You can now use the 
newly created image as a base in your Dockerfile.