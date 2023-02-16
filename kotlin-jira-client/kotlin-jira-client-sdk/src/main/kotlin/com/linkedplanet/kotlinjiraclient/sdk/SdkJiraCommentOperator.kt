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
import com.linkedplanet.kotlinjiraclient.api.error.JiraClientError
import com.linkedplanet.kotlinjiraclient.api.interfaces.JiraCommentOperator
import com.linkedplanet.kotlinjiraclient.api.model.JiraIssueComment
import com.linkedplanet.kotlinjiraclient.sdk.util.catchJiraClientError

object SdkJiraCommentOperator : JiraCommentOperator {

    private val issueManager by lazy { ComponentAccessor.getIssueManager() }
    private val commentManager by lazy { ComponentAccessor.getCommentManager() }
    private val jiraAuthenticationContext by lazy { ComponentAccessor.getJiraAuthenticationContext() }
    private fun user() = jiraAuthenticationContext.loggedInUser
    private fun dispatchEvent(): Boolean = true // default dispatch behaviour for this operator

    override suspend fun getComments(issueKey: String): Either<JiraClientError, List<JiraIssueComment>> =
        Either.catchJiraClientError {
            val issue = issueManager.getIssueByCurrentKey(issueKey)
            val comments = commentManager.getComments(issue)
            comments.map { JiraIssueComment(it.id.toString(), it.body, it.authorFullName, it.updated.toString()) }
        }

    override suspend fun createComment(issueKey: String, content: String): Either<JiraClientError, Unit> =
        Either.catchJiraClientError {
            val issue = issueManager.getIssueByCurrentKey(issueKey)
            commentManager.create(issue, user(), content, dispatchEvent())
        }

    override suspend fun updateComment(
        issueKey: String,
        commentId: String,
        content: String
    ): Either<JiraClientError, Unit> =
        Either.catchJiraClientError {
            val comment = commentManager.getMutableComment(commentId.toLongOrNull()!!)
            comment.body = content
            comment.setUpdateAuthor(user())
            commentManager.update(comment, dispatchEvent())
        }

    override suspend fun deleteComment(issueKey: String, id: String): Either<JiraClientError, Unit> =
        Either.catchJiraClientError {
            val comment = commentManager.getCommentById(id.toLongOrNull()!!)
            commentManager.delete(comment, dispatchEvent(), user())
        }
}
