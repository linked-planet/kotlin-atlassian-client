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

import com.linkedplanet.kotlininsightclient.api.interfaces.InsightHistoryOperator
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectOperator
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.endsWith
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Test

interface InsightHistoryOperatorTest {
    val insightObjectOperator: InsightObjectOperator
    val insightHistoryOperator: InsightHistoryOperator

    @Test
    fun testHistory() {
        println("### START history_testHistory")

        runBlocking {
            val country = insightObjectOperator.getObjectByName(InsightObject.Country.id, "Germany").orNull()!!
            val history = insightHistoryOperator.getHistory(country.id).orNull()!!
            assertTrue(history.historyItems.isNotEmpty())
            assertThat(history.historyItems.last().actor.key, equalTo("admin"))
            assertThat(history.historyItems.last().created, endsWith("Z"))
        }

        println("### END history_testHistory")
    }
}
