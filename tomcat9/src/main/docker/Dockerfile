FROM ${docker.openjdk.image}

# Create tomcat group and user
RUN groupadd -r tomcat \
 && useradd -r -g tomcat tomcat

# Environment variable for tomcat
ENV CATALINA_HOME ${tomcat.home}

# Create Tomcat home
ADD tomcat-home $CATALINA_HOME
RUN chmod +x ${CATALINA_HOME}/bin/startup.sh ${CATALINA_HOME}/bin/catalina.sh
RUN mkdir ${CATALINA_HOME}/logs
RUN chown -R tomcat:tomcat $CATALINA_HOME


EXPOSE 8080
CMD ["/bin/bash", "${tomcat.home}/bin/catalina.sh", "run"]