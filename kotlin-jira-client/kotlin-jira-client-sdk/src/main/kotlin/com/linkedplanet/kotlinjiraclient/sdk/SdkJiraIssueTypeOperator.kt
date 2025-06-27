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
import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.bc.project.ProjectService
import com.atlassian.jira.config.IssueTypeService
import com.atlassian.jira.issue.fields.rest.RestAwareField
import com.atlassian.jira.issue.fields.rest.json.beans.JiraBaseUrls
import com.atlassian.jira.issue.fields.screen.FieldScreenLayoutItem
import com.atlassian.jira.issue.fields.screen.FieldScreenScheme
import com.atlassian.jira.issue.fields.screen.FieldScreenTab
import com.atlassian.jira.issue.fields.screen.issuetype.IssueTypeScreenSchemeManager
import com.atlassian.jira.issue.issuetype.IssueType
import com.atlassian.jira.issue.operation.IssueOperations
import com.atlassian.jira.issue.operation.ScreenableIssueOperation
import com.atlassian.jira.security.JiraAuthenticationContext
import com.linkedplanet.kotlinjiraclient.api.error.JiraClientError
import com.linkedplanet.kotlinjiraclient.api.interfaces.JiraIssueTypeOperator
import com.linkedplanet.kotlinjiraclient.api.model.JiraIssueType
import com.linkedplanet.kotlinjiraclient.api.model.JiraIssueTypeAttribute
import com.linkedplanet.kotlinjiraclient.api.model.JiraIssueTypeAttributeSchema
import com.linkedplanet.kotlinjiraclient.sdk.util.eitherAndCatch
import com.linkedplanet.kotlinjiraclient.sdk.util.getComponent
import com.linkedplanet.kotlinjiraclient.sdk.util.toEither
import org.springframework.stereotype.Component
import java.net.MalformedURLException
import java.net.URL

@Component
object SdkJiraIssueTypeOperator : JiraIssueTypeOperator {

    private val projectService: ProjectService by getComponent()
    private val issueService: IssueService by getComponent()
    private val issueTypeService: IssueTypeService by getComponent()
    private val issueTypeScreenSchemeManager: IssueTypeScreenSchemeManager by getComponent()
    private val jiraAuthenticationContext: JiraAuthenticationContext by getComponent()
    private val jiraBaseUrls: JiraBaseUrls by getComponent()

    private fun user() = jiraAuthenticationContext.loggedInUser

    override suspend fun getCreateAttributesOfIssueType(
        projectId: Number,
        issueTypeId: Number
    ): Either<JiraClientError, List<JiraIssueTypeAttribute>> =
        getSpecificAttributesOfIssueType(projectId, issueTypeId, IssueOperations.CREATE_ISSUE_OPERATION)

    @Suppress("MemberVisibilityCanBePrivate")
    fun getSpecificAttributesOfIssueType(
        projectId: Number,
        issueTypeId: Number,
        screenableIssueOperation: ScreenableIssueOperation
    ): Either<JiraClientError, List<JiraIssueTypeAttribute>> = eitherAndCatch {
        val issueType = issueTypeService.getIssueType(user(), issueTypeId.toString()).orNull
            ?: return@getSpecificAttributesOfIssueType issueTypeNotFound(issueTypeId)
        val project = projectService.getProjectById(user(), projectId.toLong()).toEither().bind().project
        issueTypeScreenSchemeManager
            .getIssueTypeScreenScheme(project)
            .getEffectiveFieldScreenScheme(issueType)
            .attributesForOperation(screenableIssueOperation)
    }

    override suspend fun getEditAttributes(
        issueId: Long
    ): Either<JiraClientError, List<JiraIssueTypeAttribute>> = eitherAndCatch {
        val issue = issueService.getIssue(user(), issueId).toEither().bind().issue
        issueTypeScreenSchemeManager
            .getFieldScreenScheme(issue)
            .attributesForOperation(IssueOperations.EDIT_ISSUE_OPERATION)
    }

    override suspend fun getEditAttributes(
        issueKey: String
    ): Either<JiraClientError, List<JiraIssueTypeAttribute>> = eitherAndCatch {
        val issue = issueService.getIssue(user(), issueKey).toEither().bind().issue
        issueTypeScreenSchemeManager
            .getFieldScreenScheme(issue)
            .attributesForOperation(IssueOperations.EDIT_ISSUE_OPERATION)
    }

    private fun FieldScreenScheme.attributesForOperation(
        issueOperationType: ScreenableIssueOperation
    ): List<JiraIssueTypeAttribute> {
        val createScreen = getFieldScreen(issueOperationType)
        val fields = createScreen.tabs.flatMap { screenTab: FieldScreenTab ->
            screenTab.fieldScreenLayoutItems.map { layoutItem: FieldScreenLayoutItem ->
                val orderableField = layoutItem.orderableField
                val schema = (orderableField as? RestAwareField)?.jsonSchema
                // code inspired by AbstractMetaFieldBeanBuilder.java
                JiraIssueTypeAttribute(
                    id = orderableField.id,
                    name = orderableField.name,
                    schema = JiraIssueTypeAttributeSchema(
                        schema?.type ?: "Any",
                        schema?.items,
                        schema?.system,
                        schema?.custom,
                        schema?.customId,
                    )
                )
            }
        }
        return fields
    }

    private fun <T> issueTypeNotFound(issueTypeId: Number): Either<JiraClientError, T> = Either.Left(
        JiraClientError("IssueType not found", "No IssueType with id:$issueTypeId found.", statusCode = 404)
    )

    override suspend fun getIssueType(issueTypeId: Number): Either<JiraClientError, JiraIssueType?> =
        eitherAndCatch {
            val issueType = issueTypeService.getIssueType(user(), issueTypeId.toString()).orNull
                ?: return@getIssueType issueTypeNotFound(issueTypeId)
            toJiraIssueType(issueType)
        }

    override suspend fun getIssueTypes(projectId: Number): Either<JiraClientError, List<JiraIssueType>> =
        eitherAndCatch {
            projectService.getProjectById(user(), projectId.toLong()).toEither().bind().project?.issueTypes
                ?.map(::toJiraIssueType)
                ?: emptyList()
        }

    private fun toJiraIssueType(issueType: IssueType): JiraIssueType =
        issueType.run {
            // code inspired by IssueTypeBeanBuilder
            val iconAbsoluteURL = try {
                URL(issueType.iconUrl).toString()
            } catch (_: MalformedURLException) {
                jiraBaseUrls.baseUrl() + issueType.iconUrl
            }
            val restApiBase = jiraBaseUrls.restApi2BaseUrl().removeSuffix("/")
            val self = "${restApiBase}/issuetype/${issueType.id}"
            JiraIssueType(id, name, self, descTranslation, isSubTask, iconAbsoluteURL, avatar?.id ?: 0L)
        }

}
