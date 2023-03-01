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
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.linkedplanet.kotlinhttpclient.api.http.*
import com.linkedplanet.kotlinjiraclient.api.error.JiraClientError
import com.linkedplanet.kotlinjiraclient.api.interfaces.JiraIssueOperator
import com.linkedplanet.kotlinjiraclient.api.model.JiraIssue
import com.linkedplanet.kotlinjiraclient.api.model.Page
import com.linkedplanet.kotlinjiraclient.http.field.HttpJiraField
import com.linkedplanet.kotlinjiraclient.http.model.HttpMappingField
import com.linkedplanet.kotlinjiraclient.http.util.fromHttpDomainError
import kotlinx.coroutines.runBlocking
import kotlin.math.ceil
import kotlin.math.floor

class HttpJiraIssueOperator(private val context: HttpJiraClientContext) : JiraIssueOperator<HttpJiraField> {

    override var RESULTS_PER_PAGE: Int = 25

    override suspend fun <T> getIssuesByJQL(
        jql: String,
        parser: suspend (JsonObject, Map<String, String>) -> Either<JiraClientError, T>
    ): Either<JiraClientError, List<T>> = either {
        recursiveRestCallPaginatedRaw { index, maxSize ->
            context.httpClient.executeRest<HttpJiraIssuePage>(
                "GET",
                "/rest/api/2/search",
                mapOf(
                    "jql" to jql,
                    "startAt" to index.toString(),
                    "maxResults" to maxSize.toString(),
                    "expand" to "names,transitions",
                ),
                null,
                "application/json",
                object : TypeToken<HttpJiraIssuePage>() {}.type
            )
                .map { it.body!! }
        }
            .mapLeft { e -> JiraClientError.fromHttpDomainError(e) }
            .bind()
            .map { it as HttpJiraIssuePage }
            .flatMap { page ->
                if (page.getTotal().toInt() < 1) {
                    emptyList()
                } else {
                    parseIssues(page, parser).bind()
                }
            }
    }

    override suspend fun <T> getIssuesByJQLPaginated(
        jql: String,
        pageIndex: Int,
        pageSize: Int,
        parser: suspend (JsonObject, Map<String, String>) -> Either<JiraClientError, T>
    ): Either<JiraClientError, Page<T>> = either {
        val page = context.httpClient.executeGet<HttpJiraIssuePage>(
            "/rest/api/2/search",
            mapOf(
                "jql" to jql,
                "startAt" to (pageIndex * pageSize).toString(),
                "maxResults" to pageSize.toString(),
                "expand" to "names,transitions",
            ),
            object : TypeToken<HttpJiraIssuePage>() {}.type
        )
            .map { it.body!! }
            .mapLeft { JiraClientError.fromHttpDomainError(it) }
            .bind()

        val issues = if (page.getTotal().toInt() < 1) {
            emptyList()
        } else {
            parseIssues(page, parser).bind()
        }

        val total = page.getTotal()
        val startAt = page.getStartAt()
        val maxResults = page.getMaxResults()
        Page(
            issues,
            total.toInt(),
            ceil(total.toDouble() / maxResults.toDouble()).toInt(),
            floor(startAt.toDouble() / maxResults.toDouble()).toInt(),
            maxResults.toInt()
        )
    }

    override suspend fun <T> getIssueByJQL(
        jql: String,
        parser: suspend (JsonObject, Map<String, String>) -> Either<JiraClientError, T>
    ): Either<JiraClientError, T?> = either {
        getIssuesByJQLPaginated(jql, 0, 1, parser).bind().items.firstOrNull()
    }

    override suspend fun <T> getIssuesByIssueType(
        projectId: Long,
        issueTypeId: Int,
        parser: suspend (JsonObject, Map<String, String>) -> Either<JiraClientError, T>
    ): Either<JiraClientError, List<T>> = either {
        getIssuesByJQL("project=$projectId AND issueType=$issueTypeId", parser).bind()
    }

    override suspend fun <T> getIssuesByTypePaginated(
        projectId: Long,
        issueTypeId: Int,
        pageIndex: Int,
        pageSize: Int,
        parser: suspend (JsonObject, Map<String, String>) -> Either<JiraClientError, T>
    ): Either<JiraClientError, Page<T>> = either {
        getIssuesByJQLPaginated("project=$projectId AND issueType=$issueTypeId", pageIndex, pageSize, parser).bind()
    }

    override suspend fun <T> getIssueByKey(
        key: String,
        parser: suspend (JsonObject, Map<String, String>) -> Either<JiraClientError, T>
    ): Either<JiraClientError, T?> = either {
        val successResponse = context.httpClient.executeGetCall(
            "/rest/api/2/issue/${key}",
            mapOf(
                "expand" to "names,transitions"
            ),
        )
            .map { it.body }
            .mapLeft {
                // if response is 404 this means no object found therefore null as valid response
                if (it.statusCode == 404) {
                    return@either null
                } else {
                    JiraClientError.fromHttpDomainError(it)
                }
            }.bind()

        // Keep JsonParser instantiation for downwards compatibility
        @Suppress("DEPRECATION")
        val jsonObject = JsonParser().parse(successResponse).asJsonObject
        if (jsonObject.has("id")) {
            val mappings = extractEmbeddedMappings(jsonObject)
            parser(jsonObject, mappings).bind()
        } else {
            null
        }
    }

