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
set -e

readonly dir=$(dirname $0)
readonly projectRoot=$dir/../..

# Load the library with Maven utilities
source ${projectRoot}/scripts/utils/maven.sh

# If we are in a gcloud environment we want to initialize the gcloud CLI
if [ -n "$GCLOUD_FILE" ]; then
  source ${dir}/gcloud-init.sh
fi

# Generate the Docker image tag from the git tag
pushd ${projectRoot}
  export DOCKER_TAG_LONG=$(git rev-parse --short HEAD)
popd

# If no namespace is specified deduct it from the gcloud CLI
if [ -z "$DOCKER_NAMESPACE" ]; then
  export DOCKER_NAMESPACE="gcr.io/$(gcloud info \
                | awk '/^Project: / { print $2 }' \
                | sed 's/\[//'  \
                | sed 's/\]//')"
fi

readonly IMAGE=$(maven_utils::get_property cloudbuild.tomcat.image)

echo "Building $IMAGE and running structure tests"
${projectRoot}/scripts/build.sh

echo "Running integration tests on $IMAGE"
${projectRoot}/scripts/integration_test.sh ${IMAGE}
