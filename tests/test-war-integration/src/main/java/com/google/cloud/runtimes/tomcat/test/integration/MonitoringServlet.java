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
import com.google.api.Metric;
import com.google.cloud.ServiceOptions;
import com.google.cloud.Timestamp;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.monitoring.v3.Point;
import com.google.monitoring.v3.ProjectName;
import com.google.monitoring.v3.TimeInterval;
import com.google.monitoring.v3.TimeSeries;
import com.google.monitoring.v3.TypedValue;
import java.io.IOException;
import java.util.Collections;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/monitoring")
public class MonitoringServlet extends HttpServlet {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  private static final Logger logger = Logger.getLogger(MonitoringServlet.class.getName());

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    JsonNode body = objectMapper.readTree(req.getReader());
    String name = body.path("name").asText();
    long token = body.path("token").asLong();

    logger.info("Creating Time series with name " + name + " and token " + token);

    MetricServiceClient serviceClient = MetricServiceClient.create();

    TimeSeries timeSeries =
        TimeSeries.newBuilder()
            .addPoints(Point.newBuilder()
                .setValue(TypedValue.newBuilder().setInt64Value(token))
                .setInterval(TimeInterval.newBuilder()
                  .setEndTime(Timestamp.now().toProto())))
            .setMetric(Metric.newBuilder().setType(name))
            .build();

    serviceClient.createTimeSeries(
        ProjectName.create(ServiceOptions.getDefaultProjectId()),
        Collections.singletonList(timeSeries));

    resp.setContentType("text/plain");
    resp.getWriter().println("OK");
  }

}
