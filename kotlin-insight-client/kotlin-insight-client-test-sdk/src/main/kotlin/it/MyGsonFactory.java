/*-
 * #%L
 * kotlin-jira-client-api
 * %%
 * Copyright (C) 2022 - 2023 linked-planet GmbH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package it;

import com.atlassian.plugins.osgi.test.rest.AnnotationTypeAdapter;
import com.atlassian.plugins.osgi.test.rest.AtomicIntegerTypeAdapter;
import com.atlassian.plugins.osgi.test.rest.AtomicLongTypeAdapter;
import com.atlassian.plugins.osgi.test.rest.ClassTypeAdapterFactory;
import com.atlassian.plugins.osgi.test.rest.UpmOptionAdapter;
import com.atlassian.upm.api.util.Option;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class MyGsonFactory {

    private static GsonBuilder instance = null;

    protected MyGsonFactory() {}

    public static Gson getGson() {
        return getInstance().create();
    }

    private static GsonBuilder getInstance() {
        if (instance == null) {
            instance = new GsonBuilder();
            instance.registerTypeAdapter(Annotation.class, new AnnotationTypeAdapter());
            instance.registerTypeAdapter(Option.class, new UpmOptionAdapter());
            instance.registerTypeAdapter(AtomicInteger.class, new AtomicIntegerTypeAdapter());
            instance.registerTypeAdapter(AtomicLong.class, new AtomicLongTypeAdapter());
            instance.registerTypeAdapterFactory(new ClassTypeAdapterFactory());
            instance.addDeserializationExclusionStrategy(new ExclusionStrategy() {
                @Override
                public boolean shouldSkipClass(Class<?> clazz) {
                    return clazz.equals(Serializable.class);
                }

                @Override
                public boolean shouldSkipField(FieldAttributes f) {
                    return false;
                }
            });
        }
        return instance;
    }
}
