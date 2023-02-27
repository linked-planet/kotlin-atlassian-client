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

import com.linkedplanet.kotlininsightclient.api.interfaces.InsightSchemaOperator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

interface InsightSchemaOperatorTest {
    val insightSchemaOperator: InsightSchemaOperator

    @Test
    fun testSchemaLoad() {
        println("### START schema_testSchemaLoad")

        val schemas = runBlocking {
            insightSchemaOperator.getSchemas()
        }
        assertTrue(schemas.isNotEmpty())

        println("### END schema_testSchemaLoad")
    }
}
