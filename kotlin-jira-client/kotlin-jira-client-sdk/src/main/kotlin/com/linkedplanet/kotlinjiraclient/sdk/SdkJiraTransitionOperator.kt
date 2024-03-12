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
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.transition.TransitionManager
import com.linkedplanet.kotlinjiraclient.api.error.JiraClientError
import com.linkedplanet.kotlinjiraclient.api.interfaces.JiraTransitionOperator
import com.linkedplanet.kotlinjiraclient.api.model.JiraTransition
import com.linkedplanet.kotlinjiraclient.sdk.util.eitherAndCatch
import com.linkedplanet.kotlinjiraclient.sdk.util.toEither

object SdkJiraTransitionOperator : JiraTransitionOperator {

    private val issueService = ComponentAccessor.getIssueService()
    private val workflowManager = ComponentAccessor.getWorkflowManager()
    private val transitionManager = ComponentAccessor.getComponent(TransitionManager::class.java)
    private val jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext()
    private fun user() = jiraAuthenticationContext.loggedInUser

    override suspend fun doTransition(
        issueKey: String,
        transitionId: String,
        comment: String?
    ): Either<JiraClientError, Boolean> = eitherAndCatch {
        val issue = issueService.getIssue(user(), issueKey).toEither().bind().issue
        val issueInputParameters = createParams(comment)
        val actionId = transitionId.toIntOrNull() ?: return createTransitionIdNoIntError(transitionId).left()

        val validateTransition = issueService.validateTransition(user(), issue.id, actionId, issueInputParameters)
        if (!validateTransition.isValid) return@eitherAndCatch false

        val executedTransition = issueService.transition(user(), validateTransition)
        executedTransition.isValid
    }

    override suspend fun getAvailableTransitions(issueKey: String): Either<JiraClientError, List<JiraTransition>> =
        eitherAndCatch {
            val issue = issueService.getIssue(user(), issueKey).toEither().bind().issue
            val workflow = workflowManager.getWorkflow(issue)
            val allPossibleTransitions = transitionManager.getTransitions(listOf(workflow))
            allPossibleTransitions
                .flatMap { it.transitions }
                .filter {
                    issueService.validateTransition(user(), issue.id, it.transitionId, createParams())
                        ?.isValid ?: false
                }
                .map { JiraTransition(it.transitionId.toString(), it.name) }
        }

    private fun createTransitionIdNoIntError(transitionId: String) =
        JiraClientError("Illegal Argument",
            "Transition with id $transitionId must be of type Int.",
            statusCode = 400
        )

    private fun createParams(comment: String? = null): IssueInputParameters? =
        issueService.newIssueInputParameters().apply {
            if (comment != null) setComment(comment)
        }
}
