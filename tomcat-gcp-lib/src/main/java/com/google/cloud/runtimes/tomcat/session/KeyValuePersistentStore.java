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

import com.google.cloud.trace.Trace;
import com.google.cloud.trace.Tracer;
import com.google.cloud.trace.core.TraceContext;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.time.Clock;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.apache.catalina.Session;
import org.apache.catalina.session.StoreBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * Partial implementation of {@link StoreBase}. Contains logic common to key-value persistent
 * session storage while leaving the specific storage types as parameters.
 * @param <K> the key type of the storage
 * @param <S> The session type based on the key and entity types
 * @param <E> the storage entity type of the storage
 */
public abstract class KeyValuePersistentStore<K, S extends KeyValuePersistentSession<K, E>, E>
    extends StoreBase {

  private final Log log = LogFactory.getLog(this.getClass());
  protected Clock clock;
  /**
   * Whether or not to send traces to Stackdriver for the operations related to session
   * persistence.
   */
  private boolean traceRequest = false;

  protected abstract Class getSessionType();

  protected abstract K newKey(String name);

  protected abstract Iterator<E> getEntitiesForKey(K key);

  protected abstract void putEntitiesToStore(List<E> entities);

  protected abstract void deleteAttributes(Set<String> names,
      Function<String, K> attributeKeyFunction);

  protected abstract Function<String, K> getAttributeKeyFunction(K key);

  /**
   * Load and return the Session associated with the specified session identifier from this Store,
   * without removing it. If there is no such stored Session, return null.
   *
   * <p>Look in the storage for a serialized session and attempt to deserialize it.</p>
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
    TraceContext context = startSpan("Loading session");
    K sessionKey = newKey(id);

    S session = deserializeSession(sessionKey);

    endSpan(context);
    log.debug("Session " + id + " loaded");
    return session;
  }

  /**
   * Create a new session usable by Tomcat, from a serialized session in a storage entity.
   *
   * @param sessionKey The key associated with the session metadata and attributes.
   * @return A new session containing the metadata and attributes stored in the entity.
   * @throws ClassNotFoundException Thrown if a class serialized in the entity is not available in
   *        this context.
   * @throws IOException Thrown when an error occur during the deserialization.
   */
  private S deserializeSession(K sessionKey)
      throws ClassNotFoundException, IOException {
    TraceContext loadingSessionContext = startSpan(
        "Fetching the session from KeyValuePersistentStore");
    Iterator<E> entities = getEntitiesForKey(sessionKey);
    endSpan(loadingSessionContext);

    S session = null;
    if (entities.hasNext()) {
      session = (S) manager.createEmptySession();
      TraceContext deserializationContext = startSpan("Deserialization of the session");
      session.restoreFromEntities(sessionKey, Lists.newArrayList(entities));
      endSpan(deserializationContext);
    }
    return session;
  }

  /**
   * Save the specified Session into this Store. Any previously saved information for the associated
   * session identifier is replaced.
   *
   * <p>Attempt to serialize the session and send it to the storage.</p>
   *
   * @param session Session to be saved
   * @throws IOException If an error occurs during the serialization of the session.
   */
  @Override
  public void save(Session session) throws IOException {
    log.debug("Persisting session: " + session.getId());

    if (!(session.getClass().equals(getSessionType()))) {
      throw new IOException(
          "The session must be an instance of " + getSessionType().getSimpleName()
              + " to be serialized");
    }
    S typedSession = (S) session;
    K sessionKey = newKey(session.getId());
    Function<String, K> attributeKeyFunction = getAttributeKeyFunction(sessionKey);
    List<E> entities = serializeSession(typedSession, sessionKey, attributeKeyFunction);

    TraceContext datastoreSaveContext = startSpan("Storing the session in the Datastore");
    putEntitiesToStore(entities);
    deleteAttributes(typedSession.getRemovedAttributes(), attributeKeyFunction);
    endSpan(datastoreSaveContext);
  }

  /**
   * Serialize a session to a list of Entities that can be stored to the storage.
   *
   * @param session The session to serialize.
   * @return A list of one or more entities containing the session and its attributes.
   * @throws IOException If the session cannot be serialized.
   */
  @VisibleForTesting
  List<E> serializeSession(S session, K sessionKey,
      Function<String, K> attributeKeyFunction) throws IOException {
    TraceContext serializationContext = startSpan("Serialization of the session");
    List<E> entities = session.saveToEntities(sessionKey, attributeKeyFunction);
    endSpan(serializationContext);
    return entities;
  }

  @VisibleForTesting
  TraceContext startSpan(String spanName) {
    TraceContext context = null;
    if (traceRequest) {
      context = Trace.getTracer().startSpan(spanName);
    }
    return context;
  }

  @VisibleForTesting
  private void endSpan(TraceContext context) {
    if (context != null) {
      Tracer tracer = Trace.getTracer();
      tracer.endSpan(context);
    }
  }

  /**
   * This property will be injected by Tomcat on startup.
   */
  public void setTraceRequest(boolean traceRequest) {
    this.traceRequest = traceRequest;
  }

  @VisibleForTesting
  void setClock(Clock clock) {
    this.clock = clock;
  }

}
