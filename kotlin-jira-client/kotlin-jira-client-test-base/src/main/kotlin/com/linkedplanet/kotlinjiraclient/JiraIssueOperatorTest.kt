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

import arrow.core.*
import com.linkedplanet.kotlinjiraclient.api.model.Page
import com.linkedplanet.kotlinjiraclient.util.*
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

interface JiraIssueOperatorTest<JiraFieldType> : BaseTestConfigProvider<JiraFieldType> {

    @Test
    fun issues_01DeleteAllIssuesAndCreateTenTestIssues() {
        println("### START issues_01DeleteAllIssuesAndCreateTenTestIssues")

        val success = runBlocking {
            val existingIssueIds = issueOperator.getIssuesByJQL("") { jsonObject, _ ->
                Either.Right(jsonObject.getAsJsonPrimitive("key").asString)
            }

            existingIssueIds.rightAssertedJiraClientError().forEach {
                issueOperator.deleteIssue(it)
            }

            val createResult = (1..10).map { searchedKeyIndex ->
                val fields = listOf<JiraFieldType>(
                    fieldFactory.jiraProjectField(projectId),
                    fieldFactory.jiraIssueTypeField(issueTypeId),
                    fieldFactory.jiraSummaryField("Test-$searchedKeyIndex"),
                    fieldFactory.jiraCustomInsightObjectField("InsightObject", "IT-1")
                )
                issueOperator.createIssue(
                    projectId,
                    issueTypeId,
                    fields
                )
            }.sequenceEither()

            createResult.isRight()
        }
        assertTrue(success)

        println("### END issues_01DeleteAllIssuesAndCreateTenTestIssues")
    }

    @Test
    fun issues_02GetIssuesByIssueType() {
        println("### START issues_02GetIssuesByIssueType")
        val issues: List<Story> = runBlocking {
            issueOperator.getIssuesByIssueType(projectId, issueTypeId, parser = ::issueParser).orNull() ?: emptyList()
        }

        val keys = 1..10
        keys.forEach { searchedKeyIndex ->
            val issue = issues.firstOrNull { "Test-$searchedKeyIndex" == it.summary }
            assertNotNull(issue)
            assertThat(issue!!.insightObjectKey, equalTo("IT-1"))
            assertThat(issue.status.name, equalTo("To Do"))
        }
        println("### END issues_02GetIssuesByIssueType")
    }

    @Test
    fun issues_03GetIssuesByJQL() {
        println("### START issues_03GetIssuesByJQL")
        val issues: List<Story> = runBlocking {
            issueOperator.getIssuesByJQL("summary ~ \"Test-*\"", parser = ::issueParser).orNull() ?: emptyList()
        }
        assertThat(issues.size, equalTo(10))
        val keys = 1..10
        keys.forEach { searchedKeyIndex ->
            val issue = issues.first { "Test-$searchedKeyIndex" == it.summary }
            assertThat(issue.insightObjectKey, equalTo("IT-1"))
            assertThat(issue.status.name, equalTo("To Do"))
        }
        println("### END issues_03GetIssuesByJQL")
    }

    @Test
    fun issues_04GetIssuesByJQLPaginated() {
        println("### START issues_04GetIssuesByJQLPaginated")
        // 10 items with page size 1 -> 10 pages
        val pageNumbers = 1..10
        val pages = pageNumbers.map { pageNumber ->
            val page: Page<Story>? = runBlocking {
                issueOperator.getIssuesByJQLPaginated(
                    "summary ~ \"Test-*\"",
                    pageNumber - 1,
                    1,
                    parser = ::issueParser
                ).orNull()
            }
            assertNotNull(page)
            assertThat(page!!.totalItems, equalTo(10))
            assertThat(page.totalPages, equalTo(10))
            assertThat(page.currentPageIndex, equalTo(pageNumber - 1))
            assertThat(page.pageSize, equalTo(1))
            page
        }
        assertThat(pages.size, equalTo(10))

        pageNumbers.forEach { issueKey ->
            val issue = pages.flatMap { it.items }.singleOrNull { it.summary == "Test-$issueKey" }
            assertNotNull(issue)
            assertThat(issue!!.insightObjectKey, equalTo("IT-1"))
            assertThat(issue.status.name, equalTo("To Do"))
        }
        println("### END issues_04GetIssuesByJQLPaginated")
    }

