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

import org.apache.catalina.Context;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.StoreManager;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import java.io.IOException;
import java.util.regex.Pattern;
import javax.servlet.ServletException;

/**
 * This valve uses the Store Manager to persist the session after each request.
 */
public class DatastoreValve extends ValveBase {

  private static final Log log = LogFactory.getLog(DatastoreValve.class);

  private String uriExcludePattern;

  /**
   * {@inheritDoc}
   *
   * <p>If the manager contain a store, use it to persist the session at the end of the request.</p>
   */
  @Override
  public void invoke(Request request, Response response) throws IOException, ServletException {

    log.debug("Processing request with session:" + request.getRequestedSessionId());

    getNext().invoke(request, response);

    Context context = request.getContext();
    Manager manager = context.getManager();

    Session session = request.getSessionInternal(false);
    if (session != null && !isUriExcluded(request.getRequestURI())) {
      log.debug("Persisting session with id: " + session.getId());
      session.access();
      session.endAccess();

      if (manager instanceof StoreManager) {
        StoreManager storeManager = (StoreManager) manager;
        storeManager.getStore().save(session);
        storeManager.removeSuper(session);
      } else {
        log.error("In order to persist the session the manager must implement StoreManager");
      }
    } else {
      log.debug("Session not persisted (Non existent or the URI is ignored");
    }

  }

  /**
   * Verify if the specified URI should be ignored for session persistence.
   * @param uri The URI of the request
   * @return Whether the URI should be ignored or not
   */
  private boolean isUriExcluded(String uri) {
    return uriExcludePattern != null && Pattern.matches(uriExcludePattern, uri);
  }

  /**
   * This property will be injected by Tomcat on startup.
   */
  public void setUriExcludePattern(String uriExcludePattern) {
    this.uriExcludePattern = uriExcludePattern;
  }
}