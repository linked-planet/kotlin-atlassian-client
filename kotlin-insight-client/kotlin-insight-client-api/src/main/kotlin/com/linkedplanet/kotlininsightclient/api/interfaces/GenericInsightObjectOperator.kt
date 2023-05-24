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
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectId

/**
 * Generic Interface to CRUD one type of domain object, like a customer, to Insight.
 *
 * The idea is that the DomainObject itself is completely unaware of insight,
 * but it is also possible to use a DomainObject that stores the InsightObjectId,
 * which has some performance benefits, but leaks the insight ID into the domain.
 */
interface GenericInsightObjectOperator<DomainType> {

    /**
     * Create a new domain object in insight.
     * @param domainObject The object that should be created inside insight.
     * @return Either the InsightObjectId of the new object or an InsightClientError.
     */
    suspend fun create(domainObject: DomainType): Either<InsightClientError, InsightObjectId>

    /**
     * Updates an existing object.
     * If the object does not exist an InsightClientError is returned.
     * @param domainObject The object that should be updated.
     * @return Either the InsightObjectId of the updated object or an InsightClientError.
     */
    suspend fun update(domainObject: DomainType): Either<InsightClientError, InsightObjectId>

    /**
     * Deletes an existing object
     * If the object does not exist NO error is returned.
     *
     * @param domainObject The object that should be deleted from insight.
     * @return Either Unit in case of success or an InsightClientError.
     */
    suspend fun delete(domainObject: DomainType): Either<InsightClientError, Unit>

    /**
     * Retrieves an existing object from Insight.
     *
     * @param name Retrieves an object by the Attribute "Name". Note that the name of the attribute "Name" could be changed.
     * @return Either the desired object, null if it is not found or an InsightClientError.
     */
    suspend fun getByName(name: String): Either<InsightClientError, DomainType?>

    /**
     * Retrieves an existing object from Insight.
     * This exists for performance reasons. Prefer a unique attribute of your domain object and use getByIQL.
     *
     * @param objectId The InsightObjectId of the object as returned by create/update.
     * @return Either the desired object, null if it is not found or an InsightClientError.
     */
    suspend fun getById(objectId: InsightObjectId): Either<InsightClientError, DomainType?>

    /**
     * Retrieves an existing object from Insight.
     * This exists for performance reasons. Prefer a unique attribute of your domain object and use getByIQL.
     *
     * @param iql A string containing a valid IQL (Insight Query Language)
     * @param withChildren Modifies the iql to contain all subtypes.
     * @param pageFrom: The first page that should be retrieved. Starting from 0.
     * @param perPage: The maximum number of objects that should be returned.
     * @return Either the desired object, null if it is not found or an InsightClientError.
     */
    suspend fun getByIQL(
        iql: String,
        withChildren: Boolean = false,
        pageFrom: Int = 1,
        perPage: Int = Int.MAX_VALUE
    ): Either<InsightClientError, List<DomainType>> // TODO: Move from List to PagedResult

}
