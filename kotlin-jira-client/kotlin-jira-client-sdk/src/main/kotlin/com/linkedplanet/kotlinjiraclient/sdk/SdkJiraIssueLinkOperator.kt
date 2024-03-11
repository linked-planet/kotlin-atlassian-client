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
import com.atlassian.jira.issue.link.IssueLinkTypeManager
import com.linkedplanet.kotlinjiraclient.api.error.JiraClientError
import com.linkedplanet.kotlinjiraclient.api.interfaces.JiraIssueLinkOperator
import com.linkedplanet.kotlinjiraclient.sdk.util.catchJiraClientError

object SdkJiraIssueLinkOperator : JiraIssueLinkOperator {

    private val issueManager by lazy { ComponentAccessor.getIssueManager() }
    private val issueLinkManager by lazy { ComponentAccessor.getIssueLinkManager() }
    private val issueLinkTypeManager by lazy { ComponentAccessor.getComponent(IssueLinkTypeManager::class.java) }
    private val jiraAuthenticationContext by lazy { ComponentAccessor.getJiraAuthenticationContext() }

    private fun loggedInUser() = jiraAuthenticationContext.loggedInUser

    override suspend fun createIssueLink(
        inwardIssueKey: String,
        outwardIssueKey: String,
        relationName: String
    ): Either<JiraClientError, Unit> =
        Either.catchJiraClientError {
            val inwardSourceIssue = issueManager.getIssueByCurrentKey(inwardIssueKey)
            val outwardIssue = issueManager.getIssueByCurrentKey(outwardIssueKey)
            val linkType = issueLinkTypeManager.getIssueLinkTypesByName(relationName).firstOrNull()
                ?: return issueLinkTypeNotFound(relationName)
            val sequence: Long? = null // For UI ordering. Sequence on links does not matter

            issueLinkManager.createIssueLink(
                inwardSourceIssue.id,
                outwardIssue.id,
                linkType.id,
                sequence,
                loggedInUser()
            )
        }

    private fun issueLinkTypeNotFound(relationName: String): Either<JiraClientError, Unit> = Either.Left(
        JiraClientError("IssueLinkType not found", "No IssueLinkType named $relationName found.", statusCode = 404)
    )

    override suspend fun deleteIssueLink(linkId: String): Either<JiraClientError, Unit> =
        Either.catchJiraClientError {
            val issueLink = issueLinkManager.getIssueLink(linkId.toLong())
            issueLinkManager.removeIssueLink(issueLink, loggedInUser())
        }
}
