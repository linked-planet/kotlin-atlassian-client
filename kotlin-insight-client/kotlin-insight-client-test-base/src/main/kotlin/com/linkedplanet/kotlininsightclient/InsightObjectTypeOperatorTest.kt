/*-
 * #%L
 * kotlin-insight-client-test-base
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

import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectTypeOperator
import com.linkedplanet.kotlininsightclient.api.model.ObjectTypeSchemaAttribute
import com.linkedplanet.kotlininsightclient.api.model.ReferenceKind
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

interface InsightObjectTypeOperatorTest {
    val insightObjectTypeOperator: InsightObjectTypeOperator

    @Test
    fun testGetObjectType() {
        val objectType = runBlocking {
            insightObjectTypeOperator.getObjectType(InsightObjectType.Company.id).orFail()
        }
        assertThat(objectType, notNullValue())
        assertThat(objectType.name, equalTo("Company"))
        assertThat(objectType.parentObjectTypeId, equalTo(null))
        assertThat(objectType.attributes.size, equalTo(5))

        val nameAttribute = objectType.attributes.singleOrNull { it.name == "Name" }  as? ObjectTypeSchemaAttribute.TextSchema
        assertThat(nameAttribute,  notNullValue())

        val createdAttribute = objectType.attributes.singleOrNull { it.name == "Created" } as? ObjectTypeSchemaAttribute.DateTimeSchema
        assertThat(createdAttribute,  notNullValue())

        val countryAttribute = objectType.attributes.singleOrNull { it.name == "Country" } as? ObjectTypeSchemaAttribute.ReferenceSchema
        assertThat(countryAttribute, notNullValue())
        assertThat(countryAttribute?.referenceKind, equalTo(ReferenceKind.REFERENCE))
        assertThat(countryAttribute?.referenceObjectTypeId, equalTo(InsightObjectType.Country.id))
        assertThat(countryAttribute?.minimumCardinality, equalTo(0))
        assertThat(countryAttribute?.maximumCardinality, equalTo(1))
    }
}
