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
package com.linkedplanet.kotlininsightclient.api.impl

import arrow.core.Either
import arrow.core.computations.either
import arrow.core.rightIfNotNull
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectRepository
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectOperator
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute
import com.linkedplanet.kotlininsightclient.api.model.InsightObject
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectId
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectTypeId
import com.linkedplanet.kotlininsightclient.api.model.Page

abstract class AbstractInsightObjectRepository<DomainType> : InsightObjectRepository<DomainType> {

    abstract val insightObjectOperator: InsightObjectOperator
    abstract val objectTypeId: InsightObjectTypeId

    abstract suspend fun loadExistingInsightObject(domainObject: DomainType): Either<InsightClientError, InsightObject?>

    /**
     * Transforms an InsightObjects to its corresponding domain object.
     * This is a suspend function with either return type, so clients can call insightObjectOperator functions easily.
     */
    abstract suspend fun toDomain(insightObject: InsightObject): Either<InsightClientError, DomainType>
    abstract suspend fun attributesFromDomain(domainObject: DomainType): Either<InsightClientError, List<InsightAttribute>>

    override suspend fun create(domainObject: DomainType): Either<InsightClientError, DomainType> = either {
        val attributes = attributesFromDomain(domainObject).bind()
        insightObjectOperator.createObject(
            objectTypeId,
            *attributes.toTypedArray(),
            toDomain = ::toDomain
        ).bind()
    }

    override suspend fun update(domainObject: DomainType): Either<InsightClientError, DomainType> = either {
        val insightObject = loadExistingInsightObject(domainObject).bind().rightIfNotNull {
            InsightClientError(
                "InsightObject update failed.",
                "Could not retrieve the object."
            )
        }.bind()
        val attributes: List<InsightAttribute> = attributesFromDomain(domainObject).bind()
        val insightObjectWithAttributes = insightObject.copy(attributes = attributes)
        val updatedObject = insightObjectOperator.updateInsightObject(insightObjectWithAttributes).bind()
        toDomain(updatedObject).bind()
    }

    override suspend fun delete(domainObject: DomainType): Either<InsightClientError, Unit> = either {
        val insightObject = loadExistingInsightObject(domainObject).bind()
        if (insightObject != null) {
            insightObjectOperator.deleteObject(insightObject.id).bind()
        }
    }

    override suspend fun getByName(name: String): Either<InsightClientError, DomainType?> =
        insightObjectOperator.getObjectByName(objectTypeId, name, ::toDomain)

    override suspend fun getById(objectId: InsightObjectId): Either<InsightClientError, DomainType?> =
        insightObjectOperator.getObjectById(objectId, ::toDomain)


    override suspend fun getByIQL(
        iql: String,
        withChildren: Boolean,
        pageIndex: Int,
        pageSize: Int
    ): Either<InsightClientError, Page<DomainType>> = either {
        insightObjectOperator.getObjectsByIQL(
            objectTypeId,
            iql,
            withChildren,
            pageIndex,
            pageSize,
            ::toDomain
        )
            .map { page ->
                Page(
                    page.objects,
                    page.totalFilterCount,
                    page.totalFilterCount / pageSize,
                    pageIndex,
                    pageSize
                )
            }.bind()
    }

}
