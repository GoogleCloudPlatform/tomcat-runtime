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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.Cursor;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery;
import com.google.common.collect.ImmutableList;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.session.StandardSession;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Ensures that {@code DatastoreStore} correctly uses the Datastore.
 */
public class DatastoreStoreTest {

  @Mock
  private Datastore datastore;

  @Mock
  private KeyFactory keyFactory;

  @Mock
  private StructuredQuery.Builder<Key> keyBuilder;

  @Mock
  private Manager manager;

  @Mock
  private Key key;

  @Mock
  private StructuredQuery<Key> keyQuery;

  @InjectMocks
  private DatastoreStore store;

  private QueryResults<Key> keyQueryResults;

  private static final String keyId = "123";

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    when(keyBuilder.build()).thenReturn(keyQuery);
    when(keyBuilder.setFilter(any())).thenReturn(keyBuilder);
    when(keyFactory.newKey(keyId)).thenReturn(key);
    when(key.getNameOrId()).thenReturn(keyId);
    when(manager.getContext()).thenReturn(new StandardContext());
    when(manager.willAttributeDistribute(anyString(), any())).thenReturn(true);

    keyQueryResults = new IteratorQueryResults<>(ImmutableList.of(key).iterator());
  }

  @Test
  public void testGetStoreSize() throws Exception {
    when(datastore.run(keyQuery)).thenReturn(keyQueryResults);

    int size = store.getSize();
    verify(datastore).run(keyQuery);
    assertEquals(1, size);
  }

  @Test
  public void testClearStore() throws Exception {
    when(datastore.run(keyQuery)).thenReturn(keyQueryResults);

    store.clear();
    verify(datastore).delete(key);
  }

  @Test
  public void testEnumerateKeys() throws Exception {
    when(datastore.run(keyQuery)).thenReturn(keyQueryResults);

    String[] keys = store.keys();
    verify(datastore).run(keyQuery);
    assertEquals(1, keys.length);
    assertEquals(keyId, keys[0]);
  }

  @Test
  public void testSessionDeserialization() throws Exception {
    StandardSession session = new StandardSession(manager);
    session.setValid(true);
    session.setId(keyId);
    session.setAttribute("value-to-serialize", 10);
    Blob serializedSession = serializeSession(session);
    Entity entity = Entity.newBuilder(key).set("content", serializedSession).build();

    when(datastore.get(key)).thenReturn(entity);
    when(manager.createEmptySession()).thenReturn(new StandardSession(manager));

    Session loadedSession = store.load(keyId);
    verify(datastore).get(key);
    verify(manager).createEmptySession();

    assertEquals(keyId, loadedSession.getId());
    assertEquals(10, loadedSession.getSession().getAttribute("value-to-serialize"));
  }

  @Test
  public void testLoadNonExistentSession() throws Exception {
    when(datastore.get(any(Key.class))).thenReturn(null);

    Session session = store.load("456");
    verify(manager, never()).createEmptySession();
    assertNull(session);
  }

  @Test
  public void testSessionRemoval() throws Exception {
    store.remove(keyId);
    verify(datastore).delete(key);
  }

  @Test
  public void testExpirationProcess() throws Exception {
    when(datastore.run(keyQuery)).thenReturn(keyQueryResults);

    store.processExpires();
    verify(datastore).delete(key);
  }

  @Test
  public void testSessionSave() throws Exception {
    StandardSession session = spy(new StandardSession(manager));
    session.setValid(true);
    session.setId(keyId);

    store.save(session);
    verify(datastore).put(any(FullEntity.class));
    verify(session).writeObjectData(any());
  }

  /**
   * Create a blob containing the serialized version of the session
   *
   * @param session A session in a valid state
   * @return A Blob with the session serialized
   */
  private Blob serializeSession(StandardSession session) throws Exception {

    ByteArrayOutputStream outputArray = new ByteArrayOutputStream();
    try (ObjectOutputStream outputStream = new ObjectOutputStream(outputArray)) {
      session.writeObjectData(outputStream);
    }

    return Blob.copyFrom(outputArray.toByteArray());
  }

  /**
   * This is an helper class to mock the return of Datastore queries.
   */
  private class IteratorQueryResults<T> implements QueryResults<T> {

    private Iterator<T> iterator;

    IteratorQueryResults(Iterator<T> iterator) {
      this.iterator = iterator;
    }

    @Override
    public Class<?> getResultClass() {
      return null;
    }

    @Override
    public Cursor getCursorAfter() {
      return null;
    }

    @Override
    public boolean hasNext() {
      return iterator.hasNext();
    }

    @Override
    public T next() {
      return iterator.next();
    }
  }

}