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
import com.linkedplanet.kotlinhttpclient.error.DomainError
import com.linkedplanet.kotlininsightclient.api.InsightConfig
import com.linkedplanet.kotlininsightclient.api.interfaces.ObjectOperatorInterface
import com.linkedplanet.kotlininsightclient.api.model.*

object ObjectOperator : ObjectOperatorInterface {

    override var RESULTS_PER_PAGE: Int = 25

    override suspend fun getObjects(
        objectTypeId: Int,
        withChildren: Boolean,
        pageFrom: Int,
        pageTo: Int?,
        perPage: Int
    ): Either<DomainError, InsightObjects> = either {
        val iql = getIQLWithChildren(objectTypeId, withChildren)
        getObjectsByPlainIQL(iql, pageFrom, pageTo, perPage).bind()
    }

    override suspend fun getObjectById(id: Int): Either<DomainError, InsightObject?> =
        getObjectByPlainIQL("objectId=$id")

    override suspend fun getObjectByKey(key: String): Either<DomainError, InsightObject?> =
        getObjectByPlainIQL("Key=\"$key\"")

    override suspend fun getObjectByName(objectTypeId: Int, name: String): Either<DomainError, InsightObject?> =
        getObjectByPlainIQL("objectTypeId=$objectTypeId AND Name=\"$name\"")

    override suspend fun getObjectsByIQL(
        objectTypeId: Int,
        withChildren: Boolean,
        iql: String,
        pageFrom: Int,
        pageTo: Int?,
        perPage: Int
    ): Either<DomainError, InsightObjects> = either {
        val fullIql = "${getIQLWithChildren(objectTypeId, withChildren)} AND $iql"
        getObjectsByPlainIQL(
            fullIql,
            pageFrom,
            pageTo,
            perPage
        ).bind()
    }

    override suspend fun updateObject(obj: InsightObject): Either<DomainError, InsightObject> = either {
        val objRefEditAttributes = obj.getEditReferences()
        val objEditAttributes = obj.getEditValues()
        val editItem = ObjectEditItem(
            obj.objectType.id,
            objEditAttributes + objRefEditAttributes
        )

        InsightConfig.httpClient.executeRest<ObjectUpdateResponse>(
            "PUT",
            "rest/insight/1.0/object/${obj.id}",
            emptyMap(),
            GSON.toJson(editItem),
            "application/json",
            ObjectUpdateResponse::class.java
        )
            .map {
                getObjectById(it.body!!.id).map { it!! }
            }
            .flatten()
            .bind()
    }

    override suspend fun deleteObject(id: Int): Boolean =
        InsightConfig.httpClient.executeRestCall(
            "DELETE",
            "/rest/insight/1.0/object/$id",
            emptyMap(),
            null,
            "application/json"
        ).fold({ false }, { true })

    override suspend fun createObject(
        objectTypeId: Int,
        func: (InsightObject) -> Unit
    ): Either<DomainError, InsightObject> = either {
        val obj = createEmptyObject(objectTypeId)
        func(obj)
        val objRefEditAttributes = obj.getEditReferences()
        val objEditAttributes = obj.getEditValues()
        val editItem = ObjectEditItem(
            obj.objectType.id,
            objEditAttributes + objRefEditAttributes
        )
        val response = InsightConfig.httpClient.executeRest<ObjectUpdateResponse>(
            "POST",
            "rest/insight/1.0/object/create",
            emptyMap(),
            GSON.toJson(editItem),
            "application/json",
            ObjectUpdateResponse::class.java
        )
        obj.id = response.bind().body!!.id
        getObjectById(obj.id).bind()!!
    }


    // PRIVATE DOWN HERE
    private suspend fun getObjectPages(
        iql: String,
        resultsPerPage: Int = RESULTS_PER_PAGE
    ): Either<DomainError, Int> =
        InsightConfig.httpClient.executeGetCall(
            "rest/insight/1.0/iql/objects",
            mapOf(
                "iql" to iql,
                "includeTypeAttributes" to "true",
                "includeExtendedInfo" to "true",
                "page" to "1",
                "resultsPerPage" to resultsPerPage.toString()
            )
        ).map { response ->
            JsonParser().parse(response.body).asJsonObject.get("toIndex").asInt
        }

    private suspend fun getObjectsByPlainIQL(
        iql: String,
        pageFrom: Int,
        pageTo: Int?,
        perPage: Int
    ): Either<DomainError, InsightObjects> = either {
        val objectsAmount = getObjectCount(iql).bind()
        val maxPage = getObjectPages(iql, perPage).bind()
        val lastPage = pageTo ?: maxPage
        lastPage.let { maxPageSize ->
            (pageFrom..maxPageSize).toList()
        }.flatMap { page ->
            InsightConfig.httpClient.executeRest<InsightObjectEntries>(
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
            ).map { it.body }.bind()?.toValues()?.objects ?: emptyList()
        }.let {
            InsightObjects(
                objectsAmount,
                it
            )
        }
    }

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
    ): Either<DomainError, InsightObject?> = either {
        InsightConfig.httpClient.executeRest<InsightObjectEntries>(
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
        ).map { it.body }.bind()?.toValues()?.objects?.firstOrNull()
    }

    override suspend fun getObjectCount(iql: String): Either<DomainError, Int> = either {
        InsightConfig.httpClient.executeGetCall(
            "rest/insight/1.0/iql/objects",
            mapOf(
                "iql" to iql,
                "page" to "1",
                "resultsPerPage" to "1"
            )
        ).map { it.body }.bind().let { response: String ->
            JsonParser().parse(response).asJsonObject.get("totalFilterCount").asInt
        }
    }

    private fun InsightObjectApiResponse.toValue(): InsightObject {
        val objectType =
            InsightConfig.objectSchemas.first { it.id == this.objectType.id }
        val attributes = this.attributes.map {
            val attributeId = it.objectTypeAttributeId
            InsightAttribute(
                it.objectTypeAttributeId,
                objectType.attributes.firstOrNull { it.id == attributeId }?.name ?: "",
                it.objectAttributeValues
            )
        }
        val objectSelf = "${InsightConfig.baseUrl}/secure/insight/assets/${this.objectKey}"
        return InsightObject(
            objectType,
            this.id,
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
        val schema = InsightConfig.objectSchemas.first { it.id == objectTypeId }
        val attributes = schema.attributes.map {
            InsightAttribute(
                it.id,
                it.name,
                emptyList()
            )
        }
        return InsightObject(
            schema,
            -1,
            "",
            "",
            attributes,
            false,
            ""
        )
    }

    private fun InsightObject.getEditReferences(): List<ObjectEditItemAttribute> =
        this.attributes
            .filter { it.value.any { it.referencedObject != null } }
            .map {
                val values = it.value.map {
                    ObjectEditItemAttributeValue(
                        it.referencedObject!!.id
                    )
                }
                ObjectEditItemAttribute(
                    it.attributeId,
                    values
                )
            }

    private suspend fun InsightObject.getEditValues(): List<ObjectEditItemAttribute> =
        this.attributes
            .filter { it.value.any { it.value != null } || this.isSelectField(it.attributeName) }
            .map {
                val values = it.value.map {
                    ObjectEditItemAttributeValue(
                        it.value
                    )
                }
                ObjectEditItemAttribute(
                    it.attributeId,
                    values
                )
            }

    private fun InsightObject.isSelectField(attributeName: String): Boolean =
        this.getAttributeType(attributeName)?.takeIf { it == "Select" }?.let { true } ?: false

}
