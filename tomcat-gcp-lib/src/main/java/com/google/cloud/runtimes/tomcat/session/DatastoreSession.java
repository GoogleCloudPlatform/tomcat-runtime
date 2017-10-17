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
import com.google.cloud.datastore.BlobValue;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.common.annotations.VisibleForTesting;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.util.function.Function;
import org.apache.catalina.Manager;

/**
 * A DatastoreSession has the same behavior as a standard session but provide utilities to interact
 * with the Datastore, such as helper for attributes and metadata serialization.
 */
public class DatastoreSession extends KeyValuePersistentSession<Key, Entity> {

  public DatastoreSession(Manager manager) {
    super(manager);
  }

  @Override
  protected Key getKeyForEntity(Entity entity) {
    return entity.getKey();
  }

  @Override
  protected String getNameFromKey(Key key) {
    return key.getName();
  }

  @Override
  protected void setAttributeFromEntity(Entity entity) throws IOException, ClassNotFoundException {
    String name = entity.getKey().getName();
    Blob value = entity.getBlob(SessionMetadata.ATTRIBUTE_VALUE_NAME);
    try (InputStream fis = value.asInputStream();
        ObjectInputStream ois = new ObjectInputStream(fis)) {
      Object attribute = ois.readObject();
      setAttribute(name, attribute, false);
    }
  }

  @Override
  protected void restoreMetadataFromEntity(Entity metadata) {
    creationTime = metadata.getLong(SessionMetadata.CREATION_TIME);
    lastAccessedTime = metadata.getLong(SessionMetadata.LAST_ACCESSED_TIME);
    maxInactiveInterval = (int) metadata.getLong(SessionMetadata.MAX_INACTIVE_INTERVAL);
    isNew = metadata.getBoolean(SessionMetadata.IS_NEW);
    isValid = metadata.getBoolean(SessionMetadata.IS_VALID);
    thisAccessedTime = metadata.getLong(SessionMetadata.THIS_ACCESSED_TIME);
  }

  @VisibleForTesting
  @Override
  protected Entity saveMetadataToEntity(Key sessionKey) {
    Entity.Builder sessionEntity = Entity.newBuilder(sessionKey)
        .set(SessionMetadata.CREATION_TIME, getCreationTime())
        .set(SessionMetadata.LAST_ACCESSED_TIME, getLastAccessedTime())
        .set(SessionMetadata.MAX_INACTIVE_INTERVAL, getMaxInactiveInterval())
        .set(SessionMetadata.IS_NEW, isNew())
        .set(SessionMetadata.IS_VALID, isValid())
        .set(SessionMetadata.THIS_ACCESSED_TIME, getThisAccessedTime());

    // A negative time indicates that the session should never time out
    if (getMaxInactiveInterval() >= 0) {
      sessionEntity.set(SessionMetadata.EXPIRATION_TIME,
          getLastAccessedTime() + getMaxInactiveInterval() * 1000);
    }

    return sessionEntity.build();
  }

  @Override
  protected Entity serializeAttribute(Function<String, Key> attributeKeyFunction, String name) {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
      oos.writeObject(getAttribute(name));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    return Entity.newBuilder(attributeKeyFunction.apply(name))
        .set(SessionMetadata.ATTRIBUTE_VALUE_NAME,
            BlobValue.newBuilder(Blob.copyFrom(bos.toByteArray()))
                .setExcludeFromIndexes(true)
                .build())
        .build();
  }

}
