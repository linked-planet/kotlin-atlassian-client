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

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import com.atlassian.jira.bc.ServiceResult
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.util.ErrorCollection
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
import com.linkedplanet.kotlinjiraclient.sdk.field.SdkJiraField
import com.linkedplanet.kotlinjiraclient.sdk.util.IssueJsonConverter
import com.linkedplanet.kotlinjiraclient.sdk.util.catchJiraClientError
import org.jetbrains.kotlin.util.removeSuffixIfPresent
import javax.inject.Named
import kotlin.math.ceil

@Named
object SdkJiraIssueOperator : JiraIssueOperator<SdkJiraField> {
    override var RESULTS_PER_PAGE: Int = 10

    private val issueService by lazy { ComponentAccessor.getIssueService() }
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
        val contextPath = webResourceUrlProvider.baseUrl
        val fullPath = if (contextPath.isNotEmpty()) "$basePath/$contextPath" else basePath
        val selfLink = fullPath + "/rest/api/2/issue/" + createdIssue.id
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

    private fun <T : ServiceResult> T.toEither(errorTitle: String? = null): Either<JiraClientError, T> =
        when {
            this.isValid -> Either.Right(this)
            else -> Either.Left(jiraClientError(this.errorCollection, errorTitle
                        ?: "${this::class.simpleName?.removeSuffixIfPresent("ServiceResult")}Error"))
        }

    private fun ErrorCollection.toEither(errorTitle: String = "SdkError") : Either<JiraClientError, Unit> =
        when {
            this.hasAnyErrors() -> jiraClientError(this, errorTitle).left()
            else -> Unit.right()
        }

    private fun jiraClientError(errorCollection: ErrorCollection, errorTitle: String = "SdkError"): JiraClientError {
        val worstReason: Reason? = Reason.getWorstReason(errorCollection.reasons)
        val httpStatusSuffix = worstReason?.let { " (${it.httpStatusCode})" } ?: ""
        return JiraClientError(
            errorTitle,
            errorCollection.errorMessages.joinToString(",\n")
                    + errorCollection.errors.map { "'$it.key':${it.value}" }.joinToString(",\n")
                    + httpStatusSuffix,
            statusCode = worstReason?.httpStatusCode
        )
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
    ): Either<JiraClientError, T?> = either {
        Either.catchJiraClientError {
            val issueResult = issueService.getIssue(user(), key)
            if (Reason.getWorstReason(issueResult.errorCollection.reasons) == Reason.NOT_FOUND){
                return@catchJiraClientError null
            }
            val issue = issueResult.toEither().bind().issue
                ?: return@catchJiraClientError null
            issueToConcreteType(issue, parser).bind()
        }.bind()
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
        val user = userOrError().bind()
        val query = jqlParser.parseQuery(jql)
        val search = searchService.search(user, query, pagerFilter)
        val issues = search.results
            .map { issue -> issueToConcreteType(issue, parser) }
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
