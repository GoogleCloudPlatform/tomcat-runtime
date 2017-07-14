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

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Session;
import org.apache.catalina.Store;
import org.apache.catalina.StoreManager;
import org.apache.catalina.session.ManagerBase;
import org.apache.catalina.session.StoreBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of the {@code org.apache.catalina.Manager} interface which uses the
 * Google Datastore to replicate sessions.
 *
 * <p>This manager should be used in conjunction with {@link DatastoreValve} and can be used
 * with {@link DatastoreStore}.<br/>
 * Example configuration:</p>
 *
 * <pre>
 *   {@code
 *   <Valve className="com.google.cloud.runtimes.tomcat.session.DatastoreValve" />
 *   <Manager className="com.google.cloud.runtimes.tomcat.session.DatastoreManager" >
 *     <Store className="com.google.cloud.runtimes.tomcat.session.DatastoreStore" />
 *   </Manager>
 *   }
 * </pre>
 *
 * <p>The session is never stored locally and always fetched from the datastore.</p>
 */
public class DatastoreManager extends ManagerBase implements StoreManager {

  private static final Log log = LogFactory.getLog(DatastoreManager.class);

  /**
   * The store will be in charge of all the interaction with the Datastore.
   */
  protected Store store = null;

  /**
   * {@inheritDoc}
   *
   * <p>Ensure that a store is present and initialized.</p>
   *
   * @throws LifecycleException If an error occur during the store initialisation
   */
  @Override
  protected synchronized void startInternal() throws LifecycleException {
    super.startInternal();

    if (store == null) {
      throw new LifecycleException("No Store configured, persistence disabled");
    } else if (store instanceof Lifecycle) {
      ((Lifecycle) store).start();
    }

    setState(LifecycleState.STARTING);
  }

  /**
   * Search in the store for an existing session with the specified id.
   *
   * @param id The session id for the session to be returned
   * @return The request session or null if a session with the requested ID could not be found
   * @throws IOException If an input/output error occurs while processing this request
   */
  @Override
  public Session findSession(String id) throws IOException {
    log.debug("Datastore manager is loading session: " + id);
    Session session = null;

    try {
      session = this.getStore().load(id);
    } catch (ClassNotFoundException ex) {
      log.warn("An error occurred during session deserialization", ex);
    }

    return session;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Note: Sessions are loaded at each request therefore no session is loaded at
   * initialization.</p>
   *
   * @throws ClassNotFoundException Cannot occurs
   * @throws IOException Cannot occurs
   */
  @Override
  public void load() throws ClassNotFoundException, IOException {}

  /**
   * {@inheritDoc}
   *
   * <p>Note: Sessions are persisted after each requests but never saved into the local manager,
   * therefore no operation is needed during unload.</p>
   *
   * @throws IOException Cannot occurs
   */
  @Override
  public void unload() throws IOException {}

  /**
   * Remove the Session from the manager but not from the Datastore
   *
   * @param session The session to remove.
   */
  @Override
  public void removeSuper(Session session) {
    super.remove(session);
  }

  /**
   * Remove this Session from the active Sessions and the Datastore.
   *
   * @param session The session to remove.
   */
  @Override
  public void remove(Session session) {
    this.removeSuper(session);

    try {
      store.remove(session.getId());
    } catch (IOException e) {
      log.error("An error occurred while removing session with id: " + session.getId(), e);
    }
  }

  /**
   * Returns the number of session present in the Store.
   *
   * <p>Note: Aggregation can be slow on the Datastore, cache the result if possible</p>
   *
   * @return the session count.
   */
  @Override
  public int getActiveSessionsFull() {
    int sessionCount = 0;
    try {
      sessionCount = store.getSize();
    } catch (IOException e) {
      log.error("An error occurred while counting sessions: ", e);
    }

    return sessionCount;
  }

  /**
   * Returns a set of all sessions IDs or null if an error occur.
   *
   * <p>Note: Listing all the keys can be slow on the Datastore.</p>
   *
   * @return The complete set of sessions IDs across the cluster.
   */
  @Override
  public Set<String> getSessionIdsFull() {
    Set<String> sessionsId = null;
    try {
      String[] keys = this.store.keys();
      sessionsId = new HashSet<>(Arrays.asList(keys));
    } catch (IOException e) {
      log.error("An error occurred while listing active sessions: ", e);
    }

    return sessionsId;
  }

  @Override
  protected void stopInternal() throws LifecycleException {
    super.stopInternal();

    if (store instanceof Lifecycle) {
      ((Lifecycle) store).stop();
    }

    setState(LifecycleState.STOPPING);
  }

  @Override
  public void processExpires() {
    log.debug("Processing expired sessions");
    if (store instanceof StoreBase) {
      ((StoreBase) store).processExpires();
    }
  }

  public Store getStore() {
    return this.store;
  }

  /**
   * The store will be injected by Tomcat on startup.
   *
   * <p>See distributed-session.xml for the configuration.</p>
   */
  public void setStore(Store store) {
    this.store = store;
    store.setManager(this);
  }
}
