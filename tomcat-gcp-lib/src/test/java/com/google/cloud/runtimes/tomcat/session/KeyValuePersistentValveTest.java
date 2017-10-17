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

package com.google.cloud.runtimes.tomcat.session;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.catalina.Context;
import org.apache.catalina.Session;
import org.apache.catalina.Store;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Ensures that {@code KeyValuePersistentValve} is persisting the session in the store at the end of
 * each request.
 */
public class KeyValuePersistentValveTest {

  @Mock
  private Context context;

  @Mock
  private DatastoreManager manager;

  @Mock
  private Store store;

  @Mock
  private Request request;

  @Mock
  private Response response;

  @Mock
  private Session session;

  @Mock
  private Valve nextValve = mock(Valve.class);

  private KeyValuePersistentValve valve;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    when(context.getManager()).thenReturn(manager);
    when(manager.getStore()).thenReturn(store);
    when(request.getSessionInternal(anyBoolean())).thenReturn(session);
    when(request.getContext()).thenReturn(context);
    valve = new KeyValuePersistentValve();
  }

  @Test
  public void testSessionPersistence() throws Exception {
    valve.setNext(nextValve);
    valve.invoke(request, response);

    verify(store).save(session);
    verify(manager).removeSuper(session);
  }

  @Test
  public void testIgnoredHealthCheck() throws Exception {
    when(request.getRequestURI()).thenReturn("/_ah/health");

    valve.setNext(nextValve);
    valve.setUriExcludePattern("^/_ah/.*");
    valve.invoke(request, response);

    verify(session, never()).access();
  }

  @Test
  public void testIgnoredExtension() throws Exception {
    when(request.getRequestURI()).thenReturn("/img/foo.png");

    valve.setNext(nextValve);
    valve.setUriExcludePattern(".*\\.(ico|png|gif|jpg|css|js|)$");
    valve.invoke(request, response);

    verify(session, never()).access();
  }

  @Test
  public void testNonIgnoredHealthCheck() throws Exception {
    when(request.getRequestURI()).thenReturn("/_ah/health");

    valve.setNext(nextValve);
    valve.setUriExcludePattern(".*\\.(ico|png|gif|jpg|css|js|)$");
    valve.invoke(request, response);

    verify(session).access();
  }

}