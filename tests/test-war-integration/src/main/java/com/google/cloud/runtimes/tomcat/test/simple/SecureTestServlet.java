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
public class SecureTestServlet extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    resp.setContentType("plain/text");

    if (req.getHeader("x-forwarded-proto") != null) {

      if (req.getHeader("x-forwarded-proto").equals("http") && req.isSecure()) {
        resp.setStatus(500);
        resp.getWriter().println("Error: x-forwarded-proto is set to http and the connection is considered secure");
      }

      else if (req.getHeader("x-forwarded-proto").equals("https") && !req.isSecure()) {
        resp.setStatus(500);
        resp.getWriter().println("Error: x-forwarded-proto is set to https but the connection is not considered secure");
      }
    }
  }
}
