/*-
 * #%L
 * kotlin-jira-client-api
 * %%
 * Copyright (C) 2022 - 2024 linked-planet GmbH
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

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.config.properties.ApplicationProperties
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.security.JiraAuthenticationContext
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.util.ErrorCollection.Reason
import com.atlassian.jira.util.ErrorCollections
import com.atlassian.jira.web.bean.I18nBean
import com.atlassian.jira.web.bean.PagerFilter
import com.google.gson.JsonObject
import com.linkedplanet.kotlinatlassianclientcore.common.api.Page
import com.linkedplanet.kotlinatlassianclientcore.common.error.asEither
import com.linkedplanet.kotlinjiraclient.api.error.JiraClientError
import com.linkedplanet.kotlinjiraclient.api.interfaces.JiraIssueOperator
import com.linkedplanet.kotlinjiraclient.api.model.JiraIssue
import com.linkedplanet.kotlinjiraclient.api.model.IssueQueryParams
import com.linkedplanet.kotlinjiraclient.sdk.field.SdkJiraField
import com.linkedplanet.kotlinjiraclient.sdk.util.IssueJsonConverter
import com.linkedplanet.kotlinjiraclient.sdk.util.catchJiraClientError
import com.linkedplanet.kotlinjiraclient.sdk.util.getComponent
import com.linkedplanet.kotlinjiraclient.sdk.util.jiraClientError
import com.linkedplanet.kotlinjiraclient.sdk.util.toEither
import javax.inject.Named
import kotlin.math.ceil

@Named
object SdkJiraIssueOperator : JiraIssueOperator<SdkJiraField> {
    override var RESULTS_PER_PAGE: Int = 10

    private val issueService: IssueService by getComponent()
    private val customFieldManager: CustomFieldManager by getComponent()
    private val searchService: SearchService by getComponent()
    private val jiraAuthenticationContext: JiraAuthenticationContext by getComponent()
    private val jqlParser: JqlQueryParser by getComponent()
    private val applicationProperties: ApplicationProperties by getComponent()
    private val issueJsonConverter = IssueJsonConverter()

    private fun user() = jiraAuthenticationContext.loggedInUser

    override suspend fun createIssue(
        projectId: Long,
        issueTypeId: Int,
        fields: List<SdkJiraField>
    ): Either<JiraClientError, JiraIssue?> = either {
        Either.catchJiraClientError {
            val inputParameters = issueInputParameters(projectId, issueTypeId, fields)
            val validateCreate = issueService.validateCreate(user(), inputParameters).toEither().bind()
            val createResult = issueService.create(user(), validateCreate).toEither().bind()
            toBasicReturnTypeIssue(createResult.issue)
        }.bind()
    }

    private fun toBasicReturnTypeIssue(createdIssue: MutableIssue): JiraIssue {
        val basePath = applicationProperties.jiraBaseUrl
        val selfLink = basePath + "/rest/api/2/issue/" + createdIssue.id
        return JiraIssue(createdIssue.id.toString(), createdIssue.key, selfLink)
    }

    override suspend fun updateIssue(
        projectId: Long,
        issueTypeId: Int,
        issueKey: String,
        fields: List<SdkJiraField>
    ): Either<JiraClientError, Unit> = either {
        Either.catchJiraClientError {
            val issueId = issueService.getIssue(user(), issueKey).toEither().bind().issue.id
            val inputParameters = issueInputParameters(projectId, issueTypeId, fields)
            val validateUpdate = issueService.validateUpdate(user(), issueId, inputParameters)
            val validationResult = validateUpdate.toEither().bind()
            issueService.update(user(), validationResult, EventDispatchOption.ISSUE_UPDATED, false).toEither().bind()
        }.bind()
    }

    private fun issueInputParameters(
        projectId: Long,
        issueTypeId: Int,
        fields: List<SdkJiraField>
    ): IssueInputParameters? {
        val issueInput = issueService.newIssueInputParameters()
        issueInput.setSkipScreenCheck(true)
        issueInput.setSkipLicenceCheck(true)
        issueInput.setApplyDefaultValuesWhenParameterNotProvided(true)
        issueInput.setRetainExistingValuesWhenParameterNotProvided(true)

        issueInput.projectId = projectId
        issueInput.issueTypeId = issueTypeId.toString()
        fields.forEach { field ->
            field.render(issueInput)
        }
        return issueInput
    }

    override suspend fun deleteIssue(issueKey: String): Either<JiraClientError, Unit> = either {
        Either.catchJiraClientError {
            val issueToDelete = issueService.getIssue(user(), issueKey).toEither().bind()
            val validateDelete = issueService.validateDelete(user(), issueToDelete.issue.id).toEither().bind()
            issueService.delete(user(), validateDelete, EventDispatchOption.ISSUE_DELETED, false).toEither().bind()
        }.bind()
    }

    override suspend fun <T> getIssueById(
        id: Int,
        queryParams: IssueQueryParams,
        parser: suspend (JsonObject, Map<String, String>) -> Either<JiraClientError, T>
    ): Either<JiraClientError, T?> =
        getIssueByKey(id.toString(), queryParams, parser)

    override suspend fun <T> getIssueByJQL(
        jql: String,
        queryParams: IssueQueryParams,
        parser: suspend (JsonObject, Map<String, String>) -> Either<JiraClientError, T>
    ): Either<JiraClientError, T?> = either {
        val potentiallyMultipleIssues = getIssuesByJQLPaginated(jql, 0, 1, queryParams, parser).bind()
        if (potentiallyMultipleIssues.totalItems < 1) {
            JiraClientError("Issue not found", "No issue was found.").asEither<JiraClientError, T?>().bind()
        }
        potentiallyMultipleIssues.items.first()
    }

    override suspend fun <T> getIssuesByIssueType(
        projectId: Long,
        issueTypeId: Int,
        queryParams: IssueQueryParams,
        parser: suspend (JsonObject, Map<String, String>) -> Either<JiraClientError, T>
    ): Either<JiraClientError, List<T>> {
        val jql = "project=$projectId AND issueType=$issueTypeId"
        return getIssuesByJQL(jql, queryParams, parser)
    }

    override suspend fun <T> getIssuesByTypePaginated(
        projectId: Long,
        issueTypeId: Int,
        pageIndex: Int,
        pageSize: Int,
        queryParams: IssueQueryParams,
        parser: suspend (JsonObject, Map<String, String>) -> Either<JiraClientError, T>
    ): Either<JiraClientError, Page<T>> {
        val jql = "project=$projectId AND issueType=$issueTypeId"
        return getIssuesByJQLPaginated(jql, pageIndex, pageSize, queryParams, parser)
    }

    override suspend fun <T> getIssueByKey(
        key: String,
        queryParams: IssueQueryParams,
        parser: suspend (JsonObject, Map<String, String>) -> Either<JiraClientError, T>
    ): Either<JiraClientError, T?> = either {
        Either.catchJiraClientError {
            val issueResult = issueService.getIssue(user(), key)
            if (Reason.getWorstReason(issueResult.errorCollection.reasons) == Reason.NOT_FOUND){
                return@catchJiraClientError null
            }
            val issue = issueResult.toEither().bind().issue
                ?: return@catchJiraClientError null
            issueToConcreteType(issue, queryParams, parser).bind()
        }.bind()
    }

    override suspend fun <T> getIssuesByJQL(
        jql: String,
        queryParams: IssueQueryParams,
        parser: suspend (JsonObject, Map<String, String>) -> Either<JiraClientError, T>
    ): Either<JiraClientError, List<T>> = either {
        val issuePage = getIssuesByJqlWithPagerFilter(jql, PagerFilter.getUnlimitedFilter(), queryParams, parser).bind()
        issuePage.items
    }

    override suspend fun <T> getIssuesByJQLPaginated(
        jql: String,
        pageIndex: Int,
        pageSize: Int,
        queryParams: IssueQueryParams,
        parser: suspend (JsonObject, Map<String, String>) -> Either<JiraClientError, T>
    ): Either<JiraClientError, Page<T>> {
        val pagerFilter = PagerFilter.newPageAlignedFilter(pageIndex * pageSize, pageSize)
        return getIssuesByJqlWithPagerFilter(jql, pagerFilter, queryParams, parser)
    }

    private suspend fun <T> issueToConcreteType(
        issue: Issue,
        queryParams: IssueQueryParams,
        parser: suspend (JsonObject, Map<String, String>) -> Either<JiraClientError, T>
    ): Either<JiraClientError, T> = Either.catchJiraClientError {
        val jsonIssue: JsonObject = issueJsonConverter.createJsonIssue(issue, queryParams)
        val customFieldMap = customFieldManager.getCustomFieldObjects(issue).associate { it.name to it.id }
        return parser(jsonIssue, customFieldMap)
    }

    private suspend fun <T> getIssuesByJqlWithPagerFilter(
        jql: String,
        pagerFilter: PagerFilter<*>?,
        queryParams: IssueQueryParams,
        parser: suspend (JsonObject, Map<String, String>) -> Either<JiraClientError, T>
    ): Either<JiraClientError, Page<T>> = either {
        val user = userOrError().bind()
        val query = Either.catchJiraClientError { jqlParser.parseQuery(jql) }.bind()
        val search = Either.catchJiraClientError { searchService.search(user, query, pagerFilter) }.bind()
        val issues = search.results
            .map { issue -> issueToConcreteType(issue, queryParams, parser) }
            .bindAll()
        val totalItems = search.total
        val pageSize = pagerFilter?.pageSize ?: 0
        val totalPages = ceil(totalItems.toDouble() / pageSize.toDouble()).toInt()
        val currentPageIndex = pagerFilter?.start?.let { start -> start / pageSize } ?: 0
        Page(issues, totalItems, totalPages, currentPageIndex, pageSize)
    }

    private fun userOrError() : Either<JiraClientError, ApplicationUser> = either {
        val applicationUser = user()
        return applicationUser?.right()
            ?: jiraClientError(
                ErrorCollections
                    .create(
                        I18nBean(I18nBean.getLocaleFromUser(applicationUser))
                            .getText("admin.errors.issues.no.permission.to.see"),
                        Reason.NOT_LOGGED_IN
                    )
            ).left()
    }

}
