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

import com.linkedplanet.kotlinjiraclient.api.model.JiraIssueTypeAttribute
import com.linkedplanet.kotlinjiraclient.util.rightAssertedJiraClientError
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

interface JiraIssueTypeOperatorTest<JiraFieldType> : BaseTestConfigProvider<JiraFieldType> {

    @Test
    fun issueTypes_01GetIssueTypes() {
        println("### START issueTypes_01GetIssueTypes")

        val issueTypeNames = listOf("Bug", "Epic", "Story", "Sub-task", "Task")
        val issueTypes = runBlocking { issueTypeOperator.getIssueTypes(projectId) }.rightAssertedJiraClientError()
        assertThat(issueTypes.map { it.name }.toSet(), equalTo(issueTypeNames.toSet()))

        println("### END issueTypes_01GetIssueTypes")
    }

    @Test
    fun issueTypes_02GetIssueType() {
        println("### START issueTypes_02GetIssueType")

        val issueType = runBlocking { issueTypeOperator.getIssueType(issueTypeId) }.rightAssertedJiraClientError()
        assertThat(issueType.id, equalTo(issueTypeId.toString()))
        assertThat(issueType.name, equalTo("Story"))

        println("### START issueTypes_02GetIssueType")
    }

    @Test
    fun issueTypes_03GetAttributesOfIssueType() {
        println("### START issueTypes_03GetAttributesOfIssueType")

        val attributes =
            runBlocking {
                issueTypeOperator.getAttributesOfIssueType(
                    projectId,
                    issueTypeId
                )
            }.rightAssertedJiraClientError()
        val expectedAttributes = listOf(
            "Epic Link", "Summary", "Issue Type", "Reporter", "Component/s", "Description",
            "Fix Version/s", "Priority", "Labels", "Attachment", "Linked Issues", "Assignee",
            "Sprint", "InsightObject"
        )   // Newer Jira does not include "Project

        val attributeNames = attributes.map(JiraIssueTypeAttribute::name)
        assertThat(attributeNames.size, equalTo(attributes.size))
        expectedAttributes.forEach {
            assertThat("Attributes does not contain: $it", attributeNames.contains(it), equalTo(true))
        }

        println("### START issueTypes_03GetAttributesOfIssueType")
    }
}
