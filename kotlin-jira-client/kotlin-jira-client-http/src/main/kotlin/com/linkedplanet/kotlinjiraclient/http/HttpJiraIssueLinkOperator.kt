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
import com.linkedplanet.kotlinjiraclient.api.error.JiraClientError
import com.linkedplanet.kotlinjiraclient.api.interfaces.JiraIssueLinkOperator
import com.linkedplanet.kotlinjiraclient.http.util.fromHttpDomainError

class HttpJiraIssueLinkOperator(private val context: HttpJiraClientContext) : JiraIssueLinkOperator {

    override suspend fun createIssueLink(
        inwardIssueKey: String,
        outwardIssueKey: String,
        relationName: String
    ): Either<JiraClientError, Unit> = either {
        val jsonBody = JsonObject()
        jsonBody.add("type", JsonObject().apply { addProperty("name", relationName) })
        jsonBody.add("inwardIssue", JsonObject().apply { addProperty("key", inwardIssueKey) })
        jsonBody.add("outwardIssue", JsonObject().apply { addProperty("key", outwardIssueKey) })

        context.httpClient.executeRestCall(
            "POST",
            "/rest/api/2/issueLink",
            emptyMap(),
            jsonBody.toString(),
            "application/json"
        )
            .mapLeft { JiraClientError.fromHttpDomainError(it) }
            .bind()
    }

    override suspend fun deleteIssueLink(linkId: String): Either<JiraClientError, Unit> = either {
        context.httpClient.executeRestCall(
            "DELETE",
            "/rest/api/2/issueLink/$linkId",
            emptyMap(),
            null,
            "application/json"
        )
            .mapLeft { JiraClientError.fromHttpDomainError(it) }
            .bind()
    }
}
