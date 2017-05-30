# Copyright 2017 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
FROM ${docker.openjdk.image}

RUN groupadd -r tomcat \
 && useradd -r -g tomcat tomcat

# Environment variable for tomcat
ENV CATALINA_HOME ${tomcat.home}

# Create Tomcat home
COPY tomcat-home $CATALINA_HOME
RUN chmod +x ${CATALINA_HOME}/bin/catalina.sh \
    && chown -R tomcat:tomcat ${CATALINA_HOME} \
    && rm -rf ${CATALINA_HOME}/webapps

# Create Tomcat base
ENV CATALINA_BASE ${tomcat.base}
COPY tomcat-base $CATALINA_BASE
RUN mkdir -p $CATALINA_BASE/webapps \
    && mkdir -p $CATALINA_BASE/temp \
    && chown -R tomcat:tomcat ${CATALINA_BASE}



WORKDIR $CATALINA_BASE/webapps
EXPOSE 8080

CMD ["/bin/bash", "${tomcat.home}/bin/catalina.sh", "run"]