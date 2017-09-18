package com.google.cloud.runtimes.tomcat.session;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.runtimes.tomcat.session.DatastoreSession.SessionMetadata;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.catalina.Context;
import org.apache.catalina.Manager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DatastoreSessionTest {

  @Mock
  private Manager sessionManager;

  @Mock
  private Context managerContext;

  @Mock
  private Key sessionKey;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    when(sessionManager.getContext()).thenReturn(managerContext);
  }

  @Test
  public void testMetadataDeserialization() throws Exception {
    Entity metadata = Entity.newBuilder(sessionKey)
        .set(SessionMetadata.MAX_INACTIVE_INTERVAL.getValue(), 0)
        .set(SessionMetadata.CREATION_TIME.getValue(), 1)
        .set(SessionMetadata.LAST_ACCESSED_TIME.getValue(), 2)
        .set(SessionMetadata.IS_NEW.getValue(), true)
        .set(SessionMetadata.IS_VALID.getValue(), true)
        .set(SessionMetadata.THIS_ACCESSED_TIME.getValue(), 3)
        .build();

    List<FullEntity> attributes = new LinkedList<>();

    DatastoreSession session = new DatastoreSession(sessionManager);
    session.restoreFromEntities(metadata, attributes);

    assertEquals(session.getMaxInactiveInterval(), 0);
    assertEquals(session.getCreationTime(), 1);
    assertEquals(session.getLastAccessedTime(), 2);
    assertEquals(session.isNew(), true);
    assertEquals(session.isValid(), true);
    assertEquals(session.getThisAccessedTime(), 3);
  }

  @Test
  public void testAttributesDeserialization() throws Exception {
    Entity metadata = mock(Entity.class);
    when(metadata.getBoolean(any())).thenReturn(true);
    when(sessionKey.getName()).thenReturn("count");
    int count = 5;
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(bos);
    oos.writeObject(count);

    Entity firstAttribute = Entity.newBuilder(sessionKey)
        .set("value", Blob.copyFrom(bos.toByteArray()))
        .build();

    DatastoreSession session = new DatastoreSession(sessionManager);
    session.restoreFromEntities(metadata, Collections.singletonList(firstAttribute));

    assertEquals(count, session.getAttribute("count"));
  }

  @Test
  public void testAttributesSerializationKey() throws Exception {
    DatastoreSession session = new DatastoreSession(sessionManager);
    session.setValid(true);
    session.setAttribute("count", 2);
    session.setAttribute("map", new HashMap<>());

    KeyFactory factory = new KeyFactory("project").setKind("kind");
    List<FullEntity> entities = session.saveAttributesToEntity(factory, true);

    assertTrue(entities.stream()
        .map(entity -> (Key)entity.getKey())
        .map(key -> key.getName())
        .collect(Collectors.toSet())
        .containsAll(Arrays.asList("count", "map")));
  }

  @Test
  public void testSerializationCycle() throws Exception {
    DatastoreSession initialSession = new DatastoreSession(sessionManager);
    initialSession.setValid(true);
    initialSession.setAttribute("count", 5);
    initialSession.setAttribute("map", Collections.singletonMap("key", "value"));

    KeyFactory keyFactory = new KeyFactory("project").setKind("kind");
    List<FullEntity> attributes = initialSession.saveAttributesToEntity(keyFactory, true);
    Entity.Builder metadata = initialSession.saveMetadataToEntity(sessionKey);

    DatastoreSession restoredSession = new DatastoreSession(sessionManager);
    restoredSession.restoreFromEntities(metadata.build(), attributes);

    assertTrue(restoredSession.getAttribute("count") != null);
    assertTrue(restoredSession.getAttribute("map") != null);

    assertEquals(5, restoredSession.getAttribute("count"));
    assertEquals("value", ((Map)restoredSession.getAttribute("map")).get("key"));
  }

}