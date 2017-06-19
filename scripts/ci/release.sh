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

dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
projectRoot=${dir}/../..

source ${projectRoot}/scripts/utils/gcloud.sh

RUNTIME_NAME='tomcat'
RUNTIME_VERSION='8.5'

if [ -z "${DOCKER_TAG}" ]; then
  DOCKER_TAG="${RUNTIME_VERSION}-$(date -u +%Y-%m-%d_%H_%M)"
fi

if [ -z "$DOCKER_NAMESPACE" ]; then
  DOCKER_NAMESPACE="gcr.io/$(gcloud_utils::get_project_name)"
fi

IMAGE="${DOCKER_NAMESPACE}/${RUNTIME_NAME}:${DOCKER_TAG}"

gcloud container builds submit \
        --config ${dir}/release-cloudbuild.yaml \
        --substitutions="_IMAGE=$IMAGE,_DOCKER_TAG=$DOCKER_TAG" \
        ${projectRoot}

# Allow external script to reference the image tag
export TAG=${DOCKER_TAG}