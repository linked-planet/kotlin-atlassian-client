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
import com.google.gson.JsonParser
import com.linkedplanet.kotlinhttpclient.api.http.GSON
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectOperator
import com.linkedplanet.kotlininsightclient.api.model.*
import com.linkedplanet.kotlininsightclient.http.model.InsightObjectApiResponse
import com.linkedplanet.kotlininsightclient.http.model.InsightObjectEntries
import com.linkedplanet.kotlininsightclient.http.model.ObjectAttributeValueApiResponse
import com.linkedplanet.kotlininsightclient.http.model.ObjectEditItem
import com.linkedplanet.kotlininsightclient.http.model.ObjectTypeAttributeDefaultTypeApiResponse
import com.linkedplanet.kotlininsightclient.http.model.ObjectUpdateApiResponse
import com.linkedplanet.kotlininsightclient.http.model.getEditAttributes
import com.linkedplanet.kotlininsightclient.http.model.toEditObjectItem
import com.linkedplanet.kotlininsightclient.http.util.toInsightClientError

class HttpInsightObjectOperator(private val context: HttpInsightClientContext) : InsightObjectOperator {

    override var RESULTS_PER_PAGE: Int = 25

    override suspend fun getObjects(
        objectTypeId: InsightObjectTypeId,
        withChildren: Boolean,
        pageFrom: Int,
        perPage: Int
    ): Either<InsightClientError, InsightObjects> = either {
        val iql = getIQLWithChildren(objectTypeId, withChildren)
        getObjectsByIQL(iql, pageFrom, perPage).bind()
    }

    override suspend fun getObjectById(id: InsightObjectId): Either<InsightClientError, InsightObject?> =
        getObjectByPlainIQL("objectId=${id.value}")

    override suspend fun getObjectByKey(key: String): Either<InsightClientError, InsightObject?> =
        getObjectByPlainIQL("Key=\"$key\"")

    override suspend fun getObjectByName(objectTypeId: InsightObjectTypeId, name: String): Either<InsightClientError, InsightObject?> =
        getObjectByPlainIQL("objectTypeId=${objectTypeId.raw} AND Name=\"$name\"")

    override suspend fun getObjectsByObjectTypeName(objectTypeName: String): Either<InsightClientError, List<InsightObject>> {
        val iql = "objectType=$objectTypeName"
        return getObjectsByIQL(iql, 1, Int.MAX_VALUE).map { it.objects }
    }

    override suspend fun getObjectsByIQL(
        objectTypeId: InsightObjectTypeId,
        iql: String,
        withChildren: Boolean,
        pageFrom: Int,
        perPage: Int
    ): Either<InsightClientError, InsightObjects> = either {
        val fullIql = "${getIQLWithChildren(objectTypeId, withChildren)} AND $iql"
        getObjectsByIQL(
            fullIql,
            pageFrom,
            perPage
        ).bind()
    }

    override suspend fun getObjectsByIQL(
        iql: String,
        pageFrom: Int,
        perPage: Int
    ): Either<InsightClientError, InsightObjects> = either {
        val objects = context.httpClient.executeRest<InsightObjectEntries>(
            "GET",
            "rest/insight/1.0/iql/objects",
            mapOf(
                "iql" to iql,
                "includeTypeAttributes" to "true",
                "includeExtendedInfo" to "true",
                "page" to "$pageFrom",
                "resultPerPage" to perPage.toString()
            ),
            null,
            "application/json",
            InsightObjectEntries::class.java
        )
            .map { it.body }
            .mapLeft { it.toInsightClientError() }
            .bind()
            ?.toValues()
        objects ?: InsightObjects(getObjectCount(iql).bind(), emptyList())
    }

    override suspend fun updateObject(obj: InsightObject): Either<InsightClientError, InsightObject> = either {
        context.httpClient.executeRest<ObjectUpdateApiResponse>(
            "PUT",
            "rest/insight/1.0/object/${obj.id.value}",
            emptyMap(),
            GSON.toJson(obj.toEditObjectItem()),
            "application/json",
            ObjectUpdateApiResponse::class.java
        )
            .map { updateResponse -> getObjectById(InsightObjectId(updateResponse.body!!.id)).map { it!! } }
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
        func: suspend (InsightObject) -> Unit
    ): Either<InsightClientError, InsightObject> = either {
        val obj = createEmptyObject(objectTypeId)
        func(obj)
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
        getObjectById(objectId).bind()!!
    }


    // PRIVATE DOWN HERE
    private fun InsightObjectEntries.toValues(): InsightObjects =
        InsightObjects(
            this.totalFilterCount,
            this.objectEntries.map {
                it.toValue()
            }
        )

    /**
     * ATTENTION: Method returns only the first page -> don't use for big result sets...
     */
    private suspend fun getObjectByPlainIQL(
        iql: String
    ): Either<InsightClientError, InsightObject?> = either {
        context.httpClient.executeRest<InsightObjectEntries>(
            "GET",
            "rest/insight/1.0/iql/objects",
            mapOf(
                "iql" to iql,
                "includeTypeAttributes" to "true",
                "includeExtendedInfo" to "true"
            ),
            null,
            "application/json",
            InsightObjectEntries::class.java
        )
            .map { it.body }
            .mapLeft { it.toInsightClientError() }
            .bind()
            ?.toValues()
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
                it.objectTypeAttributeId,
                it.objectTypeAttribute?.name,
                attributeType,
                it.objectTypeAttribute?.defaultType?.let { dt: ObjectTypeAttributeDefaultTypeApiResponse ->
                    ObjectTypeAttributeDefaultType(dt.id, dt.name)
                },
                it.objectTypeAttribute?.options,
                it.objectTypeAttribute?.minimumCardinality,
                it.objectTypeAttribute?.maximumCardinality,
                it.objectAttributeValues.map { av: ObjectAttributeValueApiResponse ->
                    ObjectAttributeValue(av.value, av.displayValue, av.referencedObject?.let { ro ->
                        ReferencedObject(InsightObjectId(ro.id), ro.label, ro.objectKey, ro.objectType?.let { ot ->
                            ReferencedObjectType(InsightObjectTypeId(ot.id), ot.name)
                        })
                    })
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
