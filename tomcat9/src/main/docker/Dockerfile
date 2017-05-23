FROM ${docker.openjdk.image}

# Create tomcat group and user
RUN groupadd -r tomcat \
 && useradd -r -g tomcat tomcat

# Environment variable for tomcat
ENV TOMCAT_HOME ${tomcat.home}
ADD tomcat-home $TOMCAT_HOME

RUN chmod +x ${TOMCAT_HOME}/bin/startup.sh ${TOMCAT_HOME}/bin/catalina.sh
RUN mkdir ${TOMCAT_HOME}/logs

EXPOSE 8080
CMD ["/bin/bash", "${tomcat.home}/bin/catalina.sh", "run"]