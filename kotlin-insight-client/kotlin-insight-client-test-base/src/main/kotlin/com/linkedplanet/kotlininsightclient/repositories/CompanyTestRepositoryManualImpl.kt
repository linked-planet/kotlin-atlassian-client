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
import arrow.core.computations.either
import arrow.core.right
import com.linkedplanet.kotlininsightclient.Company
import com.linkedplanet.kotlininsightclient.InsightObjectType
import com.linkedplanet.kotlininsightclient.TestAttributes
import com.linkedplanet.kotlininsightclient.api.interfaces.identity
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectRepository
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectOperator
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute.Companion.toReference
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute.Companion.toValue
import com.linkedplanet.kotlininsightclient.api.model.InsightObject
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectId
import com.linkedplanet.kotlininsightclient.api.model.Page
import com.linkedplanet.kotlininsightclient.api.model.getSingleReferenceValue
import com.linkedplanet.kotlininsightclient.api.model.getStringValue

class CompanyTestRepositoryManualImpl(
    private val insightObjectOperator: InsightObjectOperator,
    private val countryTestRepositoryManualImpl: CountryTestRepositoryManualImpl
    ) : InsightObjectRepository<Company>{

    override var RESULTS_PER_PAGE: Int = Int.MAX_VALUE
    private val objectTypeId = InsightObjectType.Company.id
    private val countryRef = TestAttributes.CompanyCountry.attributeId
    private val name = TestAttributes.CompanyName.attributeId

    private suspend fun toDomain(insightObject: InsightObject) = Company(
        name = insightObject.getStringValue(name)!!,
        country = countryTestRepositoryManualImpl.getById(insightObject.getSingleReferenceValue(countryRef)!!.objectId).orNull()!!,
    ).right()

    override suspend fun create(domainObject: Company): Either<InsightClientError, Company> = either {
        insightObjectOperator.createObject(
            objectTypeId,
            name toValue domainObject.name,
            countryRef toReference domainObject.country?.name?.let { countryName ->
                insightObjectOperator.getObjectByName(
                    countryTestRepositoryManualImpl.objectTypeId,
                    countryName,
                    ::identity
                ).bind()?.id
            },
            toDomain = ::toDomain
        ).bind()
    }

    override suspend fun update(domainObject: Company): Either<InsightClientError, Company> = either {
        val objectByName = insightObjectOperator.getObjectByName(objectTypeId, domainObject.name, ::identity).bind()
        if (objectByName == null){
            create(domainObject).bind()
        } else {
            val udpatedObject = insightObjectOperator.updateInsightObject(
                objectByName.id,
                name toValue domainObject.name,
                countryRef toValue domainObject.name,
                toDomain = ::identity
            ).bind()
            toDomain(udpatedObject).bind()
        }
    }

    override suspend fun delete(domainObject: Company): Either<InsightClientError, Unit> = either {
        val objectByName = insightObjectOperator.getObjectByName(objectTypeId, domainObject.name, ::identity).bind()
            ?: return@either
        insightObjectOperator.deleteObject(objectByName.id).bind()
    }

    override suspend fun getByName(name: String): Either<InsightClientError, Company?> =
        insightObjectOperator.getObjectByName(objectTypeId, name, ::toDomain)


    override suspend fun getById(objectId: InsightObjectId): Either<InsightClientError, Company?> =
        insightObjectOperator.getObjectById(objectId, ::toDomain)

    override suspend fun getByIQL(
        iql: String,
        withChildren: Boolean,
        pageIndex: Int,
        pageSize: Int
    ): Either<InsightClientError, Page<Company>> =
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
                page.totalFilterCount / pageSize,
                pageIndex,
                pageSize
            )
        }


}