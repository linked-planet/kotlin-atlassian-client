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
package com.linkedplanet.kotlininsightclient.repositories

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import com.linkedplanet.kotlininsightclient.Country
import com.linkedplanet.kotlininsightclient.InsightObjectType
import com.linkedplanet.kotlininsightclient.TestAttributes
import com.linkedplanet.kotlininsightclient.api.interfaces.identity
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectRepository
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectOperator
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute.Companion.toValue
import com.linkedplanet.kotlininsightclient.api.model.InsightObject
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectId
import com.linkedplanet.kotlinatlassianclientcore.common.api.Page
import com.linkedplanet.kotlininsightclient.api.model.getStringValue
import kotlin.math.ceil

class CountryTestRepositoryManualImpl(private val insightObjectOperator: InsightObjectOperator) : InsightObjectRepository<Country>{

    override var RESULTS_PER_PAGE: Int = Int.MAX_VALUE
    val objectTypeId = InsightObjectType.Country.id
    private val shortName = TestAttributes.CountryShortName.attributeId
    private val name = TestAttributes.CountryName.attributeId

    private fun toDomain(insightObject: InsightObject) = Country(
        name = insightObject.getStringValue(name)!!,
        shortName = insightObject.getStringValue(shortName)!!,
    ).right()

    override suspend fun create(domainObject: Country): Either<InsightClientError, Country> {
        return insightObjectOperator.createObject(objectTypeId,
            name toValue domainObject.name,
            shortName toValue domainObject.shortName,
            toDomain = ::toDomain
        )
    }

    override suspend fun update(domainObject: Country): Either<InsightClientError, Country> = either {
        val objectByName = insightObjectOperator.getObjectByName(objectTypeId, domainObject.name, ::identity).bind()
        if (objectByName == null){
            create(domainObject).bind()
        } else {
            val udpatedObject = insightObjectOperator.updateInsightObject(
                objectByName.id,
                name toValue domainObject.name,
                shortName toValue domainObject.name,
                toDomain = ::identity
            ).bind()
            toDomain(udpatedObject).bind()
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
        pageIndex: Int,
        pageSize: Int
    ): Either<InsightClientError, Page<Country>> =
        insightObjectOperator.getObjectsByIQL(
            objectTypeId,
            iql,
            withChildren,
            pageIndex,
            pageSize,
            ::toDomain
        ).map { page ->
            Page(
                page.objects,
                page.totalFilterCount,
                ceil(page.totalFilterCount.toDouble() / pageSize.toDouble()).toInt(),
                pageIndex,
                pageSize
            )
        }


}