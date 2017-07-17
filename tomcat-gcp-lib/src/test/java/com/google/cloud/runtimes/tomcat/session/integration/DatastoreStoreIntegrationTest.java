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

package com.google.cloud.runtimes.tomcat.session.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNoException;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.runtimes.tomcat.session.DatastoreManager;
import com.google.cloud.runtimes.tomcat.session.DatastoreStore;
import com.google.common.collect.Streams;
import java.io.IOException;
import java.util.Arrays;
import javax.servlet.http.HttpSession;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.core.StandardContext;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Ensure that the {@link DatastoreStore} respects the contract set by the
 * {@link org.apache.catalina.Store} interface and verify the behavior of
 * the serialization mechanisms.
 *
 * If no credentials can be found for the Datastore, those tests are ignored.
 */
public class DatastoreStoreIntegrationTest {

  private static DatastoreStore store;
  private static Datastore datastore;
  private static Manager manager;
  private static KeyFactory keyFactory;
  private static final String namespace = "tomcat-gcp-persistent-session-test";
  private static final String kind = "TomcatGCloudSession";

  private Session session;

  private static void setUpDatastore() {
    datastore = DatastoreOptions.newBuilder().setNamespace(namespace).build().getService();
    keyFactory = datastore.newKeyFactory().setKind(kind);
  }

  private static void setUpStore() {
    store = new DatastoreStore();
    Context context = new StandardContext();
    manager = new DatastoreManager();
    manager.setContext(context);
    store.setManager(manager);
    store.setNamespace(namespace);
    store.setSessionKind(kind);
  }

  private void clearNamespace() {
    QueryResults<Key> keys = datastore.run(Query.newKeyQueryBuilder().build());
    datastore.delete(Streams.stream(keys).toArray(Key[]::new));
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    setUpDatastore();
    setUpStore();

    // Run the tests only if the datastore is accessible
    try {
      datastore.get(keyFactory.newKey("123"));
    } catch (DatastoreException e) {
      assumeNoException(e);
    }

    store.start();
  }

  @Before
  public void setUp() throws LifecycleException {
    this.clearNamespace();
    this.session = manager.createSession("123");
  }

  @AfterClass
  public static void tearDown() throws LifecycleException {
    store.stop();
  }

  @Test
  public void testSessionSave() throws IOException {
    store.save(session);

    Entity serializedSession = datastore.get(keyFactory.newKey(session.getId()));
    assertNotNull(serializedSession);
  }

  @Test
  public void testSessionCount() throws IOException {
    Session session2 = manager.createSession("456");
    store.save(session);
    store.save(session2);

    int size = store.getSize();
    assertEquals(2, size);
  }

  @Test
  public void testSessionEnumeration() throws IOException {
    store.save(session);

    String[] keys = store.keys();
    assertTrue(Arrays.stream(keys).anyMatch(id -> session.getId().equals(id)));
    assertEquals(1, keys.length);
  }

  /**
   * The Store contract specifies that if no entity is found an empty array must be return.
   */
  @Test
  public void testSessionEnumerationForEmptyStore() throws IOException {
    String[] keys = store.keys();
    assertNotNull(keys);
  }

  @Test
  public void testSessionLoad() throws IOException, ClassNotFoundException {
    session.getSession().setAttribute("attribute", "test-value");
    store.save(session);

    Session loadedSession = store.load(session.getId());
    assertEquals("test-value", loadedSession.getSession().getAttribute("attribute"));
  }

  @Test
  public void testSessionRemoval() throws IOException {
    store.save(session);

    store.remove(session.getId());
    Entity serializedSession = datastore.get(keyFactory.newKey(session.getId()));
    assertNull(serializedSession);
  }

  @Test
  public void testStoreReset() throws IOException {
    store.save(session);

    store.clear();
    Entity serializedSession = datastore.get(keyFactory.newKey(session.getId()));
    assertNull(serializedSession);
    assertEquals(0, store.getSize());
  }

  /**
   * Non serializable attributes should be ignored but should not prevent the serialization
   * of the session.
   */
  @Test
  public void testSerializationOfNonSerializableAttribute()
      throws IOException, ClassNotFoundException {
    HttpSession innerSession = session.getSession();
    innerSession.setAttribute("serializable", "test");
    innerSession.setAttribute("non-serializable", manager);
    store.save(session);

    Session loadedSession = store.load(session.getId());
    HttpSession innerLoadedSession = loadedSession.getSession();
    assertNull(innerLoadedSession.getAttribute("non-serializable"));
    assertNotNull(innerLoadedSession.getAttribute("serializable"));
  }

}