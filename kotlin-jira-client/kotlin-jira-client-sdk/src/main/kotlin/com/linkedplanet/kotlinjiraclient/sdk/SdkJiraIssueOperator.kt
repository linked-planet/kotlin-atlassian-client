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
package com.linkedplanet.kotlinjiraclient.sdk

import arrow.core.*
import arrow.core.raise.either
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.web.bean.PagerFilter
import com.google.gson.JsonObject
import com.linkedplanet.kotlinjiraclient.api.error.JiraClientError
import com.linkedplanet.kotlinjiraclient.api.interfaces.JiraIssueOperator
import com.linkedplanet.kotlinjiraclient.api.model.JiraIssue
import com.linkedplanet.kotlinatlassianclientcore.common.api.Page
import com.linkedplanet.kotlinatlassianclientcore.common.error.asEither
import com.linkedplanet.kotlinjiraclient.sdk.field.SdkJiraField
import com.linkedplanet.kotlinjiraclient.sdk.util.IssueJsonConverter
import com.linkedplanet.kotlinjiraclient.sdk.util.catchJiraClientError
import javax.inject.Named
import kotlin.math.ceil

@Named
object SdkJiraIssueOperator : JiraIssueOperator<SdkJiraField> {
    override var RESULTS_PER_PAGE: Int = 10

    private val issueManager by lazy { ComponentAccessor.getIssueManager() }
    private val issueFactory by lazy { ComponentAccessor.getIssueFactory() }
    private val customFieldManager by lazy { ComponentAccessor.getCustomFieldManager() }
    private val searchService: SearchService by lazy { ComponentAccessor.getComponent(SearchService::class.java) }
    private val jiraAuthenticationContext by lazy { ComponentAccessor.getJiraAuthenticationContext() }
    private val jqlParser by lazy { ComponentAccessor.getComponent(JqlQueryParser::class.java) }
    private val applicationProperties by lazy { ComponentAccessor.getApplicationProperties() }
    private val webResourceUrlProvider by lazy { ComponentAccessor.getWebResourceUrlProvider() }
    private val issueJsonConverter = IssueJsonConverter()

    private fun user() = jiraAuthenticationContext.loggedInUser

    override suspend fun createIssue(
        projectId: Long,
        issueTypeId: Int,
        fields: List<SdkJiraField>
    ): Either<JiraClientError, JiraIssue?> = Either.catchJiraClientError {
        val freshIssue: MutableIssue = issueFactory.issue
        freshIssue.projectId = projectId
        freshIssue.issueTypeId = issueTypeId.toString()
        fields.forEach { field ->
            field.render(freshIssue)
        }
        val createdIssue: Issue = issueManager.createIssueObject(user(), freshIssue)
        val basePath = applicationProperties.jiraBaseUrl
        val contextPath = webResourceUrlProvider.baseUrl
        val fullPath = if (contextPath.isNotEmpty()) "$basePath/$contextPath" else basePath
        val selfLink = fullPath + "/rest/api/2/issue/" + createdIssue.id
        JiraIssue(createdIssue.id.toString(), createdIssue.key, selfLink)
    }

    override suspend fun updateIssue(
        projectId: Long,
        issueTypeId: Int,
        issueKey: String,
        fields: List<SdkJiraField>
    ): Either<JiraClientError, Unit> = Either.catchJiraClientError {
        val issue = issueManager.getIssueByCurrentKey(issueKey)
        issue.projectId = projectId
        issue.issueTypeId = issueTypeId.toString()
        fields.forEach { field ->
            field.render(issue)
        }
        issueManager.updateIssue(user(), issue, EventDispatchOption.ISSUE_UPDATED, false)
    }

    override suspend fun deleteIssue(issueKey: String): Either<JiraClientError, Unit> =
        Either.catchJiraClientError {
            val issue: Issue = issueManager.getIssueByCurrentKey(issueKey)
            val sendMail = false
            issueManager.deleteIssue(user(), issue, EventDispatchOption.ISSUE_DELETED, sendMail)
        }

