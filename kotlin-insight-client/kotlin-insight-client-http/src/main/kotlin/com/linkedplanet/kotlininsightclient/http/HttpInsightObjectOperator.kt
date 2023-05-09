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
import arrow.core.identity
import arrow.core.rightIfNotNull
import com.google.gson.JsonParser
import com.linkedplanet.kotlinhttpclient.api.http.GSON
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectOperator
import com.linkedplanet.kotlininsightclient.api.interfaces.MapToDomain
import com.linkedplanet.kotlininsightclient.api.interfaces.MapToInsight
import com.linkedplanet.kotlininsightclient.api.model.*
import com.linkedplanet.kotlininsightclient.http.model.InsightObjectApiResponse
import com.linkedplanet.kotlininsightclient.http.model.InsightObjectEntriesApiResponse
import com.linkedplanet.kotlininsightclient.http.model.ObjectAttributeValueApiResponse
import com.linkedplanet.kotlininsightclient.http.model.ObjectEditItem
import com.linkedplanet.kotlininsightclient.http.model.ObjectTypeAttributeApiResponse
import com.linkedplanet.kotlininsightclient.http.model.ObjectUpdateApiResponse
import com.linkedplanet.kotlininsightclient.http.model.getEditAttributes
import com.linkedplanet.kotlininsightclient.http.model.toEditObjectItem
import com.linkedplanet.kotlininsightclient.http.util.toInsightClientError

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
        getObjectByPlainIQL("objectId=${id.value}", toDomain)

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
        objects ?: InsightObjectPage(getObjectCount(iql).bind(), emptyList())
    }

    override suspend fun updateObject(obj: InsightObject): Either<InsightClientError, InsightObject>{
        return updateObject(obj, ::identity, ::identity)
    }

    override suspend fun <T> updateObject(domainObject: T, toInsight: MapToInsight<T>, toDomain: MapToDomain<T>): Either<InsightClientError, T> = either {
        val obj = toInsight(domainObject)
        context.httpClient.executeRest<ObjectUpdateApiResponse>(
            "PUT",
            "rest/insight/1.0/object/${obj.id.value}",
            emptyMap(),
            GSON.toJson(obj.toEditObjectItem()),
            "application/json",
            ObjectUpdateApiResponse::class.java
        )
            .map { updateResponse -> getObjectById(InsightObjectId(updateResponse.body!!.id), toDomain).map { it!! } }
            .mapLeft { it.toInsightClientError() }
            .flatten()
            .bind()
    }

    override suspend fun deleteObject(id: InsightObjectId): Either<InsightClientError, Unit> =
        context.httpClient.executeRestCall(
            "DELETE",
            "/rest/insight/1.0/object/${id.value}",
            emptyMap(),
            null,
            "application/json"
        )
            .mapLeft { it.toInsightClientError() }
            .map { /*to Unit*/ }

    override suspend fun createObject(
        objectTypeId: InsightObjectTypeId,
        vararg insightAttributes: InsightAttribute
    ): Either<InsightClientError, InsightObjectId> = either {
        val obj = createEmptyObject(objectTypeId)
        obj.attributes = insightAttributes.toList()
        val editItem = ObjectEditItem(
            obj.objectTypeId.raw,
            obj.getEditAttributes()
        )
        // TODO: ensure object type has the specified attributes
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
        val insightObjectId = createObject(objectTypeId, *insightAttributes).bind()
        getObjectById(insightObjectId, toDomain).bind().rightIfNotNull {
            InsightClientError(
                "InsightObject create failed.",
                "Could not retrieve the object after seemingly successful creation."
            )
        }.bind()
    }

    // PRIVATE DOWN HERE
    private fun <T>InsightObjectEntriesApiResponse.toValues(mapper: MapToDomain<T>): InsightObjectPage<T> =
        InsightObjectPage(
            this.totalFilterCount,
            this.objectEntries
                .map { it.toValue() }
                .map(mapper)
        )

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
            ?.toValues(mapper)
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

    private fun InsightObjectApiResponse.toValue(): InsightObject {
        val attributes = this.attributes.map {
            val attributeType =
                it.objectTypeAttribute?.type
                    ?.let { type -> InsightObjectAttributeType.parse(type) }
                    ?: InsightObjectAttributeType.DEFAULT
            InsightAttribute(
                InsightAttributeId(it.objectTypeAttributeId),
                attributeType,
                it.objectAttributeValues.map { av: ObjectAttributeValueApiResponse ->
                    ObjectAttributeValue(
                        value = av.value,
                        displayValue = av.displayValue,
                        referencedObject = av.referencedObject?.let { ro ->
                            ReferencedObject(InsightObjectId(ro.id), ro.label, ro.objectKey, ro.objectType?.let { ot ->
                                ReferencedObjectType(InsightObjectTypeId(ot.id), ot.name)
                            })
                        },
                        user = av.user?.run { InsightUser(displayName, name, emailAddress?: "", key) }
                    )
                },
                schema = it.objectTypeAttribute?.let { type: ObjectTypeAttributeApiResponse ->
                    objectTypeSchemaAttribute(type)
                }
            )
        }
        val objectSelf = "${context.baseUrl}/secure/insight/assets/${this.objectKey}"
        return InsightObject(
            InsightObjectTypeId(this.objectType.id),
            InsightObjectId(this.id),
            this.objectType.name,
            this.objectKey,
            this.label,
            attributes,
            this.extendedInfo.attachmentsExists,
            objectSelf
        )
    }

    private fun objectTypeSchemaAttribute(type: ObjectTypeAttributeApiResponse) =
        ObjectTypeSchemaAttribute(
            id = InsightAttributeId(type.id),
            name = type.name,
            defaultType = type.defaultType?.id?.let { DefaultType.parse(it) },
            options = type.options,
            minimumCardinality = type.minimumCardinality,
            maximumCardinality = type.maximumCardinality,
            referenceKind = ReferenceKind.parse(type.referenceObjectTypeId),
            includeChildObjectTypes = type.includeChildObjectTypes,
            referenceObjectTypeId = InsightObjectTypeId(type.referenceObjectTypeId),
            type = InsightObjectAttributeType.parse(type.type),
        )

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
