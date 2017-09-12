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

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Entity.Builder;
import com.google.cloud.datastore.EntityValue;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.trace.Trace;
import com.google.cloud.trace.Tracer;
import com.google.cloud.trace.core.TraceContext;
import com.google.common.collect.Streams;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Session;
import org.apache.catalina.session.StoreBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;


/**
 * This store interacts with the Datastore service to persist sessions.
 *
 * <p>It does not make any assumptions about the manager, so it could be used
 * by all manager implementations.</p>
 *
 * <p>However, aggregations can be slow on the Datastore. So, if performance is a concern, prefer
 * using a manager implementation which is not using aggregations such as
 * {@link DatastoreManager}</p>
 */
public class DatastoreStore extends StoreBase {

  private static final Log log = LogFactory.getLog(DatastoreStore.class);

  private Datastore datastore = null;
  private KeyFactory sessionKeyFactory = null;
  private StructuredQuery.Builder<Key> keyQueryBuilder = null;

  /**
   * Name of the kind used in The Datastore for the session.
   */
  private String sessionKind;

  /**
   * Namespace to use in the Datastore.
   */
  private String namespace;

  private boolean useUniqueEntity = false;

  private boolean traceRequest = true;

  /**
   * {@inheritDoc}
   *
   * <p>Initiate a connection to the Datastore.</p>
   *
   */
  @Override
  protected synchronized void startInternal() throws LifecycleException {
    log.debug("Initialization of the Datastore Store");

    this.datastore = DatastoreOptions.newBuilder().setNamespace(namespace).build().getService();
    this.sessionKeyFactory = datastore.newKeyFactory().setKind(sessionKind);
    this.keyQueryBuilder = Query.newKeyQueryBuilder().setKind(sessionKind);

    super.startInternal();
  }

  /**
   * Return the number of Sessions present in this Store.
   *
   * <p>The Datastore does not support counting elements in a collection.
   * So, all keys are fetched and the counted locally.</p>
   *
   * <p>This method may be slow if a large number of sessions are persisted,
   * prefer operations on individual entities rather than aggregations.</p>
   *
   * @return The number of sessions stored into the Datastore
   */
  @Override
  public int getSize() throws IOException {
    log.debug("Accessing sessions count, be cautious this operation can cause performance issues");
    Query<Key> query = this.keyQueryBuilder.build();
    QueryResults<Key> results = datastore.run(query);
    long count = Streams.stream(results).count();
    return Math.toIntExact(count);
  }

  /**
   * Returns an array containing the session identifiers of all Sessions currently saved in this
   * Store. If there are no such Sessions, a zero-length array is returned.
   *
   * <p>This operation may be slow if a large number of sessions is persisted.
   * Note that the number of keys returned may be bounded by the Datastore configuration.</p>
   *
   * @return The ids of all persisted sessions
   */
  @Override
  public String[] keys() throws IOException {
    String[] keys;

    Query<Key> query = this.keyQueryBuilder.build();
    QueryResults<Key> results = datastore.run(query);
    keys = Streams.stream(results)
        .map(key -> key.getNameOrId().toString())
        .toArray(String[]::new);

    if (keys == null) {
      keys = new String[0];
    }

    return keys;
  }

  /**
   * Load and return the Session associated with the specified session identifier from this Store,
   * without removing it. If there is no such stored Session, return null.
   *
   * <p>Look in the Datastore for a serialized session and attempt to deserialize it.</p>
   *
   * <p>If the session is successfully deserialized, it is added to the current manager and is
   * returned by this method. Otherwise null is returned.</p>
   *
   * @param id Session identifier of the session to load
   * @return The loaded session instance
   * @throws ClassNotFoundException If a deserialization error occurs
   */
  @Override
  public Session load(String id) throws ClassNotFoundException, IOException {
    log.debug("Session " + id + " requested");

    TraceContext metadataContext = startSpan("Load metadata from Datastore");
    Key sessionKey = sessionKeyFactory.newKey(id);
    endSpan(metadataContext);

    DatastoreSession session = deserializeSession(sessionKey);

    log.debug("Session " + id + " loaded");
    return session;
  }

  /**
   * Create a new session usable by Tomcat from a session serialized in a Datastore Entity.
   * @param sessionKey The key associated to the session metadata and its attributes.
   * @return A new session containing the metadata and attributes stored into the entity.
   * @throws ClassNotFoundException Thrown if the class serialized in the entity is not available in
   *                                this context.
   * @throws IOException Thrown when an error occur during the deserialization.
   */
  private DatastoreSession deserializeSession(Key sessionKey)
      throws ClassNotFoundException, IOException {
    Entity sessionEntity = null;

    TraceContext entitiesContext = startSpan("Load attributes from Datastore");
    List<FullEntity> attributeEntities = new LinkedList<>();
    if (useUniqueEntity) {
      sessionEntity = datastore.get(sessionKey);
      if (sessionEntity.contains("attributes")) {
        sessionEntity.<EntityValue>getList("attributes")
            .forEach(val -> attributeEntities.add(val.get()));
      }
    } else {
      Iterator<Entity> entities = datastore.run(Query.newEntityQueryBuilder()
          .setKind(getAttributesKind())
          .setFilter(PropertyFilter.hasAncestor(sessionKey))
          .build());

      while (entities.hasNext()) {
        Entity entity = entities.next();
        if (entity.getKey().equals(sessionKey)) {
          sessionEntity = entity;
        } else {
          attributeEntities.add(entity);
        }
      }
    }
    endSpan(entitiesContext);

    DatastoreSession session = null;
    if (sessionEntity != null) {
      session = (DatastoreSession) manager.createEmptySession();
      TraceContext deserializationContext = startSpan("Deserialization of the session");
      session.restoreFromEntities(sessionEntity, attributeEntities);
      session.setId(sessionKey.getName());
      endSpan(deserializationContext);
    }
    return session;
  }