    override suspend fun <T> getIssueById(
        id: Int,
        parser: suspend (JsonObject, Map<String, String>) -> Either<JiraClientError, T>
    ): Either<JiraClientError, T?> = getIssueByKey(id.toString(), parser)

    override suspend fun createIssue(
        projectId: Long,
        issueTypeId: Int,
        fields: List<HttpJiraField>
    ): Either<JiraClientError, JiraIssue?> = either {
        val jsonBody = prepareRequestBody(projectId, issueTypeId, fields.toList()).bind()

        context.httpClient.executeRest<JiraIssue>(
            "POST",
            "/rest/api/2/issue",
            emptyMap(),
            jsonBody.toString(),
            "application/json",
            object : TypeToken<JiraIssue>() {}.type
        )
            .mapLeft { JiraClientError.fromHttpDomainError(it) }
            .bind()
            .body
    }

    override suspend fun updateIssue(
        projectId: Long,
        issueTypeId: Int,
        issueKey: String,
        fields: List<HttpJiraField>
    ): Either<JiraClientError, Unit> = either {
        val jsonBody = prepareRequestBody(projectId, issueTypeId, fields.toList()).bind()

        context.httpClient.executeRestCall(
            "PUT",
            "/rest/api/2/issue/$issueKey",
            emptyMap(),
            jsonBody.toString(),
            "application/json"
        )
            .mapLeft { JiraClientError.fromHttpDomainError(it) }
            .bind()
    }

    override suspend fun deleteIssue(
        issueKey: String
    ): Either<JiraClientError, Unit> = either {
        context.httpClient.executeRestCall(
            "DELETE",
            "/rest/api/2/issue/$issueKey",
            emptyMap(),
            null,
            "application/json"
        )
            .mapLeft { JiraClientError.fromHttpDomainError(it) }
            .bind()
    }

    private suspend fun <T> parseIssues(
        issuePage: HttpJiraIssuePage,
        parser: suspend (JsonObject, Map<String, String>) -> Either<JiraClientError, T>
    ): Either<JiraClientError, List<T>> = either {
        if (issuePage.getTotal() == 0) {
            emptyList()
        } else {
            val issues = issuePage.getValues()
            val mappings = extractEmbeddedMappings(issuePage)
            issues.map {
                parser(it.asJsonObject, mappings).bind()
            }
        }
    }

    private fun extractEmbeddedMappings(issuePage: HttpJiraIssuePage): Map<String, String> {
        val names = issuePage.names.asJsonObject
        return names
            .entrySet()
            .map { it.key }
            .associateBy {
                names.get(it).asString
            }
    }

    private fun extractEmbeddedMappings(jsonObject: JsonObject): Map<String, String> {
        val names = jsonObject.get("names").asJsonObject
        return names
            .entrySet()
            .map { it.key }
            .associateBy {
                names.get(it).asString
            }
    }

    private suspend fun getMappings(
        projectId: String,
        issueTypeId: String
    ): Either<JiraClientError, Map<String, String>> = either {
        recursiveRestCallPaginated { index, pageSize ->
            runBlocking {
                context.httpClient.executeRest<DefaultHttpPage<HttpMappingField>>(
                    "GET",
                    "/rest/api/2/issue/createmeta/$projectId/issuetypes/$issueTypeId",
                    mapOf(
                        "startAt" to "$index",
                        "maxResults" to "$pageSize",
                        "expand" to "projects.issuetypes.fields"
                    ),
                    null,
                    "application/json",
                    object : TypeToken<DefaultHttpPage<HttpMappingField>>() {}.type
                ).map { it.body!! }
            }
        }
            .mapLeft { JiraClientError.fromHttpDomainError(it) }
            .bind()
            .associateBy(keySelector = { it.name }, valueTransform = { it.fieldId })
    }

    private suspend fun prepareRequestBody(projectId: Number, issueTypeId: Number, fields: List<HttpJiraField>) =
        getMappings(projectId.toString(), issueTypeId.toString())
            .map { mappings ->
                val fieldsObject = JsonObject()
                fields.onEach {
                    it.render(fieldsObject, mappings)
                }

                val jsonBody = JsonObject()
                jsonBody.add("fields", fieldsObject)
                jsonBody
            }

    private class HttpJiraIssuePage(
        private val maxResults: Number,
        private val startAt: Number,
        private val total: Number,
        private val issues: List<JsonElement>,
        val names: JsonElement
    ) : HttpPage<JsonElement> {
        override fun getMaxResults() = maxResults
        override fun getStartAt() = startAt
        override fun getTotal() = total
        override fun getValues(): List<JsonElement> = issues
    }
}
