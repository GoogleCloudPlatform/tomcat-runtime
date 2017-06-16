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
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This class is used by the the runtime common testing framework as a custom test to ensure
 * that when x-forwarded-proto is specified and equals to https
 * (e.g the application is served by a load balancer)
 * the connection is considered as secure.
 *
 * Note:
 * - If the request with the header x-forwarded-proto does not come from a local ip the
 *   connection will not be considered as secure.
 *
 * - In Tomcat this header is interpreted by the valve:
 *   https://tomcat.apache.org/tomcat-9.0-doc/config/valve.html#Remote_IP_Valve
 *   See tomcat-base/server.xml for the configuration.
 */
@WebServlet(urlPatterns = "/custom/tests/secure")
public class SecureServlet extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    resp.setContentType("plain/text");

    // This test must not be run if we are not behind a load balancer
    if (req.getHeader("x-forwarded-proto") == null || req.getHeader("x-forwarded-proto").equals("http")) {
      resp.getWriter().print("OK - Test not run");
      return;
    }

    if (req.isSecure()) {
      resp.getWriter().print("OK");
    } else {
      resp.setStatus(500);
      PrintWriter writer = resp.getWriter();
      writer.println("x-forwarded-proto is present but the request is not considered as secure");
    }
  }
}
