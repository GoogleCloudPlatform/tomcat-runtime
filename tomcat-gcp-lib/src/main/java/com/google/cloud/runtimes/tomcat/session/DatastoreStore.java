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

import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.KeyQuery;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.common.collect.Streams;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.Session;
import org.apache.catalina.session.StandardSession;
import org.apache.catalina.session.StoreBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

/**
 * This store interact with the Datastore service to persist sessions.
 *
 * <p>It does not make any assumptions about the manager, so it could be used
 * by all manager implementations.</p>
 *
 * <p>However aggregations can be slow on the Datastore so if performance is a concern prefer using
 * a manager implementation which is not using aggregations such as {@link DatastoreManager}</p>
 */
public class DatastoreStore extends StoreBase {

  private static final Log log = LogFactory.getLog(DatastoreStore.class);

  private Datastore datastore = null;
  private KeyFactory keyFactory = null;
  private KeyQuery.Builder keyQueryBuilder = null;

  /**
   * Name of the kind used in The Datastore for the session.
   */
  private String sessionKind = "Session";

  /**
   * Namespace to use in the Datastore.
   */
  private String namespace = "tomcat-gcp-persistent-session";

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
    this.keyFactory = datastore.newKeyFactory().setKind(sessionKind);
    this.keyQueryBuilder = Query.newKeyQueryBuilder().setKind(sessionKind);

    super.startInternal();
  }

  /**
   * Return the number of Sessions present in this Store.
   *
   * <p>The Datastore does not support counting element in a collection
   * so all the keys are fetched and the count is computed locally.</p>
   *
   * <p>This method may be slow if a large number of sessions are persisted,
   * prefer operations on individual entity rather than aggregations.</p>
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
   * Return an array containing the session identifiers of all Sessions currently saved in this
   * Store. If there are no such Sessions, a zero-length array is returned.
   *
   * <p>This operation may be slow if a large number of sessions are persisted.
   * Note that the number of key returned may be bounded by the Datastore configuration.</p>
   *
   * @return The ids of all the persisted sessions
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
   * <p>Look in the datastore for a serialized session and attempt to deserialize it.</p>
   *
   * <p>If the session is successfully deserialized, it is added to the current manager and is
   * returned by this method. Otherwise null is returned </p>
   *
   * @param id Session identifier of the session to load
   * @return The loaded session instance
   * @throws ClassNotFoundException If a deserialization error occurs
   */
  @Override
  public Session load(String id) throws ClassNotFoundException, IOException {
    log.debug("Session " + id + " requested");
    StandardSession session = null;

    Entity sessionEntity = datastore.get(keyFactory.newKey(id));

    if (sessionEntity != null) {
      try {
        InputStream fis = sessionEntity.getBlob("content").asInputStream();
        ObjectInputStream ois = getObjectInputStream(fis);
        session = (StandardSession) manager.createEmptySession();
        session.readObjectData(ois);
        session.setManager(manager);
      } catch (IOException e) {
        log.warn("An error occurred during session deserialization");
        session = null;
      }
    }

    log.debug("Session " + id + " loaded");
    return session;
  }

  /**
   * Remove the Session with the specified session identifier from this Store, if present.
   * If no such Session is present, this method takes no action.
   *
   * @param id Session identifier of the session to remove
   */
  @Override
  public void remove(String id) {
    log.debug("Removing session: " + id);
    datastore.delete(keyFactory.newKey(id));
  }

  /**
   * Remove all Sessions from this Store.
   */
  @Override
  public void clear() throws IOException {
    log.debug("Deleting all sessions");
    datastore.delete(Arrays.stream(keys())
                           .map(keyFactory::newKey)
                           .toArray(Key[]::new));
  }

  /**
   * Save the specified Session into this Store. Any previously saved information for
   * the associated session identifier is replaced.
   *
   * <p>Attempt to serialize the session and send it to the datastore.</p>
   *
   * @param session Session to be saved
   */
  @Override
  public void save(Session session) throws IOException {
    log.debug("Persisting session: " + session.getId());

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(bos);
    if (!(session instanceof StandardSession)) {
      throw new IOException("The session must be an instance of StandardSession to be serialized");
    }
    ((StandardSession)session).writeObjectData(oos);
    oos.flush();

    Entity sessionEntity = Entity.newBuilder(keyFactory.newKey(session.getId()))
        .set("content", Blob.copyFrom(bos.toByteArray()))
        .build();

    datastore.put(sessionEntity);
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public void setSessionKind(String sessionKind) {
    this.sessionKind = sessionKind;
  }
}
