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

import com.google.cloud.trace.Trace;
import com.google.cloud.trace.Tracer;
import com.google.cloud.trace.core.SpanContext;
import com.google.cloud.trace.core.TraceContext;
import com.google.cloud.trace.service.TraceGrpcApiService;
import com.google.cloud.trace.service.TraceService;
import com.google.common.annotations.VisibleForTesting;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import java.io.IOException;
import javax.servlet.ServletException;

/**
 * This valve sends information about the requests to the Stackdriver Trace service.
 */
public class TraceValve extends ValveBase {

  /**
   * Header used by GCP to stores the trace id and the span id.
   */
  private static final String X_CLOUD_TRACE_HEADER = "x-cloud-trace-context";

  private static final Log log = LogFactory.getLog(TraceValve.class);

  private TraceService traceService;

  /**
   * Delay in second before the trace scheduler send the traces (allow buffering of traces).
   */
  private Integer traceScheduledDelay;

  /**
   * Identifier of the project to use when sending traces.
   */
  private String projectId;

  /**
   * {@inheritDoc}
   *
   * <p>Initialize the Trace service.</p>
   */
  @Override
  protected void initInternal() throws LifecycleException {
    super.initInternal();
    initTraceService();
  }

  @VisibleForTesting
  void initTraceService() throws LifecycleException {

    if (traceScheduledDelay != null && traceScheduledDelay <= 0) {
      throw new LifecycleException("The delay for trace must be greater than 0");
    }

    if (projectId == null || projectId.isEmpty()) {
      throw new LifecycleException("You must specify a project to store the traces");
    }

    try {
      TraceGrpcApiService.Builder traceServiceBuilder = TraceGrpcApiService.builder()
          .setProjectId(projectId);

      if (traceScheduledDelay != null) {
        traceServiceBuilder.setScheduledDelay(traceScheduledDelay);
      }

      traceService = traceServiceBuilder.build();

      Trace.init(traceService);
      log.info("Trace service initialized for project: " + projectId);
    } catch (IOException e) {
      log.error("An error occurred during the initialization of the Trace valve", e);
      throw new LifecycleException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>Create a new trace containing information about the requests.
   *    The traces are buffered by the TraceService and regularly sent to Stackdriver</p>
   */
  @Override
  public void invoke(Request request, Response response) throws IOException, ServletException {
    Tracer tracer = traceService.getTracer();

    String traceHeader = request.getHeader(X_CLOUD_TRACE_HEADER);
    if (traceHeader != null) {
      SpanContext spanContext = traceService
          .getSpanContextFactory()
          .fromHeader(traceHeader);
      traceService.getSpanContextHandler().attach(spanContext);
      log.debug("Tracing request with header: " + request.getHeader(X_CLOUD_TRACE_HEADER));
    }

    TraceContext context = tracer.startSpan(request.getRequestURI());

    getNext().invoke(request, response);

    tracer.endSpan(context);
  }

  /**
   * This property will be injected by Tomcat on startup.
   */
  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  /**
   * This property will be injected by Tomcat on startup.
   */
  public void setTraceScheduledDelay(Integer traceScheduledDelay) {
    this.traceScheduledDelay = traceScheduledDelay;
  }

  public void setTraceService(TraceService traceService) {
    this.traceService = traceService;
  }
}
