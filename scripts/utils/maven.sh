#!/bin/bash

# Copyright 2017 Google Inc. All rights reserved.

# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at

#     http://www.apache.org/licenses/LICENSE-2.0

# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

function maven_utils::execute () {
  if [ -n "$DOCKER_NAMESPACE" ]; then
    set -- $@ "-Ddocker.project.namespace=$DOCKER_NAMESPACE"
  fi

  if [ -n "$DOCKER_TAG_LONG" ]; then
    set -- $@ "-Ddocker.tag.long=$DOCKER_TAG_LONG"
  fi

  mvn -P-local-docker-build -P-test.local "$@"
}

function maven_utils::get_property () {
  echo $(maven_utils::execute org.codehaus.mojo:exec-maven-plugin:1.6.0:exec \
          --non-recursive -q \
          -Dexec.executable="echo" \
          -Dexec.args="\${$1}" )
}