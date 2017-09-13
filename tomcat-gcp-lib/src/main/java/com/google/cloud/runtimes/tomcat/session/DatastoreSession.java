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
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.catalina.Manager;
import org.apache.catalina.session.StandardSession;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public class DatastoreSession extends StandardSession {

  private enum SessionMetadata {
    CREATION_TIME("creationTime"),
    LAST_ACCESSED_TIME("lastAccessedTime"),
    MAX_INACTIVE_INTERVAL("maxInactiveInterval"),
    IS_NEW("isNew"),
    IS_VALID("isValid"),
    THIS_ACCESSED_TIME("thisAccessedTime"),
    EXPIRATION_TIME("expiration_time"),
    ATTRIBUTE_VALUE_NAME("value");

    private final String value;

    SessionMetadata(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  private static final Log log = LogFactory.getLog(DatastoreSession.class);

  public DatastoreSession(Manager manager) {
    super(manager);
  }

  /**
   * Restore the attributes and metadata of the session from Datastore Entities.
   *
   * @param metadata An entity containing the metadata of the session.
   * @param attributes An iterator of entity containing the name of each attribute and value
   *                   serialized.
   * @throws ClassNotFoundException The class in attempt to be deserialized is not present in the
   *                                application.
   * @throws IOException Error during the deserialization of the object.
   */
  public void restoreFromEntities(Entity metadata, Iterable<FullEntity> attributes) throws
      ClassNotFoundException, IOException {
    creationTime = metadata.getLong(SessionMetadata.CREATION_TIME.getValue());
    lastAccessedTime = metadata.getLong(SessionMetadata.LAST_ACCESSED_TIME.getValue());
    maxInactiveInterval = (int)metadata.getLong(SessionMetadata.MAX_INACTIVE_INTERVAL.getValue());
    isNew = metadata.getBoolean(SessionMetadata.IS_NEW.getValue());
    isValid = metadata.getBoolean(SessionMetadata.IS_VALID.getValue());
    thisAccessedTime = metadata.getLong(SessionMetadata.THIS_ACCESSED_TIME.getValue());

    for (FullEntity entity : attributes) {
      String name = ((Key) entity.getKey()).getName();
      Blob value = entity.getBlob(SessionMetadata.ATTRIBUTE_VALUE_NAME.getValue());
      try (InputStream fis = value.asInputStream();
          ObjectInputStream ois = new ObjectInputStream(fis)) {
        Object attribute = ois.readObject();
        setAttribute(name, attribute);
      }
    }
  }

  /**
   * Store the metadata of the session in an entity.
   * @param sessionKey Identifier of the session on the Datastore
   * @return An entity containing the metadata.
   */
  public Entity.Builder saveMetadataToEntity(Key sessionKey) {
    Entity.Builder sessionEntity = Entity.newBuilder(sessionKey)
        .set(SessionMetadata.CREATION_TIME.getValue(), getCreationTime())
        .set(SessionMetadata.LAST_ACCESSED_TIME.getValue(), getLastAccessedTime())
        .set(SessionMetadata.MAX_INACTIVE_INTERVAL.getValue(), getMaxInactiveInterval())
        .set(SessionMetadata.IS_NEW.getValue(), isNew())
        .set(SessionMetadata.IS_VALID.getValue(), isValid())
        .set(SessionMetadata.THIS_ACCESSED_TIME.getValue(), getThisAccessedTime());

    // A negative time indicates that the session should never time out
    if (getMaxInactiveInterval() >= 0) {
      sessionEntity.set(SessionMetadata.EXPIRATION_TIME.getValue(),
          getLastAccessedTime() + getMaxInactiveInterval() * 1000);
    }

    return sessionEntity;
  }

  /**
   * Serialize the session attributes into entities.
   * @param attributeKeyFactory The key builder for the entities.
   * @return A list of entities where the key correspond to the name of the attribute
             and the property `value` to the serialized attribute.
   * @throws IOException If an error occur during the serialization.
   */
  public List<FullEntity> saveAttributesToEntity(KeyFactory attributeKeyFactory) throws
      IOException {
    Stream<FullEntity> entities = Collections
        .list(getAttributeNames())
        .stream()
        .filter(name -> isAttributeDistributable(name, getAttribute(name)))
        .map(name -> serializeAttribute(attributeKeyFactory, name));

    try {
      return entities.collect(Collectors.toList());
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
  }

  /**
   * Serialize an attribute an embed it into an entity whose key is generated by the provided
   * KeyFactory.
   * @param attributeKeyFactory The KeyFactory to use to create the key for the entity.
   * @param name The name of the attribute to serialize.
   * @return An Entity containing the serialized attribute.
   */
  private FullEntity serializeAttribute(KeyFactory attributeKeyFactory, String name) {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
      oos.writeObject(getAttribute(name));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    return Entity.newBuilder(attributeKeyFactory.newKey(name))
        .set(SessionMetadata.ATTRIBUTE_VALUE_NAME.getValue(),
            BlobValue.newBuilder(Blob.copyFrom(bos.toByteArray()))
                .setExcludeFromIndexes(true)
                .build())
        .build();
  }

}
