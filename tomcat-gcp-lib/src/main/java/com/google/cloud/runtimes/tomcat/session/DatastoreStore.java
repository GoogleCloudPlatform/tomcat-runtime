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
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Streams;

import java.io.IOException;
import java.time.Clock;
import java.util.Arrays;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.apache.catalina.LifecycleException;
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
public class DatastoreStore extends KeyValuePersistentStore<Key, DatastoreSession, Entity> {

  private static final Log log = LogFactory.getLog(DatastoreStore.class);

  private Datastore datastore = null;

  /**
   * Name of the kind used in The Datastore for the session.
   */
  private String sessionKind;

  /**
   * Namespace to use in the Datastore.
   */
  private String namespace;

  @VisibleForTesting
  static Function<String, Key> getAttributeKeyFunctionFromFactory(KeyFactory factory) {
    return name -> factory.newKey(name);
  }

  @Override
  protected Class getSessionType() {
    return DatastoreSession.class;
  }

  @Override
  protected Key newKey(String name) {
    return datastore.newKeyFactory().setKind(sessionKind).newKey(name);
  }

  @Override
  protected Iterator<Entity> getEntitiesForKey(Key key) {
    return datastore.run(Query.newEntityQueryBuilder()
        .setKind(sessionKind)
        .setFilter(PropertyFilter.hasAncestor(key))
        .build());
  }

  @Override
  protected void putEntitiesToStore(List<Entity> entities) {
    datastore.put(entities.toArray(new FullEntity[0]));
  }

  @Override
  protected void deleteAttributes(Set<String> names, Function<String, Key> attributeKeyFunction) {
    datastore.delete(names.stream()
        .map(name -> attributeKeyFunction.apply(name))
        .toArray(Key[]::new));
  }

  @Override
  protected Function<String, Key> getAttributeKeyFunction(Key key) {
    return getAttributeKeyFunctionFromFactory(datastore.newKeyFactory()
        .setKind(sessionKind)
        .addAncestor(PathElement.of(sessionKind, key.getName())));
  }

  /**
   * {@inheritDoc}
   *
   * <p>Initiate a connection to the Datastore.</p>
   *
   */
  @Override
  protected synchronized void startInternal() throws LifecycleException {
    log.debug("Initialization of the Datastore Store");

    this.clock = Clock.systemUTC();
    this.datastore = DatastoreOptions.newBuilder().setNamespace(namespace).build().getService();

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
    Query<Key> query = Query.newKeyQueryBuilder().build();
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

    Query<Key> query = Query.newKeyQueryBuilder().build();
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
   * Remove the Session with the specified session identifier from this Store.
   * If no such Session is present, this method takes no action.
   *
   * @param id Session identifier of the session to remove
   */
  @Override
  public void remove(String id) {
    log.debug("Removing session: " + id);
    datastore.delete(newKey(id));
  }

  /**
   * Remove all Sessions from this Store.
   */
  @Override
  public void clear() throws IOException {
    log.debug("Deleting all sessions");
    datastore.delete(Arrays.stream(keys())
                           .map(this::newKey)
                           .toArray(Key[]::new));
  }

  /**
   * Remove expired sessions from the datastore.
   */
  @Override
  public void processExpires() {
    log.debug("Processing expired sessions");

    Query<Key> query = Query.newKeyQueryBuilder().setKind(sessionKind)
        .setFilter(PropertyFilter.le(KeyValuePersistentSession.SessionMetadata.EXPIRATION_TIME,
            clock.millis()))
        .build();

    QueryResults<Key> keys = datastore.run(query);

    Stream<Key> toDelete = Streams.stream(keys)
        .parallel()
        .flatMap(key -> Streams.stream(datastore.run(Query.newKeyQueryBuilder()
                .setKind(sessionKind)
                .setFilter(PropertyFilter.hasAncestor(newKey(key.getName())))
                .build())));
    datastore.delete(toDelete.toArray(Key[]::new));
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


  @VisibleForTesting
  void setDatastore(Datastore datastore) {
    this.datastore = datastore;
  }

}
