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
import com.linkedplanet.kotlininsightclient.InsightObjectType
import com.linkedplanet.kotlininsightclient.ObjectWithAllDefaultTypes
import com.linkedplanet.kotlininsightclient.ObjectWithAllDefaultTypesAttributeIds.*
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.impl.AbstractInsightObjectRepository
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectOperator
import com.linkedplanet.kotlininsightclient.api.interfaces.identity
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute.Companion.toEmailValue
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute.Companion.toIpaddressValue
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute.Companion.toSelectValues
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute.Companion.toTextareaValue
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute.Companion.toUrlValues
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute.Companion.toValue
import com.linkedplanet.kotlininsightclient.api.model.InsightObject
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectTypeId
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute.Integer
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute.Text
import com.linkedplanet.kotlininsightclient.api.model.getAttributeValue

class ObjectWithAllDefaultTypesRepository(
    override val insightObjectOperator: InsightObjectOperator
) : AbstractInsightObjectRepository<ObjectWithAllDefaultTypes>() {
    override var RESULTS_PER_PAGE: Int = Int.MAX_VALUE
    override val objectTypeId: InsightObjectTypeId = InsightObjectType.ObjectWithAllDefaultTypes.id

    override suspend fun loadExistingInsightObject(domainObject: ObjectWithAllDefaultTypes): Either<InsightClientError, InsightObject?> =
        insightObjectOperator.getObjectById(domainObject.id!!, ::identity)

    override suspend fun toDomain(insightObject: InsightObject): Either<InsightClientError, ObjectWithAllDefaultTypes> =
        either {
            ObjectWithAllDefaultTypes(
                id = insightObject.id,
                name = insightObject.getAttributeValue<Text>(Name.attributeId)?.value!!,
                testBoolean = insightObject.getAttributeValue<InsightAttribute.Bool>(TestBoolean.attributeId)?.value,
                testInteger = insightObject.getAttributeValue<Integer>(TestInteger.attributeId)?.value,
                testFloat = insightObject.getAttributeValue<InsightAttribute.DoubleNumber>(TestFloat.attributeId)?.value,
                testDate = insightObject.getAttributeValue<InsightAttribute.Date>(TestDate.attributeId)?.value,
                testDateTime = insightObject.getAttributeValue<InsightAttribute.DateTime>(TestDateTime.attributeId)?.value,
                testUrl = insightObject.getAttributeValue<InsightAttribute.Url>(TestUrl.attributeId)?.values?.toSet() ?: emptySet(),
                testEmail = insightObject.getAttributeValue<InsightAttribute.Email>(TestEmail.attributeId)?.value,
                testTextArea = insightObject.getAttributeValue<InsightAttribute.Textarea>(TestTextArea.attributeId)?.value,
                testSelect = insightObject.getAttributeValue<InsightAttribute.Select>(TestSelect.attributeId)?.values
                    ?: emptyList(),
                testIpAddress = insightObject.getAttributeValue<InsightAttribute.Ipaddress>(TestIpAddress.attributeId)?.value,
            )
        }

    override suspend fun attributesFromDomain(domainObject: ObjectWithAllDefaultTypes): Either<InsightClientError, List<InsightAttribute>> =
        listOf(
            // skip ID
            Name.attributeId toValue domainObject.name,
            TestBoolean.attributeId toValue domainObject.testBoolean,
            TestInteger.attributeId toValue domainObject.testInteger,
            TestFloat.attributeId toValue domainObject.testFloat,
            TestDate.attributeId toValue domainObject.testDate,
            TestDateTime.attributeId toValue domainObject.testDateTime,
            TestUrl.attributeId toUrlValues domainObject.testUrl.toList(),
            TestEmail.attributeId toEmailValue domainObject.testEmail,
            TestTextArea.attributeId toTextareaValue domainObject.testTextArea,
            TestSelect.attributeId toSelectValues domainObject.testSelect,
            TestIpAddress.attributeId toIpaddressValue domainObject.testIpAddress,
        ).right()
}