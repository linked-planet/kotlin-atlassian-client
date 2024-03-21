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
import com.atlassian.jira.bc.project.ProjectService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.config.IssueTypeService
import com.atlassian.jira.issue.fields.rest.RestAwareField
import com.atlassian.jira.issue.fields.screen.FieldScreenLayoutItem
import com.atlassian.jira.issue.fields.screen.FieldScreenTab
import com.atlassian.jira.issue.fields.screen.issuetype.IssueTypeScreenSchemeManager
import com.atlassian.jira.issue.issuetype.IssueType
import com.atlassian.jira.issue.operation.IssueOperations
import com.linkedplanet.kotlinjiraclient.api.error.JiraClientError
import com.linkedplanet.kotlinjiraclient.api.interfaces.JiraIssueTypeOperator
import com.linkedplanet.kotlinjiraclient.api.model.JiraIssueType
import com.linkedplanet.kotlinjiraclient.api.model.JiraIssueTypeAttribute
import com.linkedplanet.kotlinjiraclient.api.model.JiraIssueTypeAttributeSchema
import com.linkedplanet.kotlinjiraclient.sdk.util.eitherAndCatch
import com.linkedplanet.kotlinjiraclient.sdk.util.toEither
import javax.inject.Named

@Named
object SdkJiraIssueTypeOperator : JiraIssueTypeOperator {

    private val projectService = ComponentAccessor.getComponent(ProjectService::class.java)
    private val issueTypeService = ComponentAccessor.getComponent(IssueTypeService::class.java)
    private val issueTypeScreenSchemeManager = ComponentAccessor.getComponent(IssueTypeScreenSchemeManager::class.java)
    private val jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext()

    private fun user() = jiraAuthenticationContext.loggedInUser

    override suspend fun getAttributesOfIssueType(
        projectId: Number,
        issueTypeId: Number
    ): Either<JiraClientError, List<JiraIssueTypeAttribute>> =
        eitherAndCatch {
            val issueType = issueTypeService.getIssueType(user(), issueTypeId.toString()).orNull
                ?: return@getAttributesOfIssueType issueTypeNotFound(issueTypeId)
            val screenSchemes = issueTypeScreenSchemeManager.getIssueTypeScreenScheme(
                projectService.getProjectById(user(), projectId.toLong()).toEither().bind().project
            )
            val screenScheme = screenSchemes.getEffectiveFieldScreenScheme(issueType)
            val createScreen = screenScheme.getFieldScreen(IssueOperations.CREATE_ISSUE_OPERATION)
            val fields = createScreen.tabs.flatMap { screenTab: FieldScreenTab ->
                screenTab.fieldScreenLayoutItems.map { layoutItem: FieldScreenLayoutItem ->
                    val orderableField = layoutItem.orderableField
                    val schema = (orderableField as? RestAwareField)?.jsonSchema
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
            fields
        }

    private fun <T> issueTypeNotFound(issueTypeId: Number): Either<JiraClientError, T> = Either.Left(
        JiraClientError("IssueType not found", "No IssueType with id:$issueTypeId found.", statusCode = 404)
    )

    override suspend fun getIssueType(issueTypeId: Number): Either<JiraClientError, JiraIssueType?> =
        eitherAndCatch {
            val issueType = issueTypeService.getIssueType(user(), issueTypeId.toString()).orNull
                ?: return@getIssueType issueTypeNotFound(issueTypeId)
            issueType.let { it: IssueType ->
                JiraIssueType(it.id, it.name)
            }
        }

    override suspend fun getIssueTypes(projectId: Number): Either<JiraClientError, List<JiraIssueType>> =
        eitherAndCatch {
            projectService.getProjectById(user(), projectId.toLong()).toEither().bind().project?.issueTypes
                ?.map { it: IssueType ->
                    JiraIssueType(it.id, it.name)
                } ?: emptyList()
        }
}
