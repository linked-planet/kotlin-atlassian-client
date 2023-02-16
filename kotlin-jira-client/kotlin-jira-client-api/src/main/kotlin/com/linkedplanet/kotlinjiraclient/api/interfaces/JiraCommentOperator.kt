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
package com.linkedplanet.kotlinjiraclient.api.interfaces

import arrow.core.Either
import com.linkedplanet.kotlinjiraclient.api.error.JiraClientError
import com.linkedplanet.kotlinjiraclient.api.model.JiraIssueComment

/**
 * Manages Jira comments.
 */
interface JiraCommentOperator {

    /**
     * Returns a list of all comments on the issue identified by the specified key.
     * @param issueKey The key of the issue.
     * @return A list of all comments or an error.
     */
    suspend fun getComments(issueKey: String): Either<JiraClientError, List<JiraIssueComment>>

    /**
     * Creates a comment on the specified issue.
     * @param issueKey The key used to identify the issue.
     * @param content The content of the comment.
     * @return Either an error or success.
     */
    suspend fun createComment(
        issueKey: String,
        content: String
    ): Either<JiraClientError, Unit>

    /**
     * Updates an existing comment on the specified issue.
     * @param issueKey The key used to identify the issue.
     * @param commentId The ID of the comment to update.
     * @param content The updated content of the comment.
     * @return Either an error or success.
     */
    suspend fun updateComment(
        issueKey: String,
        commentId: String,
        content: String
    ): Either<JiraClientError, Unit>

    /**
     * Deletes an existing comment on the specified issue.
     * @param issueKey The key used to identify the issue.
     * @param id The ID of the comment to delete.
     * @return Either an error or success.
     */
    suspend fun deleteComment(
        issueKey: String,
        id: String
    ): Either<JiraClientError, Unit>
}
