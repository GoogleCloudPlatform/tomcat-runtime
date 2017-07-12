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
 * Ensures that {@code DatastoreValve} is persisting the session in the store at the end
 * of each request.
 */
public class DatastoreValveTest {

  @Mock
  private Context context;

  @Mock
  private DatastoreManager manager;

  @Mock
  private Store store;

  @Mock
  private Valve nextValve = mock(Valve.class);

  private DatastoreValve valve;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    when(context.getManager()).thenReturn(manager);
    when(manager.getStore()).thenReturn(store);
    valve = new DatastoreValve();
  }

  @Test
  public void testSessionPersistence() throws Exception {
    Request request = mock(Request.class);
    Response response = mock(Response.class);
    Session session = mock(Session.class);

    when(request.getSessionInternal(anyBoolean())).thenReturn(session);
    when(request.getContext()).thenReturn(context);

    valve.setNext(nextValve);
    valve.invoke(request, response);

    verify(store).save(session);
    verify(manager).removeSuper(session);
  }

}