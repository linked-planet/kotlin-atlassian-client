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
import com.atlassian.jira.bc.issue.link.IssueLinkService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.link.Direction
import com.atlassian.jira.issue.link.IssueLinkTypeManager
import com.linkedplanet.kotlinjiraclient.api.error.JiraClientError
import com.linkedplanet.kotlinjiraclient.api.interfaces.JiraIssueLinkOperator
import com.linkedplanet.kotlinjiraclient.sdk.util.eitherAndCatch
import com.linkedplanet.kotlinjiraclient.sdk.util.toEither

object SdkJiraIssueLinkOperator : JiraIssueLinkOperator {

    private val issueService = ComponentAccessor.getIssueService()
    private val issueLinkService = ComponentAccessor.getComponent(IssueLinkService::class.java)
    private val issueLinkTypeManager = ComponentAccessor.getComponent(IssueLinkTypeManager::class.java)
    private val jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext()

    private fun user() = jiraAuthenticationContext.loggedInUser
    private const val DISPATCH_EVENT: Boolean = true // default dispatch behaviour for this operator

    override suspend fun createIssueLink(
        inwardIssueKey: String,
        outwardIssueKey: String,
        relationName: String
    ): Either<JiraClientError, Unit> =
        eitherAndCatch {
            val user = user()
            val inwardSourceIssue = issueService.getIssue(user, inwardIssueKey).toEither().bind().issue
            val outwardIssue = issueService.getIssue(user, outwardIssueKey).toEither().bind().issue
            val linkType = issueLinkTypeManager.getIssueLinkTypesByName(relationName).firstOrNull()
                ?: return issueLinkTypeNotFound(relationName)
            val validate = issueLinkService.validateAddIssueLinks(
                user,
                outwardIssue,
                linkType.id,
                Direction.OUT,
                listOf(inwardSourceIssue.key),
                DISPATCH_EVENT
            ).toEither().bind()
            issueLinkService.addIssueLinks(user, validate)
        }

    private fun issueLinkTypeNotFound(relationName: String): Either<JiraClientError, Unit> = Either.Left(
        JiraClientError("IssueLinkType not found", "No IssueLinkType named $relationName found.", statusCode = 404)
    )

    override suspend fun deleteIssueLink(linkId: String): Either<JiraClientError, Unit> =
        eitherAndCatch {
            val user = user()
            val issueLink = issueLinkService.getIssueLink(linkId.toLong(), user).toEither().bind().issueLink
            val issue = issueService.getIssue(user, issueLink.destinationId).toEither().bind().issue
            val validate = issueLinkService.validateDelete(user, issue, issueLink).toEither().bind()
            issueLinkService.delete(validate)
        }
}
