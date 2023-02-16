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
package com.linkedplanet.kotlinjiraclient.http

import arrow.core.Either
import arrow.core.computations.either
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.linkedplanet.kotlinhttpclient.api.http.recursiveRestCallPaginated
import com.linkedplanet.kotlinjiraclient.api.error.JiraClientError
import com.linkedplanet.kotlinjiraclient.api.interfaces.JiraCommentOperator
import com.linkedplanet.kotlinjiraclient.api.model.JiraIssueComment
import com.linkedplanet.kotlinjiraclient.http.model.*
import com.linkedplanet.kotlinjiraclient.http.util.fromHttpDomainError

class HttpJiraCommentOperator(private val context: HttpJiraClientContext) : JiraCommentOperator {

    override suspend fun getComments(issueKey: String): Either<JiraClientError, List<JiraIssueComment>> = either {
        recursiveRestCallPaginated { index, pageSize ->
            context.httpClient.executeRest<HttpCommentPage>(
                "GET",
                "/rest/api/2/issue/$issueKey/comment?startAt=$index&maxResults=$pageSize",
                emptyMap(),
                null,
                "application/json",
                object : TypeToken<HttpCommentPage>() {}.type
            ).map { it.body!! }
        }
            .mapLeft { JiraClientError.fromHttpDomainError(it) }
            .bind()
            .toJiraIssueComments()
    }

    override suspend fun createComment(issueKey: String, content: String): Either<JiraClientError, Unit> =
        either {
            val jsonBody = JsonObject()
            jsonBody.addProperty("body", content)

            context.httpClient.executeRest<HttpJiraIssueComment>(
                "POST",
                "/rest/api/2/issue/$issueKey/comment",
                emptyMap(),
                jsonBody.toString(),
                "application/json",
                object : TypeToken<HttpJiraIssueComment>() {}.type
            ).map { it.body!! }
                .mapLeft { JiraClientError.fromHttpDomainError(it) }
                .bind()
                .toJiraIssueComment()
        }

    override suspend fun updateComment(
        issueKey: String,
        commentId: String,
        content: String
    ): Either<JiraClientError, Unit> = either {
        val jsonBody = JsonObject()
        jsonBody.addProperty("body", content)

        context.httpClient.executeRest<HttpJiraIssueComment>(
            "PUT",
            "/rest/api/2/issue/$issueKey/comment/$commentId",
            emptyMap(),
            jsonBody.toString(),
            "application/json",
            object : TypeToken<HttpJiraIssueComment>() {}.type
        ).map { it.body!! }
            .mapLeft { JiraClientError.fromHttpDomainError(it) }
            .bind()
            .toJiraIssueComment()
    }

    override suspend fun deleteComment(issueKey: String, id: String): Either<JiraClientError, Unit> =
        either {
            context.httpClient.executeRestCall(
                "DELETE",
                "/rest/api/2/issue/$issueKey/comment/$id",
                emptyMap(),
                null,
                "application/json"
            )
                .mapLeft { JiraClientError.fromHttpDomainError(it) }
                .bind()
        }
}
