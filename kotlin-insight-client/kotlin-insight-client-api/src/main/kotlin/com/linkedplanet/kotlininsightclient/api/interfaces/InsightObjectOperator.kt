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
import arrow.core.right
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute
import com.linkedplanet.kotlininsightclient.api.model.InsightObject
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectId
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectPage
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectTypeId

typealias MapToDomain<DomainType> = suspend (InsightObject) -> Either<InsightClientError, DomainType>

/**
 * Function that does not change the input object
 * Pass this as MapToDomain if you want to work directly with InsightObject instead of a specific DomainObject
 */
fun <T> identity(obj: T): Either<InsightClientError, T> = obj.right()

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
     * @param pageIndex The starting page of the paginated list. Starting at 0.
     * @param pageSize The number of results to be returned per page
     * @return Either an [InsightClientError] or an [InsightObjectPage] object containing the list of Insight objects
     */
    suspend fun <T> getObjects(
        objectTypeId: InsightObjectTypeId,
        withChildren: Boolean = false,
        pageIndex: Int = 0,
        pageSize: Int = RESULTS_PER_PAGE,
        toDomain: MapToDomain<T>
    ): Either<InsightClientError, InsightObjectPage<T>>

    /**
     * Retrieves the Insight object with the specified id.
     *
     * @param id The id of the Insight object to retrieve
     * @return Either an [InsightClientError] or the retrieved [InsightObject] object
     */
    suspend fun <T> getObjectById(id: InsightObjectId, toDomain: MapToDomain<T>): Either<InsightClientError, T?>

    /**
     * Retrieves the Insight object with the specified key.
     *
     * @param key The key of the Insight object to retrieve
     * @return Either an [InsightClientError] or the retrieved [InsightObject] object
     */
    suspend fun <T> getObjectByKey(
        key: String,
        toDomain: MapToDomain<T>
    ): Either<InsightClientError, T?>

    /**
     * Retrieves the InsightObject with the specified name and type.
     *
     * @param objectTypeId The id of the Insight object type to retrieve
     * @param name The name of the Insight object to retrieve
     * @return Either an [InsightClientError] or the retrieved [InsightObject] object
     */
    suspend fun <T> getObjectByName(
        objectTypeId: InsightObjectTypeId,
        name: String,
        toDomain: MapToDomain<T>
    ): Either<InsightClientError, T?>

    /**
     * Retrieves a list of Insight objects of the specified type name.
     * WARNING: The Attribute named "Name" could be renamed or removed.
     *
     * @param objectTypeName The name of the Insight object type to retrieve
     * @return Either an [InsightClientError] or a list of [InsightObject] objects
     */
    suspend fun <T> getObjectsByObjectTypeName(
        objectTypeName: String,
        toDomain: MapToDomain<T>
    ): Either<InsightClientError, List<T>>

    /**
     * Retrieves a paginated list of Insight objects of the specified type that match the given IQL query.
     *
     * @param objectTypeId The id of the Insight object type to retrieve
     * @param iql The IQL query to use for filtering
     * @param withChildren Determines whether or not to retrieve child objects as well
     * @param pageIndex The starting page of the paginated list. Starting at 0.
     * @param pageSize The number of results to be returned per page
     * @return Either an [InsightClientError] or an [InsightObjectPage] object containing the filtered list of Insight objects
     */
    suspend fun <T> getObjectsByIQL(
        objectTypeId: InsightObjectTypeId,
        iql: String,
        withChildren: Boolean = false,
        pageIndex: Int = 0,
        pageSize: Int = RESULTS_PER_PAGE,
        toDomain: MapToDomain<T>
    ): Either<InsightClientError, InsightObjectPage<T>>

    /**
     * Retrieves a paginated list of Insight objects that match the given IQL query.
     *
     * @param iql The IQL query to use for filtering
     * @param pageIndex The starting page of the paginated list. Starting at 0.
     * @param pageSize The number of results to be returned per page
     * @return Either an [InsightClientError] or an [InsightObjectPage] object containing the filtered list of Insight objects
     */
    suspend fun <T> getObjectsByIQL(
        iql: String,
        pageIndex: Int = 0,
        pageSize: Int = RESULTS_PER_PAGE,
        toDomain: MapToDomain<T>
    ): Either<InsightClientError, InsightObjectPage<T>>

    /**
     * Returns the number of Insight objects that match the specified IQL (Insight Query Language) statement.
     *
     * @param iql The IQL statement to execute.
     * @return An [Either] that contains either an [InsightClientError] or an [Int] representing the number of matching objects.
     */
    suspend fun getObjectCount(iql: String): Either<InsightClientError, Int>

    /**
     * Updates an existing Insight object in the system.
     * This will overwrite the existing object, so missing attributes will get deleted.
     *
     * @param obj The Insight object to update.
     * @return An [Either] that contains either an [InsightClientError] or an [InsightObject] representing the updated object.
     */
    suspend fun updateObject(obj: InsightObject): Either<InsightClientError, InsightObject>

    /**
     * Updates an existing Insight object in the system.
     * This allows you to only update some attributes while keeping the values for all other attributes.
     *
     * @param insightAttributes: the attributes that will be updated
     * @return An [Either] that contains either an [InsightClientError] or an [InsightObject] representing the updated object.
     */
    suspend fun <T> updateObject(
        objectId: InsightObjectId,
        vararg insightAttributes: InsightAttribute,
        toDomain: MapToDomain<T>
    ): Either<InsightClientError, T>

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
     * @return An [Either] that contains either an [InsightClientError] or an [InsightObject] representing the newly created object.
     */
    suspend fun <T> createObject(
        objectTypeId: InsightObjectTypeId,
        vararg insightAttributes: InsightAttribute,
        toDomain: MapToDomain<T>
    ): Either<InsightClientError, T>

    /**
     * Creates a new Insight object with the specified type and properties.
     *
     * @param objectTypeId The ID of the Insight object type.
     * @param insightAttributes
     */
    suspend fun createObject(
        objectTypeId: InsightObjectTypeId,
        vararg insightAttributes: InsightAttribute,
    ): Either<InsightClientError, InsightObjectId>
}