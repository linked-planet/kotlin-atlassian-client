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
package com.linkedplanet.kotlininsightclient.api.experimental

import arrow.core.Either
import arrow.core.computations.either
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.interfaces.GenericInsightObjectOperator
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectOperator
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute
import com.linkedplanet.kotlininsightclient.api.model.InsightObject
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectId
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectTypeId

abstract class AbstractInsightObjectOperator<DomainType> : GenericInsightObjectOperator<DomainType> {

    abstract val insightObjectOperator: InsightObjectOperator
    abstract val objectTypeId: InsightObjectTypeId

    abstract suspend fun loadExistingInsightObject(domainObject: DomainType): Either<InsightClientError, InsightObject?>
    abstract fun toDomain(insightObject: InsightObject): DomainType
    abstract fun attributesFromDomain(domainObject: DomainType): List<InsightAttribute>

    override suspend fun create(domainObject: DomainType): Either<InsightClientError, InsightObjectId> = either {
        val attributes = attributesFromDomain(domainObject)
        insightObjectOperator.createObject(
            objectTypeId,
            *attributes.toTypedArray()
        ).bind()
    }

    private fun createEmptyObject(objectTypeId: InsightObjectTypeId): InsightObject {
        return InsightObject(
            objectTypeId,
            InsightObjectId.notPersistedObjectId,
            "",
            "",
            "",
            emptyList(),
            false,
            ""
        )
    }

    override suspend fun update(domainObject: DomainType): Either<InsightClientError, InsightObjectId> = either {
        val insightObject = loadExistingInsightObject(domainObject).bind() ?: createEmptyObject(objectTypeId)
        val attributes: List<InsightAttribute> = attributesFromDomain(domainObject)
        val insightObjectWithAttributes = insightObject.copy(attributes = attributes)
        val updatedObject = insightObjectOperator.updateObject(insightObjectWithAttributes).bind()
        updatedObject.id
    }

    override suspend fun delete(domainObject: DomainType): Either<InsightClientError, Unit> = either {
        val insightObject = loadExistingInsightObject(domainObject).bind()
        if (insightObject != null) {
            insightObjectOperator.deleteObject(insightObject.id).bind()
        }
    }

    override suspend fun getByName(name: String): Either<InsightClientError, DomainType?> =
        insightObjectOperator.getObjectByName(objectTypeId, name) { toDomain(it) }

    override suspend fun getById(objectId: InsightObjectId): Either<InsightClientError, DomainType?> =
        insightObjectOperator.getObjectById(objectId) { toDomain(it) }

    override suspend fun getByIQL(
        iql: String,
        withChildren: Boolean,
        pageFrom: Int,
        perPage: Int
    ): Either<InsightClientError, List<DomainType>> =
        insightObjectOperator.getObjectsByIQL(
            objectTypeId,
            iql,
            withChildren,
            pageFrom,
            perPage
        ) { toDomain(it) }
            .map { it.objects }

}