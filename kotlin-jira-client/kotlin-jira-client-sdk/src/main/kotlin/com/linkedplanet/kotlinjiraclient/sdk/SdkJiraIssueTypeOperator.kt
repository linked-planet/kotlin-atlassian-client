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
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.config.IssueTypeManager
import com.atlassian.jira.issue.fields.screen.issuetype.IssueTypeScreenSchemeManager
import com.atlassian.jira.issue.issuetype.IssueType
import com.atlassian.jira.issue.operation.IssueOperations
import com.linkedplanet.kotlinjiraclient.api.error.JiraClientError
import com.linkedplanet.kotlinjiraclient.api.interfaces.JiraIssueTypeOperator
import com.linkedplanet.kotlinjiraclient.api.model.JiraIssueType
import com.linkedplanet.kotlinjiraclient.api.model.JiraIssueTypeAttribute
import com.linkedplanet.kotlinjiraclient.sdk.util.catchJiraClientError
import javax.inject.Named

@Named
object SdkJiraIssueTypeOperator : JiraIssueTypeOperator {

    private val projectManager by lazy { ComponentAccessor.getProjectManager() }
    private val issueTypeManager by lazy { ComponentAccessor.getComponent(IssueTypeManager::class.java) }
    private val issueTypeScreenSchemeManager by lazy { ComponentAccessor.getComponent(IssueTypeScreenSchemeManager::class.java) }

    override suspend fun getAttributesOfIssueType(
        projectId: Number,
        issueTypeId: Number
    ): Either<JiraClientError, List<JiraIssueTypeAttribute>> =
        Either.catchJiraClientError {
            val issueType = issueTypeManager.getIssueType(issueTypeId.toString())
            val screenSchemes = issueTypeScreenSchemeManager.getIssueTypeScreenScheme(
                projectManager.getProjectObj(projectId.toLong())
            )
            val screenScheme = screenSchemes.getEffectiveFieldScreenScheme(issueType)
            val createScreen = screenScheme.getFieldScreen(IssueOperations.CREATE_ISSUE_OPERATION)
            val fields = createScreen.tabs.flatMap { screenTab ->
                screenTab.fieldScreenLayoutItems.map { layoutItem ->
                    JiraIssueTypeAttribute(layoutItem.orderableField.id, layoutItem.orderableField.name)
                }
            }
            fields
        }

    override suspend fun getIssueType(issueTypeId: Number): Either<JiraClientError, JiraIssueType?> =
        Either.catchJiraClientError {
            issueTypeManager.getIssueType(issueTypeId.toString())?.let { it: IssueType ->
                JiraIssueType(it.id, it.name)
            }
        }

    override suspend fun getIssueTypes(projectId: Number): Either<JiraClientError, List<JiraIssueType>> =
        Either.catchJiraClientError {
            projectManager.getProjectObj(projectId.toLong())?.issueTypes
                ?.map { it: IssueType ->
                    JiraIssueType(it.id, it.name)
                } ?: emptyList()
        }
}