    override suspend fun <T> getIssueById(
        id: Int,
        parser: suspend (JsonObject, Map<String, String>) -> Either<JiraClientError, T>
    ): Either<JiraClientError, T?> =
        getIssueByKey(id.toString(), parser)

    override suspend fun <T> getIssueByJQL(
        jql: String,
        parser: suspend (JsonObject, Map<String, String>) -> Either<JiraClientError, T>
    ): Either<JiraClientError, T?> = either {
        val potentiallyMultipleIssues = getIssuesByJQLPaginated(jql, 0, 1, parser).bind()
        if (potentiallyMultipleIssues.totalItems < 1) {
            JiraClientError("Issue not found", "No issue was found.").asEither<JiraClientError, T?>().bind()
        }
        potentiallyMultipleIssues.items.first()
    }

    override suspend fun <T> getIssuesByIssueType(
        projectId: Long,
        issueTypeId: Int,
        parser: suspend (JsonObject, Map<String, String>) -> Either<JiraClientError, T>
    ): Either<JiraClientError, List<T>> =
        getIssuesByJQL("project=$projectId AND issueType=$issueTypeId", parser)

    override suspend fun <T> getIssuesByTypePaginated(
        projectId: Long,
        issueTypeId: Int,
        pageIndex: Int,
        pageSize: Int,
        parser: suspend (JsonObject, Map<String, String>) -> Either<JiraClientError, T>
    ): Either<JiraClientError, Page<T>> =
        getIssuesByJQLPaginated("project=$projectId AND issueType=$issueTypeId", pageIndex, pageSize, parser)

    override suspend fun <T> getIssueByKey(
        key: String,
        parser: suspend (JsonObject, Map<String, String>) -> Either<JiraClientError, T>
    ): Either<JiraClientError, T?> = Either.catchJiraClientError {
        val issue = issueManager.getIssueByCurrentKey(key)
            ?: return@catchJiraClientError null

        return issueToConcreteType(issue, parser)
    }

    override suspend fun <T> getIssuesByJQL(
        jql: String,
        parser: suspend (JsonObject, Map<String, String>) -> Either<JiraClientError, T>
    ): Either<JiraClientError, List<T>> = either {
        val issuePage = getIssuesByJqlWithPagerFilter(jql, PagerFilter.getUnlimitedFilter(), parser).bind()
        issuePage.items
    }

    override suspend fun <T> getIssuesByJQLPaginated(
        jql: String,
        pageIndex: Int,
        pageSize: Int,
        parser: suspend (JsonObject, Map<String, String>) -> Either<JiraClientError, T>
    ): Either<JiraClientError, Page<T>> =
        getIssuesByJqlWithPagerFilter(jql, PagerFilter.newPageAlignedFilter(pageIndex * pageSize, pageSize), parser)

    private suspend fun <T> issueToConcreteType(
        issue: Issue,
        parser: suspend (JsonObject, Map<String, String>) -> Either<JiraClientError, T>
    ): Either<JiraClientError, T> {
        val jsonIssue: JsonObject = issueJsonConverter.createJsonIssue(issue)
        val customFieldMap = customFieldManager.getCustomFieldObjects(issue).associate { it.name to it.id }
        return parser(jsonIssue, customFieldMap)
    }

    private suspend fun <T> getIssuesByJqlWithPagerFilter(
        jql: String,
        pagerFilter: PagerFilter<*>?,
        parser: suspend (JsonObject, Map<String, String>) -> Either<JiraClientError, T>
    ): Either<JiraClientError, Page<T>> = either {
        val query = jqlParser.parseQuery(jql)
        val search = searchService.search(user(), query, pagerFilter)
        val issues = search.results
            .map { issue -> issueToConcreteType(issue, parser) }
            .sequenceEither()
            .bind()
        val totalItems = search.total
        val pageSize = pagerFilter?.pageSize ?: 0
        val totalPages = ceil(totalItems.toDouble() / pageSize.toDouble()).toInt()
        val currentPageIndex = pagerFilter?.start?.let { start -> start / pageSize } ?: 0
        Page(issues, totalItems, totalPages, currentPageIndex, pageSize)
    }
}
