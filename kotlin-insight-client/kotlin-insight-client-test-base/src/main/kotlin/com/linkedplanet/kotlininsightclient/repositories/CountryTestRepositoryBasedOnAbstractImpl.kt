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
import com.linkedplanet.kotlininsightclient.Country
import com.linkedplanet.kotlininsightclient.InsightObjectType
import com.linkedplanet.kotlininsightclient.TestAttributes
import com.linkedplanet.kotlininsightclient.api.interfaces.identity
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.impl.AbstractInsightObjectRepository
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectOperator
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute.Companion.toValue
import com.linkedplanet.kotlininsightclient.api.model.InsightObject
import com.linkedplanet.kotlininsightclient.api.model.getStringValue

class CountryTestRepositoryBasedOnAbstractImpl(
    override val insightObjectOperator: InsightObjectOperator
) : AbstractInsightObjectRepository<Country>() {

    override var RESULTS_PER_PAGE: Int = Int.MAX_VALUE
    override val objectTypeId = InsightObjectType.Country.id
    private val shortName = TestAttributes.CountryShortName.attributeId
    private val name = TestAttributes.CountryName.attributeId

    override suspend fun loadExistingInsightObject(domainObject: Country): Either<InsightClientError, InsightObject?> {
        return insightObjectOperator.getObjectByName(objectTypeId, domainObject.name, ::identity)
    }

    override suspend fun toDomain(insightObject: InsightObject): Either<InsightClientError, Country> = either {
        Country(
            name = insightObject.getStringValue(name)!!,
            shortName = insightObject.getStringValue(shortName)!!,
        )
    }

    override suspend fun attributesFromDomain(domainObject: Country): Either<InsightClientError, List<InsightAttribute>> =
        either {
            listOf(
                name toValue domainObject.name,
                shortName toValue domainObject.shortName
            )
        }

}
