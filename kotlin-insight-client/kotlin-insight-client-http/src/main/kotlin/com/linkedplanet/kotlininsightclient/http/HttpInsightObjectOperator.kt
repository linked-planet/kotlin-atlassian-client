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
import arrow.core.computations.either
import arrow.core.flatten
import arrow.core.rightIfNotNull
import com.google.gson.JsonParser
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
import java.time.ZonedDateTime
import java.util.*

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
        val obj = getObjectById(objectId, ::identity).bind()
            .rightIfNotNull { ObjectNotFoundError(objectId) }.bind()

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
        getObjectById(insightObjectId, toDomain).bind()
            .rightIfNotNull { ObjectNotFoundError(insightObjectId) }.bind()
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
            val attributeType: InsightObjectAttributeType =
                apiAttribute.objectTypeAttribute?.type
                    ?.let { type -> InsightObjectAttributeType.parse(type) }
                    ?: InsightObjectAttributeType.DEFAULT
            val value: ObjectAttributeValue = when (attributeType) {
                InsightObjectAttributeType.DEFAULT -> handleDefaultValue(apiAttribute).bind()
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
                    ObjectAttributeValue.Reference(referencedObjects)
                }
                InsightObjectAttributeType.USER -> {
                    val users = apiAttribute.objectAttributeValues.mapNotNull { av: ObjectAttributeValueApiResponse ->
                        av.user?.run { InsightUser(displayName, name, emailAddress ?: "", key) }
                    }
                    ObjectAttributeValue.User(users)
                }
                InsightObjectAttributeType.CONFLUENCE -> TODO()
                InsightObjectAttributeType.GROUP -> TODO()
                InsightObjectAttributeType.VERSION -> TODO()
                InsightObjectAttributeType.PROJECT -> TODO()
                InsightObjectAttributeType.STATUS -> TODO()
                else -> internalError("Unsupported objectTypeAttributeBean.type (${attributeType})").bind()
            }
            InsightAttribute(
                attributeId = InsightAttributeId(apiAttribute.objectTypeAttributeId),
                value = value,
                schema = apiAttribute.objectTypeAttribute?.let(::mapToObjectTypeSchemaAttribute)
            )
        }

    private suspend fun handleDefaultValue(
        apiAttribute: InsightAttributeApiResponse,
    ): Either<InsightClientError, ObjectAttributeValue> = either {
        val defaultType: DefaultType? =
            apiAttribute.objectTypeAttribute?.defaultType
                ?.let { type -> DefaultType.parse(type.id) }

        val values = apiAttribute.objectAttributeValues
        fun singleValue() = values.firstOrNull()?.value
        when (defaultType) {
            DefaultType.TEXT -> ObjectAttributeValue.Text(singleValue() as String?)
            DefaultType.INTEGER -> ObjectAttributeValue.Integer(singleValue() as Int?)
            DefaultType.BOOLEAN -> ObjectAttributeValue.Bool(singleValue() as Boolean?)
            DefaultType.DOUBLE -> ObjectAttributeValue.DoubleNumber(singleValue() as Double?)
            DefaultType.DATE -> {
                val zonedDateTime = (singleValue() as String?)?.let { ZonedDateTime.parse(it) }
                ObjectAttributeValue.Date(zonedDateTime, values.firstOrNull()?.displayValue as? String?)
            }
            DefaultType.TIME -> {
                val zonedDateTime = (singleValue() as String?)?.let { ZonedDateTime.parse(it) }
                ObjectAttributeValue.Time(zonedDateTime, values.firstOrNull()?.displayValue as? String?)
            }
            DefaultType.DATE_TIME -> {
                val zonedDateTime = (singleValue() as String?)?.let { ZonedDateTime.parse(it) }
                ObjectAttributeValue.DateTime(zonedDateTime, values.firstOrNull()?.displayValue as? String?)
            }
            DefaultType.URL -> ObjectAttributeValue.Url(singleValue() as String?)
            DefaultType.EMAIL -> ObjectAttributeValue.Email(singleValue() as String?)
            DefaultType.TEXTAREA -> ObjectAttributeValue.Textarea(singleValue() as String?)
            DefaultType.IPADDRESS -> ObjectAttributeValue.Ipaddress(singleValue() as String?)
            DefaultType.SELECT -> ObjectAttributeValue.Select(values.mapNotNull { it.value as String? })
            else -> internalError("Unsupported DefaultType (${defaultType})").bind()
        }
    }

    private fun mapToObjectTypeSchemaAttribute(apiAttributeType: ObjectTypeAttributeApiResponse): ObjectTypeSchemaAttribute =
        apiAttributeType.run {
            val iId = InsightAttributeId(id)
            return when (InsightObjectAttributeType.parse(type)) {
                InsightObjectAttributeType.DEFAULT -> mapDefaultType(this, iId)

                InsightObjectAttributeType.REFERENCE -> ObjectTypeSchemaAttribute.Reference(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes,
                    referenceObjectTypeId = InsightObjectTypeId(referenceObjectTypeId),
                    referenceKind = ReferenceKind.parse(referenceObjectTypeId)
                )
                InsightObjectAttributeType.USER -> ObjectTypeSchemaAttribute.User(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )

                InsightObjectAttributeType.CONFLUENCE -> ObjectTypeSchemaAttribute.Confluence(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                InsightObjectAttributeType.GROUP -> ObjectTypeSchemaAttribute.Group(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                InsightObjectAttributeType.VERSION -> ObjectTypeSchemaAttribute.Version(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                InsightObjectAttributeType.PROJECT -> ObjectTypeSchemaAttribute.Project(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                InsightObjectAttributeType.STATUS -> ObjectTypeSchemaAttribute.Status(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                else -> ObjectTypeSchemaAttribute.Unknown(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes,
                    "HttpInsightObjectOperator: unknown ObjectTypeAttributeApiResponse.id :$id"
                )
            }
        }

    private fun mapDefaultType(apiAttributeType: ObjectTypeAttributeApiResponse, iId: InsightAttributeId) =
        apiAttributeType.run {
            when (defaultType?.id?.let { DefaultType.parse(it) }) {
                DefaultType.TEXT -> ObjectTypeSchemaAttribute.Text(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                DefaultType.INTEGER -> ObjectTypeSchemaAttribute.Integer(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                DefaultType.BOOLEAN -> ObjectTypeSchemaAttribute.Bool(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                DefaultType.DOUBLE -> ObjectTypeSchemaAttribute.DoubleNumber(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                DefaultType.DATE -> ObjectTypeSchemaAttribute.Date(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                DefaultType.TIME -> ObjectTypeSchemaAttribute.Time(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                DefaultType.DATE_TIME -> ObjectTypeSchemaAttribute.DateTime(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                DefaultType.URL -> ObjectTypeSchemaAttribute.Url(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                DefaultType.EMAIL -> ObjectTypeSchemaAttribute.Email(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                DefaultType.TEXTAREA -> ObjectTypeSchemaAttribute.Textarea(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                DefaultType.IPADDRESS -> ObjectTypeSchemaAttribute.Ipaddress(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                DefaultType.SELECT -> ObjectTypeSchemaAttribute.Select(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes,
                    options.split(",")
                )
                else -> ObjectTypeSchemaAttribute.Unknown(
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
