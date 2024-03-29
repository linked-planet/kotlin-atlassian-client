/*-
 * #%L
 * kotlin-insight-client-http
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
package com.linkedplanet.kotlininsightclient.http

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.flatten
import arrow.core.left
import arrow.core.right
import com.google.gson.JsonParser
import com.linkedplanet.kotlinatlassianclientcore.common.api.JiraUser
import com.linkedplanet.kotlinhttpclient.api.http.GSON
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError.Companion.internalError
import com.linkedplanet.kotlininsightclient.api.error.ObjectNotFoundError
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectOperator
import com.linkedplanet.kotlininsightclient.api.interfaces.MapToDomain
import com.linkedplanet.kotlininsightclient.api.interfaces.identity
import com.linkedplanet.kotlininsightclient.api.model.*
import com.linkedplanet.kotlininsightclient.http.model.DefaultType
import com.linkedplanet.kotlininsightclient.http.model.InsightAttributeApiResponse
import com.linkedplanet.kotlininsightclient.http.model.InsightObjectApiResponse
import com.linkedplanet.kotlininsightclient.http.model.InsightObjectAttributeType
import com.linkedplanet.kotlininsightclient.http.model.InsightObjectEntriesApiResponse
import com.linkedplanet.kotlininsightclient.http.model.ObjectAttributeValueApiResponse
import com.linkedplanet.kotlininsightclient.http.model.ObjectEditItem
import com.linkedplanet.kotlininsightclient.http.model.ObjectTypeAttributeApiResponse
import com.linkedplanet.kotlininsightclient.http.model.ObjectUpdateApiResponse
import com.linkedplanet.kotlininsightclient.http.model.getEditAttributes
import com.linkedplanet.kotlininsightclient.http.model.toEditObjectItem
import com.linkedplanet.kotlininsightclient.http.util.toInsightClientError
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

class HttpInsightObjectOperator(private val context: HttpInsightClientContext) : InsightObjectOperator {

    override var RESULTS_PER_PAGE: Int = 25

    override suspend fun <T> getObjects(
        objectTypeId: InsightObjectTypeId,
        withChildren: Boolean,
        pageIndex: Int,
        pageSize: Int,
        toDomain: MapToDomain<T>
    ): Either<InsightClientError, InsightObjectPage<T>> = either {
        val iql = getIQLWithChildren(objectTypeId, withChildren)
        getObjectsByIQL(iql, pageIndex, pageSize, toDomain).bind()
    }

    override suspend fun <T> getObjectById(id: InsightObjectId, toDomain: MapToDomain<T>): Either<InsightClientError, T?> =
        getObjectByPlainIQL("objectId=${id.raw}", toDomain)

    override suspend fun <T> getObjectByKey(
        key: String,
        toDomain: MapToDomain<T>
    ): Either<InsightClientError, T?> =
        getObjectByPlainIQL("Key=\"$key\"", toDomain)

    override suspend fun <T> getObjectByName(
        objectTypeId: InsightObjectTypeId, name: String,
        toDomain: MapToDomain<T>
    ): Either<InsightClientError, T?> =
        getObjectByPlainIQL("objectTypeId=${objectTypeId.raw} AND Name=\"$name\"", toDomain)

    override suspend fun <T> getObjectsByObjectTypeName(
        objectTypeName: String,
        toDomain: MapToDomain<T>
    ): Either<InsightClientError, List<T>> {
        val iql = "objectType=$objectTypeName"
        return getObjectsByIQL(iql, pageIndex = 0, pageSize = Int.MAX_VALUE, toDomain).map { it.objects }
    }

    override suspend fun <T> getObjectsByIQL(
        objectTypeId: InsightObjectTypeId,
        iql: String,
        withChildren: Boolean,
        pageIndex: Int,
        pageSize: Int,
        toDomain: MapToDomain<T>
    ): Either<InsightClientError, InsightObjectPage<T>> = either {
        val fullIql = "${getIQLWithChildren(objectTypeId, withChildren)} AND $iql"
        getObjectsByIQL(fullIql, pageIndex, pageSize, toDomain).bind()
    }

    override suspend fun <T> getObjectsByIQL(
        iql: String,
        pageIndex: Int,
        pageSize: Int,
        toDomain: MapToDomain<T>
    ): Either<InsightClientError, InsightObjectPage<T>> = either {
        val objects = context.httpClient.executeRest<InsightObjectEntriesApiResponse>(
            "GET",
            "rest/insight/1.0/iql/objects",
            mapOf(
                "iql" to iql,
                "includeTypeAttributes" to "true",
                "includeExtendedInfo" to "true",
                "page" to "${pageIndex + 1}",
                "resultPerPage" to pageSize.toString()
            ),
            null,
            "application/json",
            InsightObjectEntriesApiResponse::class.java
        )
            .map { it.body }
            .mapLeft { it.toInsightClientError() }
            .bind()
            ?.toValues(toDomain)
            ?.bind()
        objects ?: InsightObjectPage(getObjectCount(iql).bind(), emptyList())
    }

    override suspend fun updateInsightObject(
        obj: InsightObject,
    ): Either<InsightClientError, InsightObject> = either {
        val body = GSON.toJson(obj.toEditObjectItem())
        context.httpClient.executeRest<ObjectUpdateApiResponse>(
            "PUT",
            "rest/insight/1.0/object/${obj.id.raw}",
            emptyMap(),
            body,
            "application/json",
            ObjectUpdateApiResponse::class.java
        )
            .map { updateResponse -> getObjectById(InsightObjectId(updateResponse.body!!.id), ::identity).map { it!! } }
            .mapLeft { it.toInsightClientError() }
            .flatten()
            .bind()
    }

    override suspend fun <T> updateInsightObject(
        objectId: InsightObjectId,
        vararg insightAttributes: InsightAttribute,
        toDomain: MapToDomain<T>
    ): Either<InsightClientError, T> = either {
        val obj = (getObjectById(objectId, ::identity).bind()
            ?.right() ?: ObjectNotFoundError(objectId).left()).bind()

        val attributeMap = obj.attributes.associateBy { it.attributeId }.toMutableMap()
        insightAttributes.forEach {
            attributeMap[it.attributeId] = it
        }
        obj.attributes = attributeMap.values.toList()
        val updatedObject = updateInsightObject(obj).bind()
        toDomain(updatedObject).bind()
    }

    override suspend fun deleteObject(id: InsightObjectId): Either<InsightClientError, Unit> =
        context.httpClient.executeRestCall(
            "DELETE",
            "/rest/insight/1.0/object/${id.raw}",
            emptyMap(),
            null,
            "application/json"
        )
            .mapLeft { it.toInsightClientError() }
            .map { /*to Unit*/ }

    override suspend fun createInsightObject(
        objectTypeId: InsightObjectTypeId,
        vararg insightAttributes: InsightAttribute
    ): Either<InsightClientError, InsightObjectId> = either {
        val obj = createEmptyObject(objectTypeId)
        obj.attributes = insightAttributes.toList()
        val editItem = ObjectEditItem(
            obj.objectTypeId.raw,
            obj.getEditAttributes()
        )
        val response = context.httpClient.executeRest<ObjectUpdateApiResponse>(
            "POST",
            "rest/insight/1.0/object/create",
            emptyMap(),
            GSON.toJson(editItem),
            "application/json",
            ObjectUpdateApiResponse::class.java
        )
        val objectId = InsightObjectId(response.mapLeft { it.toInsightClientError() }.bind().body!!.id)
        objectId
    }

    override suspend fun <T> createObject(
        objectTypeId: InsightObjectTypeId,
        vararg insightAttributes: InsightAttribute,
        toDomain: MapToDomain<T>
    ): Either<InsightClientError, T> = either {
        val insightObjectId = createInsightObject(objectTypeId, *insightAttributes).bind()
        (getObjectById(insightObjectId, toDomain).bind()
            ?.right() ?: ObjectNotFoundError(insightObjectId).left()).bind()
    }

    // PRIVATE DOWN HERE
    private suspend fun <T> InsightObjectEntriesApiResponse.toValues(mapper: MapToDomain<T>): Either<InsightClientError, InsightObjectPage<T>> =
        either {
            InsightObjectPage(
                this@toValues.totalFilterCount,
                this@toValues.objectEntries
                    .map { it.toValue().bind() }
                    .map { mapper(it).bind() }
            )
        }

    /**
     * ATTENTION: Method returns only the first page -> don't use for big result sets...
     */
    private suspend fun <T> getObjectByPlainIQL(
        iql: String,
        mapper: MapToDomain<T>
    ): Either<InsightClientError, T?> = either {
        context.httpClient.executeRest<InsightObjectEntriesApiResponse>(
            "GET",
            "rest/insight/1.0/iql/objects",
            mapOf(
                "iql" to iql,
                "includeTypeAttributes" to "true",
                "includeExtendedInfo" to "true"
            ),
            null,
            "application/json",
            InsightObjectEntriesApiResponse::class.java
        )
            .map { it.body }
            .mapLeft { it.toInsightClientError() }
            .bind()
            ?.toValues(mapper)?.bind()
            ?.objects
            ?.firstOrNull()
    }

    override suspend fun getObjectCount(iql: String): Either<InsightClientError, Int> = either {
        context.httpClient.executeGetCall(
            "rest/insight/1.0/iql/objects",
            mapOf(
                "iql" to iql,
                "page" to "1",
                "resultsPerPage" to "1"
            )
        )
            .map { it.body }
            .mapLeft { it.toInsightClientError() }
            .bind()
            .let { response: String ->
                // Keep JsonParser instantiation for downwards compatibility
                @Suppress("DEPRECATION")
                JsonParser().parse(response).asJsonObject.get("totalFilterCount").asInt
            }
    }

    private suspend fun InsightObjectApiResponse.toValue(): Either<InsightClientError, InsightObject> = either {
        val api = this@toValue
        val attributes: List<InsightAttribute> = api.attributes.map {
            insightAttribute(it).bind()
        }
        val objectSelf = "${context.baseUrl}/secure/insight/assets/${api.objectKey}"
        InsightObject(
            InsightObjectTypeId(api.objectType.id),
            InsightObjectId(api.id),
            api.objectType.name,
            api.objectKey,
            api.label,
            attributes,
            api.extendedInfo.attachmentsExists,
            objectSelf
        )
    }

    private suspend fun insightAttribute(apiAttribute: InsightAttributeApiResponse): Either<InsightClientError, InsightAttribute> =
        either {
            val attributeId = InsightAttributeId(apiAttribute.objectTypeAttributeId)
            val schema = apiAttribute.objectTypeAttribute?.let(::mapToObjectTypeSchemaAttribute)
            val attributeType: InsightObjectAttributeType =
                apiAttribute.objectTypeAttribute?.type
                    ?.let { type -> InsightObjectAttributeType.parse(type) }
                    ?: InsightObjectAttributeType.DEFAULT
            when (attributeType) {
                InsightObjectAttributeType.DEFAULT -> handleDefaultValue(attributeId, apiAttribute, schema).bind()
                InsightObjectAttributeType.REFERENCE -> {
                    val referencedObjects =
                        apiAttribute.objectAttributeValues.mapNotNull { av: ObjectAttributeValueApiResponse ->
                            av.referencedObject?.let { ro ->
                                ReferencedObject(
                                    InsightObjectId(ro.id),
                                    ro.label,
                                    ro.objectKey,
                                    ro.objectType?.let { ot ->
                                        ReferencedObjectType(InsightObjectTypeId(ot.id), ot.name)
                                    })
                            }
                        }
                    InsightAttribute.Reference(attributeId, referencedObjects, schema)
                }
                InsightObjectAttributeType.USER -> {
                    val users = apiAttribute.objectAttributeValues.mapNotNull { av: ObjectAttributeValueApiResponse ->
                        av.user?.run { JiraUser(key, name, emailAddress ?: "", displayName = displayName) }
                    }
                    InsightAttribute.User(attributeId, users, schema)
                }
                InsightObjectAttributeType.CONFLUENCE -> InsightAttribute.Confluence(attributeId, schema)
                InsightObjectAttributeType.GROUP -> InsightAttribute.Group(attributeId, schema)
                InsightObjectAttributeType.VERSION -> InsightAttribute.Version(attributeId, schema)
                InsightObjectAttributeType.PROJECT -> InsightAttribute.Project(attributeId, schema)
                InsightObjectAttributeType.STATUS -> InsightAttribute.Status(attributeId, schema)
                else -> internalError("Unsupported objectTypeAttributeBean.type (${attributeType})").bind()
            }
        }

    private suspend fun handleDefaultValue(
        attributeId: InsightAttributeId,
        apiAttribute: InsightAttributeApiResponse,
        schema: ObjectTypeSchemaAttribute?,
    ): Either<InsightClientError, InsightAttribute> = either {
        val defaultType: DefaultType? =
            apiAttribute.objectTypeAttribute?.defaultType
                ?.let { type -> DefaultType.parse(type.id) }

        val values = apiAttribute.objectAttributeValues
        fun singleValue() = values.firstOrNull()?.value as String?
        when (defaultType) {
            DefaultType.TEXT -> InsightAttribute.Text(attributeId, singleValue(), schema)
            DefaultType.INTEGER -> InsightAttribute.Integer(attributeId, singleValue()?.toInt(), schema)
            DefaultType.BOOLEAN -> InsightAttribute.Bool(attributeId, singleValue()?.toBoolean(), schema)
            DefaultType.DOUBLE -> InsightAttribute.DoubleNumber(attributeId, singleValue()?.toDouble(), schema)
            DefaultType.DATE -> {
                val localDate = singleValue()?.let { LocalDate.parse(it) }
                InsightAttribute.Date(attributeId, localDate, values.firstOrNull()?.displayValue as? String?, schema)
            }
            DefaultType.TIME -> {
                val localTime = singleValue()?.let { LocalTime.parse(it) }
                InsightAttribute.Time(attributeId, localTime, values.firstOrNull()?.displayValue as? String?, schema)
            }
            DefaultType.DATE_TIME -> {
                val zonedDateTime = singleValue()?.let { ZonedDateTime.parse(it) }
                InsightAttribute.DateTime(attributeId, zonedDateTime, values.firstOrNull()?.displayValue as? String?, schema)
            }
            DefaultType.EMAIL -> InsightAttribute.Email(attributeId, singleValue(), schema)
            DefaultType.TEXTAREA -> InsightAttribute.Textarea(attributeId, singleValue(), schema)
            DefaultType.IPADDRESS -> InsightAttribute.Ipaddress(attributeId, singleValue(), schema)
            // cardinality > 1
            DefaultType.URL -> InsightAttribute.Url(attributeId, values.mapNotNull { it.value as String? }, schema)
            DefaultType.SELECT -> InsightAttribute.Select(attributeId, values.mapNotNull { it.value as String? }, schema)
            else -> internalError("Unsupported DefaultType (${defaultType})").bind()
        }
    }

    private fun mapToObjectTypeSchemaAttribute(apiAttributeType: ObjectTypeAttributeApiResponse): ObjectTypeSchemaAttribute =
        apiAttributeType.run {
            val iId = InsightAttributeId(id)
            return when (InsightObjectAttributeType.parse(type)) {
                InsightObjectAttributeType.DEFAULT -> mapDefaultType(this, iId)

                InsightObjectAttributeType.REFERENCE -> ObjectTypeSchemaAttribute.ReferenceSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes,
                    referenceObjectTypeId = InsightObjectTypeId(referenceObjectTypeId),
                    referenceKind = ReferenceKind.parse(referenceObjectTypeId)
                )
                InsightObjectAttributeType.USER -> ObjectTypeSchemaAttribute.UserSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )

                InsightObjectAttributeType.CONFLUENCE -> ObjectTypeSchemaAttribute.ConfluenceSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                InsightObjectAttributeType.GROUP -> ObjectTypeSchemaAttribute.GroupSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                InsightObjectAttributeType.VERSION -> ObjectTypeSchemaAttribute.VersionSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                InsightObjectAttributeType.PROJECT -> ObjectTypeSchemaAttribute.ProjectSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                InsightObjectAttributeType.STATUS -> ObjectTypeSchemaAttribute.StatusSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                else -> ObjectTypeSchemaAttribute.UnknownSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes,
                    "HttpInsightObjectOperator: unknown ObjectTypeAttributeApiResponse.id :$id"
                )
            }
        }

    private fun mapDefaultType(apiAttributeType: ObjectTypeAttributeApiResponse, iId: InsightAttributeId) =
        apiAttributeType.run {
            when (defaultType?.id?.let { DefaultType.parse(it) }) {
                DefaultType.TEXT -> ObjectTypeSchemaAttribute.TextSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                DefaultType.INTEGER -> ObjectTypeSchemaAttribute.IntegerSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                DefaultType.BOOLEAN -> ObjectTypeSchemaAttribute.BoolSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                DefaultType.DOUBLE -> ObjectTypeSchemaAttribute.DoubleNumberSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                DefaultType.DATE -> ObjectTypeSchemaAttribute.DateSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                DefaultType.TIME -> ObjectTypeSchemaAttribute.TimeSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                DefaultType.DATE_TIME -> ObjectTypeSchemaAttribute.DateTimeSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                DefaultType.URL -> ObjectTypeSchemaAttribute.UrlSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                DefaultType.EMAIL -> ObjectTypeSchemaAttribute.EmailSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                DefaultType.TEXTAREA -> ObjectTypeSchemaAttribute.TextareaSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                DefaultType.IPADDRESS -> ObjectTypeSchemaAttribute.IpaddressSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                DefaultType.SELECT -> ObjectTypeSchemaAttribute.SelectSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes,
                    options.split(",")
                )
                else -> ObjectTypeSchemaAttribute.UnknownSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes,
                    "HttpInsightObjectOperator: got unknown DefaultType: name:${defaultType?.name} id:${defaultType?.id}"
                )
            }
        }


    private fun getIQLWithChildren(objTypeId: InsightObjectTypeId, withChildren: Boolean): String =
        if (withChildren) {
            "objectType = objectTypeAndChildren(\"${objTypeId.raw}\")"
        } else {
            "objectTypeId=${objTypeId.raw}"
        }

    private fun createEmptyObject(objectTypeId: InsightObjectTypeId): InsightObject {
        return InsightObject(
            objectTypeId,
            InsightObjectId.notPersistedObjectId,
            "",
            "",
            "",
            emptyList(),
            false,
            ""
        )
    }
}
