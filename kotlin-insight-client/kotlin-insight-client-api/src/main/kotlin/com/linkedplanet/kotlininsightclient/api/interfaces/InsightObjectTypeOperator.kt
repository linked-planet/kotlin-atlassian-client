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
package com.linkedplanet.kotlininsightclient.api.interfaces

import arrow.core.Either
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.model.ObjectTypeSchema

/**
 * Provides operations for interacting with InsightObjectTypes in the system that provide meta information about
 * InsightObjects.
 */
interface InsightObjectTypeOperator {

    /**
     * Retrieves the schema for the Insight object type with the specified ID.
     *
     * @param objectTypeId The ID of the Insight object type.
     * @return either an [InsightClientError] or an [ObjectTypeSchema] representing the schema of the object type.
     */
    suspend fun getObjectType(objectTypeId: Int): Either<InsightClientError, ObjectTypeSchema>

    /**
     * Retrieves a list of Insight object types that belong to the schema with the specified ID.
     *
     * @param schemaId The ID of the schema.
     * @return either an [InsightClientError] or a list of [ObjectTypeSchema] objects representing the object types in the schema.
     */
    suspend fun getObjectTypesBySchema(schemaId: Int): Either<InsightClientError, List<ObjectTypeSchema>>

    /**
     * Retrieves a list of Insight object types that belong to the schema with the specified ID and have the specified root object type.
     * So this includes the root type and all of its child types (think: java inheritance)
     *
     * @param schemaId The ID of the schema.
     * @param rootObjectTypeId The ID of the root object type.
     * @return either an [InsightClientError] or a list of [ObjectTypeSchema] objects representing the object types in the schema with the specified root object type.
     */
    suspend fun getObjectTypesBySchemaAndRootObjectType(
        schemaId: Int,
        rootObjectTypeId: Int
    ): Either<InsightClientError, List<ObjectTypeSchema>>
}
