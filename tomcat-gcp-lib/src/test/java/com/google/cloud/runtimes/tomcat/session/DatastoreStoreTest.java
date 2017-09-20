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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.datastore.Cursor;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.EntityValue;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.core.StandardContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Ensures that {@code DatastoreStore} correctly uses the Datastore.
 */
public class DatastoreStoreTest {

  @Mock
  private Datastore datastore;

  @Mock
  private Manager manager;

  private KeyFactory keyFactory;

  private Key key;

  private DatastoreStore store;

  private static final String keyId = "123";

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    store = new DatastoreStore();
    keyFactory = new KeyFactory("project").setKind("kind");
    key = keyFactory.newKey(keyId);
    StructuredQuery.Builder<Key> keyBuilder = Query.newKeyQueryBuilder().setKind("kind");
    QueryResults<Key> keyQueryResults = new IteratorQueryResults<>(ImmutableList.of(key).iterator());

    when(datastore.newKeyFactory()).thenReturn(new KeyFactory("project"));
    when(datastore.<Key>run(any(Query.class))).thenReturn(keyQueryResults);
    when(manager.getContext()).thenReturn(new StandardContext());
    when(manager.willAttributeDistribute(anyString(), any())).thenReturn(true);
    when(manager.createEmptySession()).thenReturn(new DatastoreSession(manager));

    store.setDatastore(datastore);
    store.setSessionKeyFactory(keyFactory);
    store.setKeyQueryBuilder(keyBuilder);
    store.setSessionKind("kind");
    store.setManager(manager);
  }

  @Test
  public void testGetStoreSize() throws Exception {
    int size = store.getSize();
    verify(datastore).run(any(Query.class));
    assertEquals(1, size);
  }

  @Test
  public void testClearStore() throws Exception {
    store.clear();
    verify(datastore).delete(any(Key.class));
  }

  @Test
  public void testEnumerateKeys() throws Exception {
    String[] keys = store.keys();
    verify(datastore).run(any(Query.class));
    assertEquals(1, keys.length);
    assertEquals(keyId, keys[0]);
  }

  @Test
  public void testSessionLoading() throws Exception {
    DatastoreSession session = new DatastoreSession(manager);
    session.setValid(true);
    session.setId(keyId);
    session.setAttribute("value-to-serialize", 10);

    Entity sessionEntity = session
        .saveMetadataToEntity(key)
        .set("attributes", session.saveAttributesToEntity(keyFactory, false).stream()
            .map(EntityValue::of)
            .collect(Collectors.toList()))
        .build();

    when(datastore.get(key)).thenReturn(sessionEntity);

    Session loadedSession = store.load(keyId);
    verify(datastore).get(key);
    verify(manager).createEmptySession();

    assertEquals(loadedSession.getId(), keyId);
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
    verify(datastore).delete(any(Key.class));
  }

  @Test
  public void testExpirationProcess() throws Exception {
    store.processExpires();
    verify(datastore).delete(key);
  }

  @Test
  public void testDecomposedSessionExpiration() throws Exception {
    store.setSeparateAttributes(true);
    Key attributeKey = keyFactory.newKey("attribute");

    when(datastore.<Query<Key>>run(any(Query.class))).thenReturn(
        new IteratorQueryResults<>(Collections.singletonList(key).iterator()),
        new IteratorQueryResults<>(Arrays.asList(key, attributeKey).iterator())
    );

    store.processExpires();
    verify(datastore).delete(Arrays.asList(key, attributeKey).toArray(new Key[0]));
  }

  @Test
  public void testSessionSave() throws Exception {
    DatastoreSession session = spy(new DatastoreSession(manager));
    session.setValid(true);
    session.setId(keyId);
    session.setAttribute("count", 5);

    store.save(session);
    ArgumentCaptor entity = ArgumentCaptor.forClass(Entity.class);
    verify(datastore).put((FullEntity<?>[]) entity.capture());
    verify(session).saveAttributesToEntity(any(), anyBoolean());

    assertNotNull(entity.getValue());
    assertEquals(entity.getValue().getClass(), Entity.class);
    assertEquals(((Entity)entity.getValue()).getKey().getName(), keyId);
  }

  @Test
  public void testDecomposedSessionSave() throws Exception {
    DatastoreSession session = new DatastoreSession(manager);
    session.setValid(true);
    session.setId(keyId);
    session.setAttribute("count", 5);

    store.setSeparateAttributes(true);
    store.save(session);

    ArgumentCaptor entities = ArgumentCaptor.forClass(Entity.class);
    verify(datastore).put(((FullEntity<?>[]) entities.capture()));

    assertEquals(2, entities.getAllValues().size());
    Stream<Entity> s = entities.getAllValues().stream();
    assertTrue(s
        .map(e -> e.getKey().getName())
        .collect(Collectors.toList())
        .containsAll(Arrays.asList("count", keyId)));
  }

  @Test
  public void testDecomposedSessionLoad() throws Exception {
    DatastoreSession session = new DatastoreSession(manager);
    session.setValid(true);
    session.setId(keyId);
    session.setAttribute("count", 2);
    session.setAttribute("map", Collections.singletonMap("key", "value"));

    List<FullEntity> entites = session.saveAttributesToEntity(keyFactory, true);
    entites.add(session.saveMetadataToEntity(key).build());

    QueryResults<FullEntity> queryResults = new IteratorQueryResults<>(entites.iterator());
    when(datastore.<FullEntity>run(any())).thenReturn(queryResults);

    store.setSeparateAttributes(true);
    Session restored = store.load(keyId);

    assertEquals(keyId, restored.getId());
    assertEquals(2, restored.getSession().getAttribute("count"));
    assertEquals("value",
        ((Map<String, String>)session.getSession().getAttribute("map")).get("key"));
  }

  @Test
  public void testSerializationCycleWithAttributeRemoval() throws Exception {
    store.setSeparateAttributes(true);
    DatastoreSession initialSession = new DatastoreSession(manager);
    initialSession.setValid(true);
    initialSession.setId(keyId);
    initialSession.setAttribute("count", 5);
    initialSession.setAttribute("map", Collections.singletonMap("key", "value"));
    KeyFactory attributeKeyFactory = datastore.newKeyFactory()
        .setKind("kind")
        .addAncestor(PathElement.of("kind", key.getName()));

    List<FullEntity> initialSessionEntities = store.serializeSession(initialSession, key,
        attributeKeyFactory);

    // Load the session and remove the map attribute
    when(datastore.<FullEntity>run(any())).thenReturn(
        new IteratorQueryResults<>(initialSessionEntities.iterator()));
    DatastoreSession session = (DatastoreSession)store.load(keyId);
    session.getSession().setAttribute("map", null);

    // Save and reload the session to ensure that the attribute map is not serialized
    store.save(session);

    ArgumentCaptor<Key> keyCaptors = ArgumentCaptor.forClass(Key.class);
    verify(datastore).delete(keyCaptors.capture());

    assertNotNull(keyCaptors.getValue());
    assertEquals("map", keyCaptors.getValue().getName());
  }


  @Test
  public void testTracerActivation() throws Exception {
    store.setTraceRequest(false);
    assertNull(store.startSpan("span"));

    store.setTraceRequest(true);
    assertNotNull(store.startSpan("span"));
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