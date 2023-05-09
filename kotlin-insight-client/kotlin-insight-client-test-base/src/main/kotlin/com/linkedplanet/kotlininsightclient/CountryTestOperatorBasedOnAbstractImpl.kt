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
import arrow.core.identity
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.experimental.AbstractInsightObjectOperator
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectOperator
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute.Companion.toValue
import com.linkedplanet.kotlininsightclient.api.model.InsightObject
import com.linkedplanet.kotlininsightclient.api.model.getStringValue

class CountryTestOperatorBasedOnAbstractImpl(
    override val insightObjectOperator: InsightObjectOperator
) : AbstractInsightObjectOperator<Country>() {

    override val objectTypeId = InsightObjectType.Company.id
    private val shortName = TestAttributes.CountryShortName.attributeId
    private val name = TestAttributes.CountryName.attributeId

    override suspend fun loadExistingInsightObject(domainObject: Country): Either<InsightClientError, InsightObject?> {
        return insightObjectOperator.getObjectByName(objectTypeId, domainObject.name, ::identity)
    }

    override fun toDomain(insightObject: InsightObject): Country = Country(
        name = insightObject.getStringValue(name)!!,
        shortName = insightObject.getStringValue(shortName)!!,
    )

    override fun attributesFromDomain(domainObject: Country): List<InsightAttribute> = listOf(
        name toValue domainObject.name,
        name toValue domainObject.shortName
    )

}