    @Test
    fun issues_05GetIssuesByJQLPaginated() {
        println("### START issues_05GetIssuesByJQLPaginated")
        // 10 items with page size 2 -> 5 pages
        val pageNumbers = 1..5
        val pages = pageNumbers.map { pageNumber ->
            val page = runBlocking {
                issueOperator.getIssuesByJQLPaginated(
                    "summary ~ \"Test-*\"",
                    pageNumber - 1,
                    2,
                    parser = ::issueParser
                ).orNull()
            }
            assertNotNull(page)
            assertThat(page!!.totalItems, equalTo(10))
            assertThat(page.totalPages, equalTo(5))
            assertThat(page.currentPageIndex, equalTo(pageNumber - 1))
            assertThat(page.pageSize, equalTo(2))
            page
        }
        assertThat(pages.size, equalTo(5))

        val issueKeys = (1..10)
        issueKeys.forEach { issueKey ->
            val issue = pages.flatMap { it.items }.singleOrNull { it.summary == "Test-$issueKey" }
            assertNotNull(issue)
            assertThat(issue!!.insightObjectKey, equalTo("IT-1"))
            assertThat(issue.status.name, equalTo("To Do"))
        }
        println("### END issues_05GetIssuesByJQLPaginated")
    }

    @Test
    fun issues_06GetIssuesByIssueTypePaginated() {
        println("### START issues_06GetIssuesByIssueTypePaginated")
        // 10 items with page size 3 -> 4 pages
        val pageNumbers = 1..4
        val pages = pageNumbers.map { pageNumber ->
            runBlocking {
                val page = issueOperator.getIssuesByTypePaginated(
                    projectId,
                    issueTypeId,
                    pageNumber - 1,
                    3,
                    parser = ::issueParser
                ).orNull()
                assertNotNull(page)
                assertThat(page!!.totalItems, equalTo(10))
                assertThat(page.totalPages, equalTo(4))
                assertThat(page.currentPageIndex, equalTo(pageNumber - 1))
                assertThat(page.pageSize, equalTo(3))
                page
            }
        }
        assertThat(pages.size, equalTo(4))

        val issueKeys = (1..10)
        issueKeys.forEach { issueKey ->
            val issue = pages.flatMap { it.items }.singleOrNull { it.summary == "Test-$issueKey" }
            assertNotNull(issue)
            assertThat(issue!!.insightObjectKey, equalTo("IT-1"))
            assertThat(issue.status.name, equalTo("To Do"))
        }
        println("### END issues_06GetIssuesByIssueTypePaginated")
    }

