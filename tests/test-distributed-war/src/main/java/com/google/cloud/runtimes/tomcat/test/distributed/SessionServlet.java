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

package com.google.cloud.runtimes.tomcat.test.distributed;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.DoubleStream;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This servlet demonstrates the usage of distributed sessions by adding session parameters and
 * modifying their values at each requests.
 */
@WebServlet(urlPatterns = {"/session"})
public class SessionServlet extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
    resp.setContentType("text/plain");

    int count = 0;
    Object sessionValue = req.getSession().getAttribute("count");
    resp.getWriter().println("Max inactive time: " + req.getSession().getMaxInactiveInterval());

    if (sessionValue != null) {
      count = (int)sessionValue;
      count++;
      resp.getWriter().println("Session parameter: " + count);
    } else {
      resp.getWriter().println("No session parameter found, reload the page");
    }

    Map<String, Object> map = new HashMap<>();
    map.put("First test", Arrays.asList(1,2,3,4,5));
    map.put("Second arguments", DoubleStream.generate(() -> Math.random() * 10000)
        .limit(5000)
        .toArray());

    req.getSession().setAttribute("count", count);
    if (Math.random() > 0.7) {
      resp.getWriter().println("Modified map");
      req.getSession().setAttribute("map", map);
    }
  }

}
