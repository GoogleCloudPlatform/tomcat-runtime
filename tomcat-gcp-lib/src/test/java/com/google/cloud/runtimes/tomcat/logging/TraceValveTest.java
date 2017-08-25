/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.runtimes.tomcat.logging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.trace.SpanContextHandler;
import com.google.cloud.trace.Tracer;
import com.google.cloud.trace.core.Label;
import com.google.cloud.trace.core.Labels;
import com.google.cloud.trace.core.SpanContext;
import com.google.cloud.trace.core.SpanContextFactory;
import com.google.cloud.trace.core.TraceContext;
import com.google.cloud.trace.service.TraceService;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TraceValveTest {

  @Mock
  private TraceService traceService;

  @Mock
  private Tracer tracer;

  @Mock
  private Request request;

  @Mock
  private Response response;

  @Mock
  private Valve nextValve;

  @Mock
  private TraceContext traceContext;

  @Mock
  private SpanContextHandler spanContextHandler;

  @Mock
  private SpanContextFactory spanContextFactory;

  private TraceValve valve;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    valve = new TraceValve();
    valve.setNext(nextValve);
    valve.setTraceService(traceService);

    when(traceService.getTracer()).thenReturn(tracer);
    when(traceService.getSpanContextFactory()).thenReturn(spanContextFactory);
    when(traceService.getSpanContextHandler()).thenReturn(spanContextHandler);
    when(tracer.startSpan(anyString())).thenReturn(traceContext);
  }

  @Test
  public void testRequestForwarding() throws Exception {
    valve.invoke(request, response);

    verify(nextValve).invoke(any(), any());
  }

  @Test
  public void testSpanCreation() throws Exception {
    when(request.getRequestURI()).thenReturn("/index");
    when(response.getStatus()).thenReturn(200);
    ArgumentCaptor<Labels> labelsArgument = ArgumentCaptor.forClass(Labels.class);
    ArgumentCaptor<TraceContext> traceArgument = ArgumentCaptor.forClass(TraceContext.class);
    Label indexLabel = new Label(HttpLabels.HTTP_URL.getValue(), "/index");
    Label statusCodeLabel = new Label(HttpLabels.HTTP_STATUS_CODE.getValue(), "200");

    valve.invoke(request, response);

    verify(tracer).startSpan(matches("/index"));
    verify(tracer).annotateSpan(traceArgument.capture(), labelsArgument.capture());
    verify(tracer).endSpan(traceContext);
    assertEquals(labelsArgument.getValue().getLabels().size(), 2);
    assertTrue(labelsArgument.getValue().getLabels().contains(indexLabel));
    assertTrue(labelsArgument.getValue().getLabels().contains(statusCodeLabel));
  }

  /**
   * If x-cloud-trace-context header is present a new context must created.
   */
  @Test
  public void testTraceHeader() throws Exception {
    SpanContext contextFromHeader = mock(SpanContext.class);
    when(request.getHeader(SpanContextFactory.headerKey())).thenReturn("traceid/spanid");
    when(spanContextFactory.fromHeader("traceid/spanid")).thenReturn(contextFromHeader);

    valve.invoke(request, response);

    verify(spanContextHandler).attach(contextFromHeader);
  }

  @Test(expected = LifecycleException.class)
  public void testInvalidTraceDelay() throws Exception {
    valve.setTraceScheduledDelay(0);
    valve.initTraceService();
  }

}