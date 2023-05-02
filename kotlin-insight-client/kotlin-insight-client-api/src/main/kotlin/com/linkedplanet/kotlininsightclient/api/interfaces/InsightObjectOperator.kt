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
import com.linkedplanet.kotlininsightclient.api.model.InsightObject
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectId
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectTypeId
import com.linkedplanet.kotlininsightclient.api.model.InsightObjects

/**
 * The InsightObjectOperator interface provides methods to interact with Insight objects.
 */
interface InsightObjectOperator {

    /**
     * The number of results to be returned per page when requesting a paginated list of Insight objects.
     */
    var RESULTS_PER_PAGE: Int

    /**
     * Retrieves a paginated list of Insight objects of the specified type. Exactly one page will be returned.
     *
     * @param objectTypeId The id of the Insight object type to retrieve
     * @param withChildren Determines whether to retrieve child objects (think java inheritance) as well
     * @param pageFrom The starting page of the paginated list. Starting at 1.
     * @param perPage The number of results to be returned per page
     * @return Either an [InsightClientError] or an [InsightObjects] object containing the list of Insight objects
     */
    suspend fun getObjects(
        objectTypeId: InsightObjectTypeId,
        withChildren: Boolean = false,
        pageFrom: Int = 1,
        perPage: Int = RESULTS_PER_PAGE
    ): Either<InsightClientError, InsightObjects>

    /**
     * Retrieves the Insight object with the specified id.
     *
     * @param id The id of the Insight object to retrieve
     * @return Either an [InsightClientError] or the retrieved [InsightObject] object
     */
    suspend fun getObjectById(id: InsightObjectId): Either<InsightClientError, InsightObject?>

    /**
     * Retrieves the Insight object with the specified key.
     *
     * @param key The key of the Insight object to retrieve
     * @return Either an [InsightClientError] or the retrieved [InsightObject] object
     */
    suspend fun getObjectByKey(key: String): Either<InsightClientError, InsightObject?>

    /**
     * Retrieves the InsightObject with the specified name and type.
     *
     * @param objectTypeId The id of the Insight object type to retrieve
     * @param name The name of the Insight object to retrieve
     * @return Either an [InsightClientError] or the retrieved [InsightObject] object
     */
    suspend fun getObjectByName(objectTypeId: InsightObjectTypeId, name: String): Either<InsightClientError, InsightObject?>

    /**
     * Retrieves a list of Insight objects of the specified type name.
     *
     * @param objectTypeName The name of the Insight object type to retrieve
     * @return Either an [InsightClientError] or a list of [InsightObject] objects
     */
    suspend fun getObjectsByObjectTypeName(objectTypeName: String): Either<InsightClientError, List<InsightObject>>

    /**
     * Retrieves a paginated list of Insight objects of the specified type that match the given IQL query.
     *
     * @param objectTypeId The id of the Insight object type to retrieve
     * @param iql The IQL query to use for filtering
     * @param withChildren Determines whether or not to retrieve child objects as well
     * @param pageFrom The starting page of the paginated list. Starting at 1.
     * @param perPage The number of results to be returned per page
     * @return Either an [InsightClientError] or an [InsightObjects] object containing the filtered list of Insight objects
     */
    suspend fun getObjectsByIQL(
        objectTypeId: InsightObjectTypeId,
        iql: String,
        withChildren: Boolean = false,
        pageFrom: Int = 1,
        perPage: Int = RESULTS_PER_PAGE
    ): Either<InsightClientError, InsightObjects>

    /**
     * Retrieves a paginated list of Insight objects that match the given IQL query.
     *
     * @param iql The IQL query to use for filtering
     * @param pageFrom The starting page of the paginated list. Starting at 1.
     * @param perPage The number of results to be returned per page
     * @return Either an [InsightClientError] or an [InsightObjects] object containing the filtered list of Insight objects
     */
    suspend fun getObjectsByIQL(
        iql: String,
        pageFrom: Int = 1,
        perPage: Int = RESULTS_PER_PAGE
    ): Either<InsightClientError, InsightObjects>

    /**
     * Returns the number of Insight objects that match the specified IQL (Insight Query Language) statement.
     *
     * @param iql The IQL statement to execute.
     * @return An [Either] that contains either an [InsightClientError] or an [Int] representing the number of matching objects.
     */
    suspend fun getObjectCount(iql: String): Either<InsightClientError, Int>

    /**
     * Updates an existing Insight object in the system.
     *
     * @param obj The Insight object to update.
     * @return An [Either] that contains either an [InsightClientError] or an [InsightObject] representing the updated object.
     */
    suspend fun updateObject(obj: InsightObject): Either<InsightClientError, InsightObject>

    /**
     * Deletes the Insight object with the specified ID from the system.
     *
     * @param id The ID of the object to delete.
     * @return An [Either] that contains either an [InsightClientError] or [Unit] if the object was successfully deleted.
     */
    suspend fun deleteObject(id: InsightObjectId): Either<InsightClientError, Unit>

    /**
     * Creates a new Insight object with the specified type and properties.
     *
     * @param objectTypeId The ID of the Insight object type.
     * @param func A suspend function that takes an [InsightObject] parameter and sets the properties of the new object.
     * @return An [Either] that contains either an [InsightClientError] or an [InsightObject] representing the newly created object.
     */
    suspend fun createObject(
        objectTypeId: InsightObjectTypeId,
        func: suspend (InsightObject) -> Unit
    ): Either<InsightClientError, InsightObject>
}