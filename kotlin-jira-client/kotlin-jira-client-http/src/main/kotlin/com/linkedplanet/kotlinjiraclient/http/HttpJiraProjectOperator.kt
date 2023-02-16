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
import com.linkedplanet.kotlinjiraclient.api.interfaces.JiraProjectOperator
import com.linkedplanet.kotlinjiraclient.api.model.JiraProject
import com.linkedplanet.kotlinjiraclient.http.model.HttpJiraProject
import com.linkedplanet.kotlinjiraclient.http.model.toJiraProjects
import com.linkedplanet.kotlinjiraclient.http.util.fromHttpDomainError

class HttpJiraProjectOperator(private val context: HttpJiraClientContext) : JiraProjectOperator {

    override suspend fun getProjects(): Either<JiraClientError, List<JiraProject>> = either {
        context.httpClient.executeRestList<HttpJiraProject>(
            "GET",
            "/rest/api/2/project",
            emptyMap(),
            null,
            "application/json",
            object : TypeToken<List<HttpJiraProject>>() {}.type
        ).map { it.body }
            .mapLeft { JiraClientError.fromHttpDomainError(it) }
            .bind()
            .toJiraProjects()
    }

    override suspend fun getProject(projectId: Number): Either<JiraClientError, JiraProject?> = either {
        context.httpClient.executeRest<HttpJiraProject?>(
            "GET",
            "/rest/api/2/project/$projectId",
            emptyMap(),
            null,
            "application/json",
            object : TypeToken<HttpJiraProject?>() {}.type
        ).map { it.body }
            .mapLeft { JiraClientError.fromHttpDomainError(it) }
            .bind()
            ?.toJiraProject()
    }

}
