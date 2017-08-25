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

package com.google.cloud.runtimes.tomcat.trace;

public enum HttpLabels {
  HTTP_REQUEST_SIZE("/http/request/size"),
  HTTP_RESPONSE_SIZE("/http/response/size"),
  HTTP_METHOD("/http/method"),
  HTTP_STATUS_CODE("/http/status_code"),
  HTTP_URL("/http/url"),
  HTTP_USER_AGENT("/http/user_agent"),
  HTTP_CLIENT_PROTOCOL("/http/client_protocol");

  private final String value;

  HttpLabels(String value) {
    this.value = value;
  }

  public String getValue() {
    return this.value;
  }
}
