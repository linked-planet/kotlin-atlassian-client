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

interface GenericInsightObjectOperator<DomainType> {

    // we have to return DomainType in case someone wants a DomainType with an InsightObjectId
    // so the implementation can return a DomainObject having a non null ID
    suspend fun create(domainObject: DomainType): Either<InsightClientError, DomainType>

    suspend fun update(domainObject: DomainType): Either<InsightClientError, DomainType>

    suspend fun delete(domainObject: DomainType): Either<InsightClientError, Unit>

    suspend fun getByName(name: String): Either<InsightClientError, DomainType?>

    suspend fun getById(objectId: InsightObjectId): Either<InsightClientError, DomainType?>

    suspend fun getByIQL(
        iql: String,
        withChildren: Boolean = false,
        pageFrom: Int = 1,
        perPage: Int = Int.MAX_VALUE
    ): Either<InsightClientError, List<DomainType>>

}
