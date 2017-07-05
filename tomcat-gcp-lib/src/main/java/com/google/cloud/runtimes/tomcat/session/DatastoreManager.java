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
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of the Tomcat `Manager` interface which use
 * Google Datastore to replicate sessions.
 *
 * <p>This manager should be used in conjunction of {@link DatastoreValve}</p>
 */
public class DatastoreManager extends ManagerBase implements StoreManager {

  private static final Log log = LogFactory.getLog(DatastoreManager.class);

  private static final String name = "DatastoreManager";

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
   * Search into the store for an existing session with the specified id.
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
      log.warn("An error occurred during session deserialization" + ex);
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

  public Store getStore() {
    return this.store;
  }

  public void setStore(Store store) {
    this.store = store;
    store.setManager(this);
  }

  @Override
  public void removeSuper(Session session) {
    super.remove(session);
  }

  @Override
  public void remove(Session session) {
    this.removeSuper(session);

    try {
      store.remove(session.getIdInternal());
    } catch (IOException e) {
      log.warn("An error occurred while removing session" + e.getMessage());
    }

  }

  /**
   * {@inheritDoc}
   *
   * <p>Note: Aggregation can be slow on the datastore, avoid repeated usage.</p>
   */
  @Override
  public int getActiveSessionsFull() {
    log.warn("The number of session have be queried, this could cause performance issue");
    return 0;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Note: Listing all the keys can be slow on the datastore.</p>
   */
  @Override
  public Set<String> getSessionIdsFull() {
    Set<String> sessionsId = null;
    try {
      String[] keys = this.store.keys();
      sessionsId = new HashSet<>(Arrays.asList(keys));
    } catch (IOException e) {
      e.printStackTrace();
    }

    return sessionsId;
  }
}
