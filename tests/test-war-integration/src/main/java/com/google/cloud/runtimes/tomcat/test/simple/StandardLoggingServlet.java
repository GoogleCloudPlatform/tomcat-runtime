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
package com.google.cloud.runtimes.tomcat.test.simple;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/logging_standard")
public class StandardLoggingServlet extends HttpServlet {

  private static final Logger logger = Logger.getLogger(StandardLoggingServlet.class.getName());

  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    JsonNode body = objectMapper.readTree(req.getReader());
    String token = body.path("token").asText();
    String level = convertStackdriverSeverityToLoggingLevel(body.path("level").asText());

    logger.log(Level.parse(level), token);

    resp.setContentType("text/plain");
    resp.getWriter().println(URLEncoder.encode("appengine.googleapis.com/stdout", "UTF-8"));
  }

  private String convertStackdriverSeverityToLoggingLevel(String level) {
    switch (level) {
      case "DEBUG":
        level = "FINE";
        break;
      case "ERROR":
      case "CRITICAL":
      case "ALERT":
        level = "SEVERE";
    }

    return level;
  }
}
