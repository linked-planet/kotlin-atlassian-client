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
import com.google.gson.reflect.TypeToken
import com.linkedplanet.kotlinjiraclient.api.error.JiraClientError
import com.linkedplanet.kotlinjiraclient.api.interfaces.JiraTransitionOperator
import com.linkedplanet.kotlinjiraclient.api.model.JiraTransition
import com.linkedplanet.kotlinjiraclient.http.model.HttpJiraTransitions
import com.linkedplanet.kotlinjiraclient.http.model.toJiraTransitions
import com.linkedplanet.kotlinjiraclient.http.util.fromHttpDomainError

class HttpJiraTransitionOperator(private val context: HttpJiraClientContext) : JiraTransitionOperator {

    override suspend fun getAvailableTransitions(issueKey: String): Either<JiraClientError, List<JiraTransition>> =
        either {
            context.httpClient.executeRest<HttpJiraTransitions>(
                "GET",
                "/rest/api/2/issue/$issueKey/transitions",
                emptyMap(),
                null,
                "application/json",
                object : TypeToken<HttpJiraTransitions>() {}.type
            ).map { it.body?.transitions ?: emptyList() }
                .mapLeft { JiraClientError.fromHttpDomainError(it) }
                .bind()
                .toJiraTransitions()
        }

    override suspend fun doTransition(
        issueKey: String,
        transitionId: String,
        comment: String?
    ): Either<JiraClientError, Boolean> {
        val commentJson =
            if (comment != null) {
                """
                    "update": {
                        "comment": [
                            {
                                "add": {
                                    "body": "$comment"
                                }
                            }
                        ]
                    },
                """.trimIndent()
            } else {
                ""
            }


        val json = """
                {
                    $commentJson
                    "transition": {
                        "id": "$transitionId"
                    }
                }
        """.trimIndent()

        return context.httpClient.executeRestCall(
            "POST",
            "/rest/api/2/issue/$issueKey/transitions",
            emptyMap(),
            json,
            "application/json"
        ).map { true }
            .mapLeft { JiraClientError.fromHttpDomainError(it) }
    }
}
