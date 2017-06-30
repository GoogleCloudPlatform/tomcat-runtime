package com.google.cloud.runtimes.tomcat.session;

import java.io.IOException;
import java.util.Set;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Session;
import org.apache.catalina.Store;
import org.apache.catalina.StoreManager;
import org.apache.catalina.session.ManagerBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * Implementation of the Tomcat `Manager` interface which use
 * Google Datastore to replicate sessions.
 *
 * This manager should be used in conjunction of {@link DatastoreValve}
 */
public class DatastoreManager extends ManagerBase implements StoreManager {

  private static final Log log = LogFactory.getLog(DatastoreManager.class);

  private static final String name = "DatastoreManager";

  /**
   * Store object which will manage the Session store.
   */
  protected Store store = null;

  /**
   * {@inheritDoc}
   *
   * Ensure that a store is present and initialized.
   * @throws LifecycleException
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
   * {@inheritDoc}
   *
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
      log.warn("An error occurred during session deserialization");
    }

    return session;
  }

  /**
   * {@inheritDoc}
   *
   * @throws ClassNotFoundException Cannot occurs
   * @throws IOException Cannot occurs
   */
  @Override
  public void load() throws ClassNotFoundException, IOException {
    // Sessions are loaded at each request therefore no session is loaded at initialization
  }

  /**
   * {@inheritDoc}
   *
   * @throws IOException Cannot occurs
   */
  @Override
  public void unload() throws IOException {
    // Sessions are persisted after each requests but never saved into the local manager,
    // therefore no operation is needed during unload.
  }

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
   * Note: Aggregation can be slow on the datastore, avoid repeated usage.
   */
  @Override
  public int getActiveSessionsFull() {
    log.warn("The number of session have be queried, this could cause performance issue");
    return 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> getSessionIdsFull() {
    return null;
  }
}
