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
import com.linkedplanet.kotlininsightclient.http.util.toInsightClientError

class HttpInsightObjectOperator(private val context: HttpInsightClientContext) : InsightObjectOperator {

    override var RESULTS_PER_PAGE: Int = 25

    override suspend fun getObjects(
        objectTypeId: Int,
        withChildren: Boolean,
        pageFrom: Int,
        pageTo: Int?,
        perPage: Int
    ): Either<InsightClientError, InsightObjects> = either {
        val iql = getIQLWithChildren(objectTypeId, withChildren)
        getObjectsByIQL(iql, pageFrom, pageTo, perPage).bind()
    }

    override suspend fun getObjectById(id: Int): Either<InsightClientError, InsightObject?> =
        getObjectByPlainIQL("objectId=$id")

    override suspend fun getObjectByKey(key: String): Either<InsightClientError, InsightObject?> =
        getObjectByPlainIQL("Key=\"$key\"")

    override suspend fun getObjectByName(objectTypeId: Int, name: String): Either<InsightClientError, InsightObject?> =
        getObjectByPlainIQL("objectTypeId=$objectTypeId AND Name=\"$name\"")

    override suspend fun getObjectsByIQL(
        objectTypeId: Int,
        withChildren: Boolean,
        iql: String,
        pageFrom: Int,
        pageTo: Int?,
        perPage: Int
    ): Either<InsightClientError, InsightObjects> = either {
        val fullIql = "${getIQLWithChildren(objectTypeId, withChildren)} AND $iql"
        getObjectsByIQL(
            fullIql,
            pageFrom,
            pageTo,
            perPage
        ).bind()
    }

    override suspend fun getObjectsByIQL(
        iql: String,
        pageFrom: Int,
        pageTo: Int?,
        perPage: Int
    ): Either<InsightClientError, InsightObjects> = either {
        val objectsAmount = getObjectCount(iql).bind()
        val maxPage = getObjectPages(iql, perPage).bind()
        val lastPage = pageTo ?: maxPage
        lastPage.let { maxPageSize ->
            (pageFrom..maxPageSize).toList()
        }.flatMap { page ->
            context.httpClient.executeRest<InsightObjectEntries>(
                "GET",
                "rest/insight/1.0/iql/objects",
                mapOf(
                    "iql" to iql,
                    "includeTypeAttributes" to "true",
                    "includeExtendedInfo" to "true",
                    "page" to "$page",
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
                ?.objects
                ?: emptyList()
        }.let {
            InsightObjects(
                objectsAmount,
                it
            )
        }
    }

    override suspend fun updateObject(obj: InsightObject): Either<InsightClientError, InsightObject> = either {
        context.httpClient.executeRest<ObjectUpdateResponse>(
            "PUT",
            "rest/insight/1.0/object/${obj.id}",
            emptyMap(),
            GSON.toJson(obj.toEditObjectItem()),
            "application/json",
            ObjectUpdateResponse::class.java
        )
            .map { updateResponse -> getObjectById(updateResponse.body!!.id).map { it!! } }
            .mapLeft { it.toInsightClientError() }
            .flatten()
            .bind()
    }

    override suspend fun deleteObject(id: Int): Boolean =
        context.httpClient.executeRestCall(
            "DELETE",
            "/rest/insight/1.0/object/$id",
            emptyMap(),
            null,
            "application/json"
        ).fold({ false }, { true })

    override suspend fun createObject(
        objectTypeId: Int,
        func: (InsightObject) -> Unit
    ): Either<InsightClientError, InsightObject> = either {
        val obj = createEmptyObject(objectTypeId)
        func(obj)
        val editItem = ObjectEditItem(
            obj.objectTypeId,
            obj.getEditAttributes()
        )
        // TODO: ensure object type has the specified attributes
        val response = context.httpClient.executeRest<ObjectUpdateResponse>(
            "POST",
            "rest/insight/1.0/object/create",
            emptyMap(),
            GSON.toJson(editItem),
            "application/json",
            ObjectUpdateResponse::class.java
        )
        obj.id = response.mapLeft { it.toInsightClientError() }.bind().body!!.id
        getObjectById(obj.id).bind()!!
    }


    // PRIVATE DOWN HERE
    private suspend fun getObjectPages(
        iql: String,
        resultsPerPage: Int = RESULTS_PER_PAGE
    ): Either<InsightClientError, Int> =
        context.httpClient.executeGetCall(
            "rest/insight/1.0/iql/objects",
            mapOf(
                "iql" to iql,
                "includeTypeAttributes" to "true",
                "includeExtendedInfo" to "true",
                "page" to "1",
                "resultsPerPage" to resultsPerPage.toString()
            )
        )
            .map { response ->
                // Keep JsonParser instantiation for downwards compatibility
                @Suppress("DEPRECATION")
                JsonParser().parse(response.body).asJsonObject.get("toIndex").asInt
            }
            .mapLeft { it.toInsightClientError() }

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
                it.objectAttributeValues
            )
        }
        val objectSelf = "${context.baseUrl}/secure/insight/assets/${this.objectKey}"
        return InsightObject(
            this.objectType.id,
            this.id,
            this.objectType.name,
            this.objectKey,
            this.label,
            attributes,
            this.extendedInfo.attachmentsExists,
            objectSelf
        )
    }

    private fun getIQLWithChildren(objTypeId: Int, withChildren: Boolean): String =
        if (withChildren) {
            "objectType = objectTypeAndChildren(\"$objTypeId\")"
        } else {
            "objectTypeId=$objTypeId"
        }

    private fun createEmptyObject(objectTypeId: Int): InsightObject {
        return InsightObject(
            objectTypeId,
            -1,
            "",
            "",
            "",
            emptyList(),
            false,
            ""
        )
    }
}
