package com.google.cloud.runtimes.tomcat.logging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.trace.SpanContextHandler;
import com.google.cloud.trace.Tracer;
import com.google.cloud.trace.core.ConstantTraceOptionsFactory;
import com.google.cloud.trace.core.SpanContext;
import com.google.cloud.trace.core.SpanContextFactory;
import com.google.cloud.trace.core.TraceContext;
import com.google.cloud.trace.service.TraceService;
import com.google.devtools.cloudtrace.v1.TraceSpan;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.tomcat.util.descriptor.web.ContextHandler;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
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

  @InjectMocks
  private TraceValve valve;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    valve.setNext(nextValve);

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
    valve.invoke(request, response);

    verify(tracer).startSpan(contains("/index"));
    verify(tracer).endSpan(traceContext);
  }

  /**
   * If x-cloud-trace-context header is present a new context must created.
   */
  @Test
  public void testTraceHeader() throws Exception {
    SpanContext contextFromHeader = mock(SpanContext.class);
    when(request.getHeader("x-cloud-trace-context")).thenReturn("traceid/spanid");
    when(spanContextFactory.fromHeader("traceid/spanid")).thenReturn(contextFromHeader);

    valve.invoke(request, response);

    verify(spanContextHandler).attach(contextFromHeader);
  }

}