    @Test
    fun issues_07CreateIssue() {
        println("### START issues_07CreateIssue")

        val summary = "MyNewSummary"
        val description = "MyDescription"
        val assignee = "test1"
        val reporter = "test2"
        val radioValue = "value1"
        val zonedDateTimeValue = ZonedDateTime.now(ZoneOffset.ofHours(1))
        val singleText = "text1"
        val multiText = "text2"
        val doubleNumber = 12.2
        val intNumber = 13
        val insightObjectKey = "IT-1"
        val insightObjectsKeys = listOf("IT-1", "IT-2")

        val creationResponse = runBlocking {
            val fields = listOf(
                fieldFactory.jiraSummaryField(summary),
                fieldFactory.jiraDescriptionField(description),
                fieldFactory.jiraProjectField(projectId),
                fieldFactory.jiraIssueTypeField(issueTypeId),
                fieldFactory.jiraAssigneeField(assignee),
                fieldFactory.jiraReporterField(reporter),
                fieldFactory.jiraCustomRadioField("Radio", radioValue),
                fieldFactory.jiraCustomDateTimeField("ZonedDateTime", zonedDateTimeValue),
                fieldFactory.jiraCustomTextField("SingleText", singleText),
                fieldFactory.jiraCustomTextField("MultiText", multiText),
                fieldFactory.jiraCustomNumberField("DoubleNumber", doubleNumber),
                fieldFactory.jiraCustomNumberField("IntNumber", intNumber),
                fieldFactory.jiraCustomInsightObjectField("InsightObject", insightObjectKey),
                fieldFactory.jiraCustomInsightObjectsField("InsightObjects", insightObjectsKeys)
            )
            issueOperator.createIssue(
                projectId,
                issueTypeId,
                fields
            )
        }.rightAssertedJiraClientError()
        assertTrue(creationResponse.self.endsWith("/rest/api/2/issue/${creationResponse.id}"))

        val createdIssue = runBlocking {
            issueOperator.getIssueByJQL("key = \"${creationResponse.key}\"", ::issueParser)
        }.rightAssertedJiraClientError()

        assertThat(createdIssue.projectId, equalTo(projectId))
        assertThat(createdIssue.issueTypeId, equalTo(issueTypeId))
        assertThat(createdIssue.summary, equalTo(summary))
        assertThat(createdIssue.description, equalTo(description))
        assertThat(createdIssue.assignee, equalTo(assignee))
        assertThat(createdIssue.reporter, equalTo(reporter))
        assertThat(createdIssue.radio, equalTo(radioValue))
        assertThat(createdIssue.singleText, equalTo(singleText))
        assertThat(createdIssue.multiText, equalTo(multiText))
        assertThat(createdIssue.doubleNumber, equalTo(doubleNumber))
        assertThat(createdIssue.intNumber, equalTo(intNumber))
        assertThat(createdIssue.insightObjectKey, equalTo(insightObjectKey))
        assertThat(createdIssue.insightObjectsKeys, equalTo(insightObjectsKeys))
        assertThat(createdIssue.status.statusCategory, equalTo("new"))
        assertThat(createdIssue.epicKey, equalTo(null))

        // Jira default datetime format has no seconds, millis, etc.
        // Jira instance is using timezone Berlin
        assertThat(
            createdIssue.zonedDateTime?.truncatedTo(ChronoUnit.MINUTES)?.withZoneSameInstant(ZoneOffset.UTC),
            equalTo(zonedDateTimeValue.truncatedTo(ChronoUnit.MINUTES).withZoneSameInstant(ZoneOffset.UTC))
        )

        val transitions = createdIssue.transitions
        assertTrue(transitions.isNotEmpty())
        assertTrue(transitions.singleOrNull { it.name == "Do it" }?.let { true } ?: false)
        assertTrue(transitions.singleOrNull { it.name == "To Do" }?.let { true } ?: false)

        println("### END issues_07CreateIssue")
    }

    @Test
    fun issues_08UpdateIssue() {
        println("### START issues_08UpdateIssue")

        val issue = runBlocking {
            issueOperator.getIssueByJQL("summary ~ \"MyNewSummary\"", ::issueParser)
        }.rightAssertedJiraClientError()

        val summary = "MyNewSummary-update"
        val description = "MyDescription-update"
        val assignee = "test2"
        val reporter = "test1"
        val radioValue = "value2"
        val zonedDateTimeValue = ZonedDateTime.now()
        val singleText = "text1-update"
        val multiText = "text2-update"
        val doubleNumber = 12.3
        val intNumber = 14
        val insightObjectKey = "IT-2"
        val insightObjectsKeys = listOf("IT-1")

        runBlocking {
            issueOperator.updateIssue(
                projectId,
                issueTypeId,
                issue.key,
                listOf(
                    fieldFactory.jiraSummaryField(summary),
                    fieldFactory.jiraDescriptionField(description),
                    fieldFactory.jiraProjectField(projectId),
                    fieldFactory.jiraIssueTypeField(issueTypeId),
                    fieldFactory.jiraAssigneeField(assignee),
                    fieldFactory.jiraReporterField(reporter),
                    fieldFactory.jiraCustomRadioField("Radio", radioValue),
                    fieldFactory.jiraCustomDateTimeField("ZonedDateTime", zonedDateTimeValue),
                    fieldFactory.jiraCustomTextField("SingleText", singleText),
                    fieldFactory.jiraCustomTextField("MultiText", multiText),
                    fieldFactory.jiraCustomNumberField("DoubleNumber", doubleNumber),
                    fieldFactory.jiraCustomNumberField("IntNumber", intNumber),
                    fieldFactory.jiraCustomInsightObjectField("InsightObject", insightObjectKey),
                    fieldFactory.jiraCustomInsightObjectsField("InsightObjects", insightObjectsKeys)
                )
            )
        }.rightAssertedJiraClientError()

        val issueAfterUpdate = runBlocking {
            issueOperator.getIssueByKey(issue.key, ::issueParser)
        }.rightAssertedJiraClientError()

        assertThat(issueAfterUpdate.projectId, equalTo(projectId))
        assertThat(issueAfterUpdate.issueTypeId, equalTo(issueTypeId))
        assertThat(issueAfterUpdate.summary, equalTo(summary))
        assertThat(issueAfterUpdate.description, equalTo(description))
        assertThat(issueAfterUpdate.assignee, equalTo(assignee))
        assertThat(issueAfterUpdate.reporter, equalTo(reporter))
        assertThat(issueAfterUpdate.radio, equalTo(radioValue))
        assertThat(issueAfterUpdate.singleText, equalTo(singleText))
        assertThat(issueAfterUpdate.multiText, equalTo(multiText))
        assertThat(issueAfterUpdate.doubleNumber, equalTo(doubleNumber))
        assertThat(issueAfterUpdate.intNumber, equalTo(intNumber))
        assertThat(issueAfterUpdate.insightObjectKey, equalTo(insightObjectKey))
        assertThat(issueAfterUpdate.insightObjectsKeys, equalTo(insightObjectsKeys))
        assertThat(issueAfterUpdate.status.statusCategory, equalTo("new"))
        assertThat(issueAfterUpdate.epicKey, equalTo(null))

        println("### END issues_08UpdateIssue")
    }

