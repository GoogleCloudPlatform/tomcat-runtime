#
# Copyright 2015 Google Inc. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS-IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
FROM tomcat:${tomcat.version}-jre8

# Create App Engine logging directory.
RUN mkdir -p /var/log/app_engine/custom_logs

# Remove original configuration and sample applications.
RUN rm -rvf conf logs webapps/*
ADD conf conf
ADD ${project.build.finalName}.jar lib/${project.build.finalName}.jar

# Override default entropy generator
ENV JAVA_OPTS -Djava.security.egd=file:///dev/urandom

WORKDIR webapps