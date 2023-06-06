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
import com.linkedplanet.kotlininsightclient.Company
import com.linkedplanet.kotlininsightclient.Country
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.impl.AbstractNameMappedRepository
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectOperator
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectTypeOperator
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightSchemaOperator
import com.linkedplanet.kotlininsightclient.api.interfaces.identity
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute
import com.linkedplanet.kotlininsightclient.api.model.InsightObject
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectId
import com.linkedplanet.kotlininsightclient.api.model.ObjectTypeSchemaAttribute

class CompanyRepositoryBasedOnNameMapping(
    override val insightObjectOperator: InsightObjectOperator,
    override val insightObjectTypeOperator: InsightObjectTypeOperator,
    override val insightSchemaOperator: InsightSchemaOperator
) : AbstractNameMappedRepository<Company>(Company::class) {

    private val countryOperator = CountryRepositoryBasedOnNameMapping(
        insightObjectOperator, insightObjectTypeOperator, insightSchemaOperator
    )

    override suspend fun loadExistingInsightObject(domainObject: Company): Either<InsightClientError, InsightObject?> =
        insightObjectOperator.getObjectByName(objectTypeId, domainObject.name, ::identity)

    override suspend fun referenceAttributeToValue(attribute: InsightAttribute): Any? {
        val country = (attribute as InsightAttribute.Reference).referencedObjects.first().id
        val eitherCountry = countryOperator.getById(country)
        return eitherCountry.orNull()
    }

    override suspend fun attributeToReferencedObjectId(
        schemaAttribute: ObjectTypeSchemaAttribute,
        value: Any?
    ): List<InsightObjectId> {
        val country = value as Country
        return listOfNotNull(
            insightObjectOperator.getObjectByName(
                objectTypeId = (schemaAttribute as ObjectTypeSchemaAttribute.Reference).referenceObjectTypeId,
                name = country.name,
                toDomain = ::identity
            )
                .orNull()?.id
        )
    }

}
