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

if [ -n "$GCLOUD_FILE" ]; then
  source $dir/gcloud-init.sh
fi

if [ -n "$DOCKER_NAMESPACE" ]; then
  PROJECT_NAMESPACE="$DOCKER_NAMESPACE"
else
  PROJECT_NAMESPACE="gcr.io/$(gcloud info \
                | awk '/^Project: / { print $2 }' \
                | sed 's/\[//'  \
                | sed 's/\]//')"
fi

pushd $projectRoot
  TAG=$(git rev-parse --short HEAD)
popd

readonly IMAGE="${PROJECT_NAMESPACE}/tomcat:${TAG}"

echo "Building $IMAGE and running structure tests"
gcloud container builds submit \
  --config ${dir}/cloudbuild.yaml \
  --substitutions="_IMAGE=$IMAGE,_DOCKER_TAG=$TAG" \
  ${projectRoot}

echo "Running integration tests on $IMAGE"
$dir/../integration_test.sh ${IMAGE}
