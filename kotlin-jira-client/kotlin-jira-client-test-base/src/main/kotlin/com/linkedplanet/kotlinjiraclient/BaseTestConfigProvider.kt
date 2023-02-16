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
package com.linkedplanet.kotlinjiraclient

import com.linkedplanet.kotlinjiraclient.api.field.JiraFieldFactory
import com.linkedplanet.kotlinjiraclient.api.interfaces.*
import com.linkedplanet.kotlinjiraclient.util.*

interface BaseTestConfigProvider<JiraFieldType> {
    val fieldFactory: JiraFieldFactory<JiraFieldType>
    val issueOperator: JiraIssueOperator<JiraFieldType>

    val projectOperator: JiraProjectOperator
    val issueTypeOperator: JiraIssueTypeOperator
    val transitionOperator: JiraTransitionOperator
    val commentOperator: JiraCommentOperator
    val issueLinkOperator: JiraIssueLinkOperator
    val userOperator: JiraUserOperator

    val issueTypeId: Int
    val epicIssueTypeId: Int
    val projectId: Long
    val projectKey: String

    val jiraIssueTestHelper: JiraIssueTestHelper<JiraFieldType>
        get() = JiraIssueTestHelper(issueOperator, fieldFactory, issueTypeId, epicIssueTypeId, projectId)

    val jiraCommentTestHelper: JiraCommentTestHelper<JiraFieldType>
        get() = JiraCommentTestHelper(commentOperator, fieldFactory, jiraIssueTestHelper)

    val jiraTransitionTestHelper: JiraTransitionTestHelper
        get() = JiraTransitionTestHelper(transitionOperator)

    val jiraIssueLinkTestHelper: JiraIssueLinkTestHelper<JiraFieldType>
        get() = JiraIssueLinkTestHelper(issueOperator)
}
