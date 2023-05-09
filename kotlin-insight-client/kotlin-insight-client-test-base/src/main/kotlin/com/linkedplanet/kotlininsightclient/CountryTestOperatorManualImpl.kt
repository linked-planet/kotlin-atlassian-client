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
package com.linkedplanet.kotlininsightclient

import arrow.core.Either
import arrow.core.computations.either
import arrow.core.identity
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.interfaces.GenericInsightObjectOperator
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectOperator
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute.Companion.toValue
import com.linkedplanet.kotlininsightclient.api.model.InsightObject
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectId
import com.linkedplanet.kotlininsightclient.api.model.getStringValue
import com.linkedplanet.kotlininsightclient.api.model.setValue


class CountryTestOperatorManualImpl(private val insightObjectOperator: InsightObjectOperator) : GenericInsightObjectOperator<Country>{

    private val objectTypeId = InsightObjectType.Company.id
    private val shortName = TestAttributes.CountryShortName.attributeId
    private val name = TestAttributes.CountryName.attributeId

    private fun toDomain(insightObject: InsightObject) = Country(
        name = insightObject.getStringValue(name)!!,
        shortName = insightObject.getStringValue(shortName)!!,
    )

    override suspend fun create(domainObject: Country): Either<InsightClientError, InsightObjectId> {
        return insightObjectOperator.createObject(objectTypeId,
            name toValue domainObject.name,
            shortName toValue domainObject.name,
        )
    }

    override suspend fun update(domainObject: Country): Either<InsightClientError, InsightObjectId> = either {
        val objectByName = insightObjectOperator.getObjectByName(objectTypeId, domainObject.name, ::identity).bind()
        if (objectByName == null){
            create(domainObject).bind()
        } else {
            objectByName.setValue(name, domainObject.name)
            objectByName.setValue(shortName, domainObject.shortName)
            val updateObject = insightObjectOperator.updateObject(objectByName).bind()
            updateObject.id
        }
    }

    override suspend fun delete(domainObject: Country): Either<InsightClientError, Unit> = either {
        val objectByName = insightObjectOperator.getObjectByName(objectTypeId, domainObject.name, ::identity).bind()
            ?: return@either
        insightObjectOperator.deleteObject(objectByName.id).bind()
    }

    override suspend fun getByName(name: String): Either<InsightClientError, Country?> =
        insightObjectOperator.getObjectByName(objectTypeId, name, ::toDomain)


    override suspend fun getById(objectId: InsightObjectId): Either<InsightClientError, Country?> =
        insightObjectOperator.getObjectById(objectId, ::toDomain)

    override suspend fun getByIQL(
        iql: String,
        withChildren: Boolean,
        pageFrom: Int,
        perPage: Int
    ): Either<InsightClientError, List<Country>> =
        insightObjectOperator.getObjectsByIQL(
            objectTypeId,
            iql,
            withChildren,
            pageFrom,
            perPage,
            ::toDomain
        ).map { it.objects }


}