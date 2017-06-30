package com.google.cloud.runtimes.tomcat.session;

import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.common.collect.Streams;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Session;
import org.apache.catalina.session.StandardSession;
import org.apache.catalina.session.StoreBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * This store interact with the datastore service to persist and manage sessions.
 *
 * It does not make any assumptions about the manager, so it could be used
 * by all manager implementations.
 *
 * However aggregations are slow on the Datastore so for performance prefer using a manager
 * who is not using aggregations such as {@link DatastoreManager}
 */
public class DatastoreStore extends StoreBase {

  private static final Log log = LogFactory.getLog(DatastoreStore.class);

  private Datastore datastore = null;
  private KeyFactory keyFactory = null;

  /**
   * Name of the kind use in datastore for the session.
   */
  private String kind = "Session";

  /**
   * Namespace to use in datastore.
   * TODO(cassand): Actually use namespace
   */
  private String namespace = "";

  /**
   * {@inheritDoc}
   *
   * Initiate a connection to the datastore.
   * @throws LifecycleException
   */
  @Override
  protected synchronized void startInternal() throws LifecycleException {

    log.debug("Start Datastore Store");

    this.datastore = DatastoreOptions.getDefaultInstance().getService();
    this.keyFactory = datastore.newKeyFactory().setKind(kind);

    super.startInternal();
  }

  /**
   * {@inheritDoc}
   *
   * Datastore does not support counting element in a collection
   * so all the keys are fetched and the count is compute locally.
   *
   * This method may be slow if a large number of sessions are persisted,
   * prefer operations on individual entity rather than aggregations.
   * @return
   */
  @Override
  public int getSize() throws IOException {
    log.debug("Accessing sessions count, be cautious this operation is not suited for datastore");
    Query<Key> query = Query.newKeyQueryBuilder().setKind(kind).build();
    QueryResults<Key> results = datastore.run(query);
    long count = Streams.stream(results).count();
    return Math.toIntExact(count);
  }

  /**
   * {@inheritDoc}
   *
   * Fetch all the sessions id present in the datastore.
   *
   * This operation may be slow if a large number of sessions are persisted.
   * Note that the number of key returned may be bounded by the datastore configuration.
   *
   * @return The ids of all the persisted sessions
   */
  @Override
  public String[] keys() throws IOException {
    log.debug("Enumerating all the sessions keys, be cautious there is no caching of this keys");
    String[] keys;

    Query<Key> query = Query.newKeyQueryBuilder().setKind(kind).build();
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
   * {@inheritDoc}
   *
   * Look in the datastore for a serialized session and attempt to deserialize it.
   *
   * If the session is successfully deserialized it is added to the current manage and
   * return by this function.
   *
   * Otherwise null is returned
   *
   * @param id Session identifier of the session to load
   * @return The loaded session instance
   * @throws ClassNotFoundException If a deserialization error occurs
   */
  @Override
  public Session load(String id) throws ClassNotFoundException, IOException {
    log.debug("Session " + id + " accessed");
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

    return session;
  }

  /**
   * {@inheritDoc}
   *
   * Remove the session from the datastore.
   * @param id Session identifier of the session to remove
   */
  @Override
  public void remove(String id) {
    log.debug("Removing session: " + id);
    datastore.delete(keyFactory.newKey(id));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clear() throws IOException {
    log.debug("Deleting all sessions");
    Arrays.stream(keys()).forEach(this::remove);
  }

  /**
   * {@inheritDoc}
   *
   * Attempt to serialize the session and to store it in the datastore.
   *
   * @param session Session to be saved
   */
  @Override
  public void save(Session session) throws IOException {
    log.debug("Persisting session: " + session.getId());

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(bos));
    ((StandardSession)session).writeObjectData(oos);
    oos.flush();
    byte[] serializedSession = bos.toByteArray();

    Entity sessionEntity = Entity.newBuilder(keyFactory.newKey(session.getId()))
        .set("content", Blob.copyFrom(serializedSession))
        .build();

    datastore.put(sessionEntity);
  }
}