  /**
   * Remove the Session with the specified session identifier from this Store.
   * If no such Session is present, this method takes no action.
   *
   * @param id Session identifier of the session to remove
   */
  @Override
  public void remove(String id) {
    log.debug("Removing session: " + id);
    datastore.delete(sessionKeyFactory.newKey(id));
  }

  /**
   * Remove all Sessions from this Store.
   */
  @Override
  public void clear() throws IOException {
    log.debug("Deleting all sessions");
    datastore.delete(Arrays.stream(keys())
                           .map(sessionKeyFactory::newKey)
                           .toArray(Key[]::new));
  }

  /**
   * Save the specified Session into this Store. Any previously saved information for
   * the associated session identifier is replaced.
   *
   * <p>Attempt to serialize the session and send it to the datastore.</p>
   *
   * @throws IOException If an error occurs during the serialization of the session.
   *
   * @param session Session to be saved
   */
  @Override
  public void save(Session session) throws IOException {
    log.debug("Persisting session: " + session.getId());

    if (!(session instanceof DatastoreSession)) {
      throw new IOException(
          "The session must be an instance of DatastoreSession to be serialized");
    }
    DatastoreSession datastoreSession = (DatastoreSession) session;

    List<FullEntity> entities = serializeSession(datastoreSession);

    TraceContext datastoreSaveContext = startSpan("Storing the session in the Datastore");
    datastore.put(entities.toArray(new FullEntity[0]));
    endSpan(datastoreSaveContext);
  }

  /**
   * Serialize a session on a list of Entities that can be send to the Datastore.
   * @param session The session to serialize.
   * @return A list of one or more entities containing the session and its attributes.
   * @throws IOException If the session cannot be serialized.
   */
  private List<FullEntity> serializeSession(DatastoreSession session)
      throws IOException {
    TraceContext serializationContext = startSpan("Serialization of the session");
    List<FullEntity> entities = new ArrayList<>();
    Key sessionKey = sessionKeyFactory.newKey(session.getId());
    KeyFactory attributeKeyFactory = datastore.newKeyFactory()
        .setKind(getAttributesKind())
        .addAncestor(PathElement.of(sessionKind, sessionKey.getName()));

    Builder sessionEntity = session.saveMetadataToEntity(sessionKey);
    List<FullEntity> attributes = session.saveAttributesToEntity(attributeKeyFactory);

    if (useUniqueEntity) {
      // Embed all the attributes entities into the session entity.
      sessionEntity.set("attributes",
          attributes.stream().map(EntityValue::of).collect(Collectors.toList())
      );
    } else {
      entities.addAll(attributes);
    }
    entities.add(sessionEntity.build());
    endSpan(serializationContext);
    return entities;
  }

  @Override
  public void processExpires() {
    log.debug("Processing expired sessions");

    Query<Key> query = keyQueryBuilder
        .setFilter(PropertyFilter.le("expirationTime", System.currentTimeMillis()))
        .build();

    QueryResults<Key> keys = datastore.run(query);

    datastore.delete(Streams.stream(keys).toArray(Key[]::new));
  }

  private TraceContext startSpan(String spanName) {
    if (traceRequest) {
      Tracer tracer = Trace.getTracer();
      return tracer.startSpan(spanName);
    } else {
      return null;
    }
  }

  private void endSpan(TraceContext context) {
    if (context != null) {
      Tracer tracer = Trace.getTracer();
      tracer.endSpan(context);
    }
  }

  private String getAttributesKind() {
    return this.sessionKind;
  }

  /**
   * This property will be injected by Tomcat on startup.
   *
   * <p>See context.xml and catalina.properties for the default values</p>
   */
  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  /**
   * This property will be injected by Tomcat on startup.
   */
  public void setSessionKind(String sessionKind) {
    this.sessionKind = sessionKind;
  }

  /**
   * This property will be injected by Tomcat on startup.
   */
  public void setTraceRequest(boolean traceRequest) {
    this.traceRequest = traceRequest;
  }

  /**
   * This property will be injected by Tomcat on startup.
   */
  public void setUseUniqueEntity(boolean useUniqueEntity) {
    this.useUniqueEntity = useUniqueEntity;
  }
}
