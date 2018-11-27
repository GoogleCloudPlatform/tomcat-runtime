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


/**
 * Implementation of the {@code org.apache.catalina.Manager} interface which uses
 * Google Datastore to share sessions across nodes.
 *
 * <p>This manager should be used in conjunction with {@link KeyValuePersistentValve} and can be
 * used with {@link DatastoreStore}.<br/>
 * Example configuration:</p>
 *
 * <pre>
 *   {@code
 *   <Valve className="com.google.cloud.runtimes.tomcat.session.KeyValuePersistentValve" />
 *   <Manager className="com.google.cloud.runtimes.tomcat.session.DatastoreManager" >
 *     <Store className="com.google.cloud.runtimes.tomcat.session.DatastoreStore" />
 *   </Manager>
 *   }
 * </pre>
 *
 * <p>The sessions is never stored locally and is always fetched from the Datastore.</p>
 */
public class DatastoreManager extends KeyValuePersistentManager<DatastoreSession> {

  @Override
  protected DatastoreSession getNewSession() {
    return new DatastoreSession(this);
  }
}
