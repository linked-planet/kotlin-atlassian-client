/*-
 * #%L
 * kotlin-jira-client-api
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
package com.linkedplanet.kotlinjiraclient

import arrow.core.*
import com.linkedplanet.kotlinjiraclient.api.interfaces.*
import com.linkedplanet.kotlinjiraclient.api.model.*
import org.junit.AssumptionViolatedException
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
abstract class JiraClientTest<JiraFieldType> :
    JiraCommentOperatorTest<JiraFieldType>,
    JiraIssueLinkOperatorTest<JiraFieldType>,
    JiraIssueOperatorTest<JiraFieldType>,
    JiraIssueTypeOperatorTest<JiraFieldType>,
    JiraProjectOperatorTest<JiraFieldType>,
    JiraTransitionOperatorTest<JiraFieldType>,
    JiraUserOperatorTest<JiraFieldType> {

    @get:Rule
    val watchman: TestRule = object : TestWatcher() {
        override fun starting(desciption: Description) {
            println("## Starting test: ${desciption.methodName}")
        }

        override fun failed(e: Throwable?, description: Description) {
            println("### Failed test: ${description.methodName} message:${e?.message}")
        }

        override fun skipped(e: AssumptionViolatedException?, description: Description) {
            println("## Skipped test: ${description.methodName} message:${e?.message}")
        }

        override fun succeeded(description: Description) {
            println("## Succeeded test: ${description.methodName}")
        }
    }
}
