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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

interface InsightObjectTypeOperatorTest {
    val insightObjectTypeOperator: InsightObjectTypeOperator

    @Test
    fun testGetObjectType() {
        println("### START testGetObjectType")

        val objectType = runBlocking {
            insightObjectTypeOperator.getObjectType(1).orNull()
        }
        assertNotNull(objectType)
        assertTrue(objectType!!.name == "Company")
        assertNull(objectType.parentObjectTypeId)

        assertEquals(5, objectType.attributes.size)

        val nameAttribute = objectType.attributes.singleOrNull {it.name == "Name"}
        assertNotNull(nameAttribute)
        assertNull(nameAttribute?.referenceType)
        assertEquals(nameAttribute?.defaultType?.name, "Text")

        val createdAttribute = objectType.attributes.singleOrNull {it.name == "Created"}
        assertNotNull(createdAttribute)
        assertNull(createdAttribute?.referenceType)
        assertEquals(createdAttribute?.defaultType?.name, "DateTime")

        val countryAttribute = objectType.attributes.singleOrNull {it.name == "Country"}
        assertNotNull(countryAttribute)
        assertNull(countryAttribute?.defaultType)
        assertEquals(countryAttribute?.referenceType?.name, "Reference")
        assertEquals(countryAttribute?.minimumCardinality, 0)
        assertEquals(countryAttribute?.maximumCardinality, 1)

        println("### END testGetObjectType")
    }
}
