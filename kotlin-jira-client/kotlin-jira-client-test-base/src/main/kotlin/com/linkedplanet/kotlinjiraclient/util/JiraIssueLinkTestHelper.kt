/*-
 * #%L
 * kotlin-jira-client-test-base
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
package com.linkedplanet.kotlinjiraclient.util

import arrow.core.Either
import com.google.gson.JsonArray
import com.linkedplanet.kotlinjiraclient.api.interfaces.JiraIssueOperator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull

class JiraIssueLinkTestHelper<JiraFieldType>(
    private val issueOperator: JiraIssueOperator<JiraFieldType>
) {

    fun getIssueLinks(issueKey: String): JsonArray {
        val jsonResponse = runBlocking {
            issueOperator.getIssueByKey(issueKey) { json, mappings ->
                Either.Right(json)
            }.orNull()
        }
        assertNotNull(jsonResponse)

        val fields = jsonResponse!!.getAsJsonObject("fields")
        return fields.getAsJsonArray("issuelinks")!!
    }
}
