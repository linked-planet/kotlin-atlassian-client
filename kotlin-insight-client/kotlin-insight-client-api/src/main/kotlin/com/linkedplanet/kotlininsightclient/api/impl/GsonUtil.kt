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
            .registerTypeAdapterFactory(insightAttributeAdapter)
            .registerTypeAdapterFactory(insightSchemaAttributeAdapter)


        private val zonedDateTimeAdapter = object : TypeAdapter<ZonedDateTime>() {
            override fun write(out: JsonWriter, value: ZonedDateTime?) {
                out.value(value.toString())
            }

            override fun read(`in`: JsonReader): ZonedDateTime {
                return ZonedDateTime.parse(`in`.nextString())
            }
        }

        private val insightAttributeAdapter =
            RuntimeTypeAdapterFactory.of(InsightAttribute::class.java, "type", true)
                .registerSubtype(InsightAttribute.Text::class.java)
                .registerSubtype(InsightAttribute.Integer::class.java)
                .registerSubtype(InsightAttribute.Bool::class.java)
                .registerSubtype(InsightAttribute.DoubleNumber::class.java)
                .registerSubtype(InsightAttribute.Select::class.java)
                .registerSubtype(InsightAttribute.Date::class.java)
                .registerSubtype(InsightAttribute.Time::class.java)
                .registerSubtype(InsightAttribute.DateTime::class.java)
                .registerSubtype(InsightAttribute.Url::class.java)
                .registerSubtype(InsightAttribute.Email::class.java)
                .registerSubtype(InsightAttribute.Textarea::class.java)
                .registerSubtype(InsightAttribute.Ipaddress::class.java)
                .registerSubtype(InsightAttribute.Reference::class.java)
                .registerSubtype(InsightAttribute.User::class.java)
                .registerSubtype(InsightAttribute.Confluence::class.java)
                .registerSubtype(InsightAttribute.Group::class.java)
                .registerSubtype(InsightAttribute.Version::class.java)
                .registerSubtype(InsightAttribute.Project::class.java)
                .registerSubtype(InsightAttribute.Status::class.java)
                .registerSubtype(InsightAttribute.Unknown::class.java)!!

        private val insightSchemaAttributeAdapter =
            RuntimeTypeAdapterFactory.of(ObjectTypeSchemaAttribute::class.java, "type", true)
                .registerSubtype(ObjectTypeSchemaAttribute.TextSchema::class.java, "Text")
                .registerSubtype(ObjectTypeSchemaAttribute.IntegerSchema::class.java, "Integer")
                .registerSubtype(ObjectTypeSchemaAttribute.BoolSchema::class.java, "Bool")
                .registerSubtype(ObjectTypeSchemaAttribute.DoubleNumberSchema::class.java, "DoubleNumber")
                .registerSubtype(ObjectTypeSchemaAttribute.SelectSchema::class.java, "Select")
                .registerSubtype(ObjectTypeSchemaAttribute.DateSchema::class.java, "Date")
                .registerSubtype(ObjectTypeSchemaAttribute.TimeSchema::class.java, "Time")
                .registerSubtype(ObjectTypeSchemaAttribute.DateTimeSchema::class.java, "DateTime")
                .registerSubtype(ObjectTypeSchemaAttribute.UrlSchema::class.java, "Url")
                .registerSubtype(ObjectTypeSchemaAttribute.EmailSchema::class.java, "Email")
                .registerSubtype(ObjectTypeSchemaAttribute.TextareaSchema::class.java, "Textarea")
                .registerSubtype(ObjectTypeSchemaAttribute.IpaddressSchema::class.java, "Ipaddress")
                .registerSubtype(ObjectTypeSchemaAttribute.ReferenceSchema::class.java, "Reference")
                .registerSubtype(ObjectTypeSchemaAttribute.UserSchema::class.java, "User")
                .registerSubtype(ObjectTypeSchemaAttribute.ConfluenceSchema::class.java, "Confluence")
                .registerSubtype(ObjectTypeSchemaAttribute.GroupSchema::class.java, "Group")
                .registerSubtype(ObjectTypeSchemaAttribute.VersionSchema::class.java, "Version")
                .registerSubtype(ObjectTypeSchemaAttribute.ProjectSchema::class.java, "Project")
                .registerSubtype(ObjectTypeSchemaAttribute.StatusSchema::class.java, "Status")
                .registerSubtype(ObjectTypeSchemaAttribute.UnknownSchema::class.java, "Unknown")
    }

}