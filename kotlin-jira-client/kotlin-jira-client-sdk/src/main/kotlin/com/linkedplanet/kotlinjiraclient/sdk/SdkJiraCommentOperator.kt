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
import com.atlassian.jira.bc.JiraServiceContextImpl
import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.bc.issue.comment.CommentService
import com.atlassian.jira.bc.issue.comment.CommentService.CommentParameters
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.util.SimpleErrorCollection
import com.linkedplanet.kotlinjiraclient.api.error.JiraClientError
import com.linkedplanet.kotlinjiraclient.api.interfaces.JiraCommentOperator
import com.linkedplanet.kotlinjiraclient.api.model.JiraIssueComment
import com.linkedplanet.kotlinjiraclient.sdk.util.*

object SdkJiraCommentOperator : JiraCommentOperator {

    private val issueService: IssueService = ComponentAccessor.getIssueService()
    private val commentService : CommentService = ComponentAccessor.getComponent(CommentService::class.java)
    private val jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext()
    private fun user() = jiraAuthenticationContext.loggedInUser
    private val dispatchEvent: Boolean = true // default dispatch behaviour for this operator

    override suspend fun getComments(issueKey: String): Either<JiraClientError, List<JiraIssueComment>> =
        eitherAndCatch {
            val issue = issueService.getIssue(user(), issueKey).toEither().bind().issue
            val comments = commentService.getCommentsForUser(user(), issue)
            comments.map { JiraIssueComment(it.id.toString(), it.body, it.authorFullName, it.updated.toString()) }
    }

    override suspend fun createComment(issueKey: String, content: String): Either<JiraClientError, Unit> =
        eitherAndCatch {
            val issue = issueService.getIssue(user(), issueKey).toEither().bind().issue
            val commentParameters = newCommentParameters(issue, content)
            val validateComment = commentService.validateCommentCreate(user(), commentParameters).toEither().bind()
            commentService.create(user(), validateComment, dispatchEvent)
            validateComment.toEither().bind()
        }

    override suspend fun updateComment(
        issueKey: String,
        commentId: String,
        content: String
    ): Either<JiraClientError, Unit> = eitherAndCatch {
        val issue = issueService.getIssue(user(), issueKey).toEither().bind().issue
        val commentParameters = newCommentParameters(issue, content)
        val valid = commentService.validateCommentUpdate(user(), commentId.toLong(), commentParameters).toEither().bind()
        commentService.update(user(), valid, dispatchEvent)
    }

    override suspend fun deleteComment(issueKey: String, id: String): Either<JiraClientError, Unit> =
        eitherAndCatch {
            val simpleErrorCollection = SimpleErrorCollection()
            val comment = commentService.getCommentById(user(), id.toLongOrNull()!!, simpleErrorCollection)
            simpleErrorCollection.toEither().bind()
            val jiraServiceContextImpl = JiraServiceContextImpl(user())
            commentService.delete(jiraServiceContextImpl, comment, dispatchEvent)
            jiraServiceContextImpl.errorCollection.toEither().bind()
        }

    private fun newCommentParameters(
        issue: MutableIssue,
        content: String
    ): CommentParameters = CommentParameters.CommentParametersBuilder()
        .issue(issue)
        .body(content)
        .author(user())
        .build()

}
