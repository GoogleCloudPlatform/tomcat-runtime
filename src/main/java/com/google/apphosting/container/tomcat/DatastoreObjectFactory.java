/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS-IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.google.apphosting.container.tomcat;

import com.google.api.services.datastore.client.Datastore;
import com.google.api.services.datastore.client.DatastoreFactory;
import com.google.api.services.datastore.client.DatastoreHelper;
import com.google.api.services.datastore.client.DatastoreOptions;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import java.util.Hashtable;

/**
 * JNDI ObjectFactory that provides access to Cloud Datastore data sets.
 *
 * In Tomcat, this can be used to define a GlobalNamingResource in server.xml or a context-specific resource in the
 * applications's context.xml descriptor:
 *
 * <blockquote><pre>
 * {@code
 * <Resource name="datastore"
 *           type="com.google.api.services.datastore.client.Datastore"
 *           factory="com.google.apphosting.container.tomcat.DatastoreObjectFactory"
 *           dataset="..."
 *           />
 * }
 * </pre></blockquote>
 */
public class DatastoreObjectFactory implements ObjectFactory {
    // TODO: Consider using the gcloud-java API
    @Override
    public Datastore getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
        Reference ref = (Reference) obj;
        RefAddr dataset = ref.get("dataset");
        if (dataset == null) {
            throw new NamingException("dataset not specified");
        }

        DatastoreOptions.Builder options = DatastoreHelper.getOptionsFromEnv();
        options.dataset(String.valueOf(dataset.getContent()));
        return DatastoreFactory.get().create(options.build());
    }
}
