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
import arrow.core.left
import com.google.gson.reflect.TypeToken
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.error.ObjectTypeNotFoundError
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectTypeOperator
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectAttributeType
import com.linkedplanet.kotlininsightclient.api.model.DefaultType
import com.linkedplanet.kotlininsightclient.api.model.ObjectTypeSchema
import com.linkedplanet.kotlininsightclient.api.model.ObjectTypeSchemaAttribute
import com.linkedplanet.kotlininsightclient.api.model.ReferenceKind
import com.linkedplanet.kotlininsightclient.http.model.ObjectTypeSchemaApiResponse
import com.linkedplanet.kotlininsightclient.http.model.ObjectTypeSchemaAttributeApiResponse
import com.linkedplanet.kotlininsightclient.http.util.toInsightClientError

class HttpInsightObjectTypeOperator(private val context: HttpInsightClientContext) : InsightObjectTypeOperator {

    override suspend fun getObjectType(objectTypeId: Int): Either<InsightClientError, ObjectTypeSchema> = either {
        context.httpClient.executeRest<ObjectTypeSchemaApiResponse>(
            "GET",
            "/rest/insight/1.0/objecttype/$objectTypeId",
            emptyMap(),
            null,
            "application/json",
            object : TypeToken<ObjectTypeSchemaApiResponse>() {}.type
        )
            .map { it.body!! }
            .mapLeft { it.toInsightClientError() }
            .bind()
            .let { populateObjectTypeSchemaAttributes(it).bind() }
            .let { apiResponse ->
                mapToPublicApiModel(apiResponse)
            }
    }

    override suspend fun getObjectTypesBySchemaAndRootObjectType(
        schemaId: Int,
        rootObjectTypeId: Int
    ): Either<InsightClientError, List<ObjectTypeSchema>> = either {
        val allObjectTypes = getObjectTypesBySchema(schemaId).bind()
        allObjectTypes
            .firstOrNull { it.id == rootObjectTypeId }
            ?.let { rootObject ->
                listOf(rootObject).plus(findObjectTypeChildren(allObjectTypes, rootObjectTypeId))
            }
            ?: ObjectTypeNotFoundError().left().bind()
    }

    override suspend fun getObjectTypesBySchema(schemaId: Int): Either<InsightClientError, List<ObjectTypeSchema>> =
        either {
            context.httpClient.executeRestList<ObjectTypeSchemaApiResponse>(
                "GET",
                "rest/insight/1.0/objectschema/$schemaId/objecttypes/flat",
                emptyMap(),
                null,
                "application/json",
                object : TypeToken<List<ObjectTypeSchemaApiResponse>>() {}.type
            )
                .map { it.body }
                .mapLeft { it.toInsightClientError() }
                .bind()
                .map {
                    populateObjectTypeSchemaAttributes(it).bind()
                }.map { apiResponse ->
                    mapToPublicApiModel(apiResponse)
                }
        }

    private fun mapToPublicApiModel(apiResponse: ObjectTypeSchemaApiResponse): ObjectTypeSchema =
        ObjectTypeSchema(
            apiResponse.id,
            apiResponse.name,
            apiResponse.attributes.map { attributeApiResponse: ObjectTypeSchemaAttributeApiResponse ->
                ObjectTypeSchemaAttribute(
                    id = attributeApiResponse.id,
                    name = attributeApiResponse.name,
                    defaultType = attributeApiResponse.defaultType?.id?.let { DefaultType.parse(it) },
                    options = attributeApiResponse.options,
                    minimumCardinality = attributeApiResponse.minimumCardinality,
                    maximumCardinality = attributeApiResponse.maximumCardinality,
                    referenceKind = attributeApiResponse.referenceType?.let {
                        ReferenceKind.parse(it.id)
                    },
                    includeChildObjectTypes = attributeApiResponse.includeChildObjectTypes,
                    referenceObjectTypeId = attributeApiResponse.referenceObjectTypeId,
                    type = InsightObjectAttributeType.parse(attributeApiResponse.id),
                )

            },
            apiResponse.parentObjectTypeId,
        )

    private suspend fun populateObjectTypeSchemaAttributes(objectTypeSchema: ObjectTypeSchemaApiResponse): Either<InsightClientError, ObjectTypeSchemaApiResponse> =
        either {
            val attributes = context.httpClient.executeRestList<ObjectTypeSchemaAttributeApiResponse>(
                "GET",
                "rest/insight/1.0/objecttype/${objectTypeSchema.id}/attributes",
                emptyMap(),
                null,
                "application/json",
                object : TypeToken<List<ObjectTypeSchemaAttributeApiResponse>>() {}.type
            )
                .map { it.body }
                .mapLeft { it.toInsightClientError() }
                .bind()
            objectTypeSchema.attributes = attributes
            objectTypeSchema
        }

    private fun findObjectTypeChildren(
        objectTypes: List<ObjectTypeSchema>,
        rootObjectTypeId: Int
    ): List<ObjectTypeSchema> {
        val directChildren = objectTypes.filter { it.parentObjectTypeId == rootObjectTypeId }
        val transitiveChildren = directChildren.flatMap { child -> findObjectTypeChildren(objectTypes, child.id) }
        return directChildren.plus(transitiveChildren)
    }
}
