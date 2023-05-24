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

import com.linkedplanet.kotlinjiraclient.util.rightAssertedJiraClientError
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

interface JiraIssueLinkOperatorTest<JiraFieldType> : BaseTestConfigProvider<JiraFieldType> {

    @Test
    fun issueLinks_01CreateAndDeleteIssueLink() {
        println("### START issueLinks_01CreateAndDeleteIssueLink")
        val (inward, _) = jiraCommentTestHelper.createIssueWithComment("issueLinks_01CreateAndDeleteIssueLink Inward")
        val (outward, _) = jiraCommentTestHelper.createIssueWithComment("issueLinks_01CreateAndDeleteIssueLink Outward")

        // Create
        runBlocking {
            issueLinkOperator.createIssueLink(
                inward.key,
                outward.key,
                "Relates"
            )
        }.rightAssertedJiraClientError()

        // Check
        val issueLinks = jiraIssueLinkTestHelper.getIssueLinks(inward.key)
        assertThat(issueLinks.size(), equalTo(1))

        val issueLink = issueLinks.first().asJsonObject
        val issueLinkId = issueLink.get("id").asString
        val outwardIssue = issueLink.getAsJsonObject("outwardIssue")
        assertThat(outwardIssue.get("key").asString, equalTo(outward.key))

        // Delete
        runBlocking { issueLinkOperator.deleteIssueLink(issueLinkId) }.rightAssertedJiraClientError()

        // Check
        val issueLinksAfterDeletion = jiraIssueLinkTestHelper.getIssueLinks(inward.key)
        assertThat(issueLinksAfterDeletion.size(), equalTo(0))

        println("### END issueLinks_01CreateAndDeleteIssueLink")
    }

    @Test
    fun issueLinks_02EpicLink() {
        println("### START issueLinks_02EpicLink")

        // Create epic
        val epic = jiraIssueTestHelper.createEpic(
            fieldFactory.jiraEpicNameField("Test17_EpicName"), // required property for Epics
            fieldFactory.jiraSummaryField("issueLinks_07_TheEpicItself")
        )

        // Create issue with link to epic
        val issueWithEpicLink = jiraIssueTestHelper.createDefaultIssue(
            fieldFactory.jiraEpicLinkField(epic.key), // adds link to the epic
            fieldFactory.jiraSummaryField("issueLinks_07_IssueWithEpicLink")
        )

        val issue = jiraIssueTestHelper.getIssueByKey(issueWithEpicLink.key)
        assertThat(issue.epicKey, equalTo(epic.key))

        println("### END issueLinks_02EpicLink")
    }
}
