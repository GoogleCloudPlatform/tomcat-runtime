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

package com.google.cloud.runtimes.tomcat.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import org.apache.catalina.Context;
import org.apache.catalina.Session;
import org.apache.catalina.Store;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 *
 * Ensures that the operations of {@link DatastoreManager} are correctly delegated to the
 * corresponding {@link org.apache.catalina.Store} instance.
 *
 * Also tests operations on local sessions.
 */
public class DatastoreManagerTest {

  @Mock
  private Store store;

  @Mock
  private Session session;

  @Mock
  private Context context;

  private DatastoreManager manager;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    when(session.getId()).thenReturn("123");
    when(session.getIdInternal()).thenReturn("123");

    manager = new DatastoreManager();
    manager.setStore(store);
    manager.setContext(context);
  }


  @Test
  public void testFindSession() throws IOException, ClassNotFoundException {
    when(store.load(anyString())).thenReturn(session);

    Session loaded = manager.findSession("123");
    assertEquals(session, loaded);
    verify(store).load("123");
  }

  @Test
  public void testFindNonExistingSession() throws IOException {
    Session loaded = manager.findSession("123");
    assertNull(loaded);
  }

  @Test
  public void testLocalSessionRemoval() throws IOException {
    manager.add(session);
    assertTrue(Arrays.asList(manager.findSessions()).contains(session));

    manager.removeSuper(session);
    assertFalse(Arrays.asList(manager.findSessions()).contains(session));
    verify(store, never()).remove(anyString());
  }

  @Test
  public void testRemoteSessionRemoval() throws IOException {
    manager.remove(session);
    verify(store).remove("123");
  }

  @Test
  public void testCountOfActiveSession() throws IOException {
    manager.getActiveSessionsFull();
    verify(store).getSize();
  }

  @Test
  public void testListOfActiveSessions() throws IOException {
    when(store.keys()).thenReturn(new String[0]);
    manager.getSessionIdsFull();
    verify(store).keys();
  }

}