    @Test
    fun issues_09DeleteIssue() {
        println("### START issues_09DeleteIssue")

        val searchNewIssue = runBlocking {
            issueOperator.getIssueByJQL("summary ~ \"MyNewSummary-update\"", ::issueParser)
        }.rightAssertedJiraClientError()

        runBlocking {
            issueOperator.deleteIssue(searchNewIssue.key)
        }.rightAssertedJiraClientError()

        val issuesAfterDeletion = runBlocking {
            issueOperator.getIssueByKey(searchNewIssue.key, ::issueParser).orNull()
        }
        assertThat(issuesAfterDeletion, equalTo(null))

        println("### END issues_09DeleteIssue")
    }

    @Test
    fun issues_10GetNonExistingIssue() {
        println("### START issues_10GetNonExistingIssue")

        val response = runBlocking {
            issueOperator.getIssueByKey("BLAAAA") { _, _ -> Either.Right(null) }
        }
        assertTrue(response.isRight())
        assertThat(response.getOrElse { -1 }, equalTo(null))

        println("### END issues_10GetNonExistingIssue")
    }

    @Test
    fun issues_11GetIssuesByIQLError() {
        println("### START issues_11GetIssuesByIQLError")

        val response = runBlocking {
            issueOperator.getIssueByJQL("key = BLAAAA") { _, _ -> Either.Right(null) }
        }
        assertTrue(response.isLeft())

        println("### END issues_11GetIssuesByIQLError")
    }

    @Test
    fun issues_12GetIssuesByJQLEmpty() {
        println("### START issues_12GetIssuesByJQLEmpty")
        val issues: List<Story>? = runBlocking {
            issueOperator.getIssuesByJQL("summary ~ \"Emptyyyyy-*\"", parser = ::issueParser).orNull()
        }

        assertNotNull(issues)
        assertTrue(issues!!.isEmpty())

        println("### END issues_12GetIssuesByJQLEmpty")
    }

    @Test
    fun issues_13GetIssuesByJQLPaginatedEmpty() {
        println("### START issues_13GetIssuesByJQLPaginatedEmpty")
        val page = runBlocking {
            issueOperator.getIssuesByJQLPaginated("summary ~ \"Emptyyyyy-*\"", parser = ::issueParser).orNull()
        }

        assertNotNull(page)
        assertThat(page!!.totalItems, equalTo(0))
        assertThat(page.totalPages, equalTo(0))
        assertThat(page.currentPageIndex, equalTo(0))
        assertTrue(page.items.isEmpty())

        println("### END issues_13GetIssuesByJQLPaginatedEmpty")
    }
}
