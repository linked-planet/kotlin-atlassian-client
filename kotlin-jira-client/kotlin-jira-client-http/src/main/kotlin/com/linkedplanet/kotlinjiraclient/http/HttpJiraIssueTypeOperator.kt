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
import com.linkedplanet.kotlinhttpclient.api.http.DefaultHttpPage
import com.linkedplanet.kotlinhttpclient.api.http.recursiveRestCallPaginated
import com.linkedplanet.kotlinjiraclient.api.error.JiraClientError
import com.linkedplanet.kotlinjiraclient.api.interfaces.JiraIssueTypeOperator
import com.linkedplanet.kotlinjiraclient.api.model.JiraIssueType
import com.linkedplanet.kotlinjiraclient.api.model.JiraIssueTypeAttribute
import com.linkedplanet.kotlinjiraclient.http.model.*
import com.linkedplanet.kotlinjiraclient.http.util.fromHttpDomainError

class HttpJiraIssueTypeOperator(private val context: HttpJiraClientContext) : JiraIssueTypeOperator {

    override suspend fun getIssueTypes(projectId: Number): Either<JiraClientError, List<JiraIssueType>> = either {
        recursiveRestCallPaginated { index, pageSize ->
            context.httpClient.executeRest<DefaultHttpPage<HttpJiraIssueType>>(
                "GET",
                "/rest/api/2/issue/createmeta/$projectId/issuetypes?startAt=$index&maxResults=$pageSize",
                emptyMap(),
                null,
                "application/json",
                object : TypeToken<DefaultHttpPage<HttpJiraIssueType>>() {}.type
            ).map { it.body!! }
        }
            .mapLeft { JiraClientError.fromHttpDomainError(it) }
            .bind()
            .toJiraIssueTypes()
    }

    override suspend fun getIssueType(issueTypeId: Number): Either<JiraClientError, JiraIssueType?> = either {
        context.httpClient.executeGet<HttpJiraIssueType>(
            "/rest/api/2/issuetype/$issueTypeId",
            emptyMap(),
            object : TypeToken<HttpJiraIssueType>() {}.type
        )
            .map { it.body }
            .mapLeft { JiraClientError.fromHttpDomainError(it) }
            .bind()
            ?.toJiraIssueType()
    }

    override suspend fun getAttributesOfIssueType(
        projectId: Number,
        issueTypeId: Number
    ): Either<JiraClientError, List<JiraIssueTypeAttribute>> = either {
        recursiveRestCallPaginated { index, pageSize ->
            context.httpClient.executeRest<DefaultHttpPage<HttpJiraIssueTypeAttribute>>(
                "GET",
                "/rest/api/2/issue/createmeta/$projectId/issuetypes/$issueTypeId?startAt=$index&maxResults=$pageSize",
                emptyMap(),
                null,
                "application/json",
                object : TypeToken<DefaultHttpPage<HttpJiraIssueTypeAttribute>>() {}.type
            ).map { it.body!! }
        }
            .mapLeft { JiraClientError.fromHttpDomainError(it) }
            .bind()
            .toJiraIssueTypeAttributes()
    }
}
