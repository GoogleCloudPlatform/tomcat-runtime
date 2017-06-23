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

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Reference the custom tests for the integration framework
 * (https://github.com/GoogleCloudPlatform/runtimes-common/tree/master/integration_tests)
 */
@WebServlet(urlPatterns = {"/custom"})
public class CustomServlet extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    StringBuilder configuration = new StringBuilder();
    configuration.append("[");
    configuration.append("{\"path\": \"custom/tests/secure\"},");
    // Dump the server configuration into the test logs
    configuration.append("{\"path\": \"dump/all\"}");
    configuration.append("]");

    resp.setContentType("application/json");
    resp.getWriter().print(configuration.toString());
  }
}
