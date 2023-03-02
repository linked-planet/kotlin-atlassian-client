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
import com.linkedplanet.kotlininsightclient.api.model.*
import com.linkedplanet.kotlininsightclient.http.util.toInsightClientError

class HttpInsightObjectTypeOperator(private val context: HttpInsightClientContext) : InsightObjectTypeOperator {

    override suspend fun getObjectType(objectTypeId: Int): Either<InsightClientError, ObjectTypeSchema> = either {
        context.httpClient.executeRest<ObjectTypeSchema>(
            "GET",
            "/rest/insight/1.0/objecttype/$objectTypeId",
            emptyMap(),
            null,
            "application/json",
            object : TypeToken<ObjectTypeSchema>() {}.type
        )
            .map { it.body!! }
            .mapLeft { it.toInsightClientError() }
            .bind()
            .let { populateObjectTypeSchemaAttributes(it).bind() }
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
            context.httpClient.executeRestList<ObjectTypeSchema>(
                "GET",
                "rest/insight/1.0/objectschema/$schemaId/objecttypes/flat",
                emptyMap(),
                null,
                "application/json",
                object : TypeToken<List<ObjectTypeSchema>>() {}.type
            )
                .map { it.body }
                .mapLeft { it.toInsightClientError() }
                .bind()
                .map {
                    populateObjectTypeSchemaAttributes(it).bind()
                }
        }

    private suspend fun populateObjectTypeSchemaAttributes(objectTypeSchema: ObjectTypeSchema): Either<InsightClientError, ObjectTypeSchema> =
        either {
            val attributes = context.httpClient.executeRestList<ObjectTypeSchemaAttribute>(
                "GET",
                "rest/insight/1.0/objecttype/${objectTypeSchema.id}/attributes",
                emptyMap(),
                null,
                "application/json",
                object : TypeToken<List<ObjectTypeSchemaAttribute>>() {}.type
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
