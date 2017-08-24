/*
 * Copyright (C) 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.runtimes.tomcat.logging;

class HttpLabels {
  static final String REQUEST_SIZE = "/request/size";
  static final String RESPONSE_SIZE = "/response/size";
  static final String HTTP_METHOD = "/http/method";
  static final String HTTP_STATUS_CODE = "/http/status_code";
  static final String HTTP_URL = "/http/url";
  static final String HTTP_USER_AGENT = "/http/user_agent";
  static final String HTTP_CLIENT_PROTOCOL = "/http/client_protocol";
}
