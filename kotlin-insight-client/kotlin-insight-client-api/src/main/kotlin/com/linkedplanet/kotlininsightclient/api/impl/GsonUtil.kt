/*-
 * #%L
 * kotlin-insight-client-api
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
package com.linkedplanet.kotlininsightclient.api.impl

import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute
import com.linkedplanet.kotlininsightclient.api.model.ObjectTypeSchemaAttribute
import java.time.ZonedDateTime


class GsonUtil {
    companion object {
        fun gsonBuilder(): GsonBuilder = GsonBuilder()
            .registerTypeAdapter(ZonedDateTime::class.java, zonedDateTimeAdapter)
            .registerTypeAdapterFactory(SealedTypeAdapterFactory.of(InsightAttribute::class))
            .registerTypeAdapterFactory(SealedTypeAdapterFactory.of(ObjectTypeSchemaAttribute::class,
                typeFieldName = "type",
                jsonNameForType = { it.simpleName!!.removeSuffix("Schema") to it }
            ))


        private val zonedDateTimeAdapter = object : TypeAdapter<ZonedDateTime>() {
            override fun write(out: JsonWriter, value: ZonedDateTime?) {
                out.value(value.toString())
            }

            override fun read(`in`: JsonReader): ZonedDateTime {
                return ZonedDateTime.parse(`in`.nextString())
            }
        }
    }
}