package com.google.cloud.runtimes.tomcat.session;

import java.io.IOException;
import javax.servlet.ServletException;
import org.apache.catalina.Context;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.StoreManager;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * This valve use the Store Manager to persist the session after each request.
 */
public class DatastoreValve extends ValveBase {

  private static final Log log = LogFactory.getLog(DatastoreValve.class);

  /**
   * {@inheritDoc}
   *
   * If the manager contain a store use it to persist the session at the end of the request.
   */
  @Override
  public void invoke(Request request, Response response) throws IOException, ServletException {

    log.debug("Processing request with session:" + request.getRequestedSessionId());

    getNext().invoke(request, response);

    Context context = request.getContext();
    Manager manager = context.getManager();

    Session session = request.getSessionInternal(false);
    if (session != null) {
      log.debug("Persisting session with id: " + session.getId());

      if (manager instanceof StoreManager) {
        StoreManager storeManager = (StoreManager) manager;
        storeManager.getStore().save(session);
        storeManager.removeSuper(session);
      } else {
        log.error("In order to persist the session the manager must implement StoreManager");
      }
    }

  }
}