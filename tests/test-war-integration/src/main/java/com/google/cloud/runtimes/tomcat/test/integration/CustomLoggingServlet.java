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

package com.google.cloud.runtimes.tomcat.test.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.MonitoredResource;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import com.google.cloud.logging.Payload;
import com.google.cloud.logging.Severity;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Collections;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/logging_custom")
public class CustomLoggingServlet extends HttpServlet {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  private final Logging logging = LoggingOptions.getDefaultInstance().getService();

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    JsonNode body = objectMapper.readTree(req.getReader());
    String logName = body.path("log_name").asText();
    String token = body.path("token").asText();
    Severity severity = Severity.valueOf(body.path("level").asText());

    LogEntry logEntry = LogEntry.newBuilder(Payload.StringPayload.of(token))
        .setLogName(logName)
        .setSeverity(severity)
        .setResource(MonitoredResource.newBuilder("global").build())
        .build();

    logging.write(Collections.singletonList(logEntry));

    resp.setContentType("text/plain");
    resp.getWriter().println(URLEncoder.encode(logName, "UTF-8"));
  }
}
