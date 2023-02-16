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
import com.google.gson.JsonObject
import com.linkedplanet.kotlinjiraclient.api.error.JiraClientError
import com.linkedplanet.kotlinjiraclient.api.model.JiraIssue

/**
 * Provides methods for working with Jira issues, including retrieving issues by JQL query, issue type, or key; creating and updating issues; and deleting issues.
 */
interface JiraIssueOperator<JiraFieldType> {

    var RESULTS_PER_PAGE: Int

    /**
     * Returns a list of issues based on a JQL query.
     * @param jql The JQL query string.
     * @param parser A parser that converts the issue data from JSON to the desired type.
     * @return Either an error or a list of issues.
     */
    suspend fun <T> getIssuesByJQL(
        jql: String,
        parser: suspend (JsonObject, Map<String, String>) -> Either<JiraClientError, T>
    ): Either<JiraClientError, List<T>>

    /**
     * Returns a paginated list of issues based on a JQL query.
     * @param jql The JQL query string.
     * @param pageIndex The index of the page to retrieve.
     * @param pageSize The number of results per page.
     * @param parser A parser that converts the issue data from JSON to the desired type.
     * @return Either an error or a list of issues.
     */
    suspend fun <T> getIssuesByJQLPaginated(
        jql: String,
        pageIndex: Int = 0,
        pageSize: Int = RESULTS_PER_PAGE,
        parser: suspend (JsonObject, Map<String, String>) -> Either<JiraClientError, T>
    ): Either<JiraClientError, List<T>>

    /**
     * Returns an issue based on a JQL query.
     * @param jql The JQL query string.
     * @param parser A parser that converts the issue data from JSON to the desired type.
     * @return Either an error or an issue.
     */
    suspend fun <T> getIssueByJQL(
        jql: String,
        parser: suspend (JsonObject, Map<String, String>) -> Either<JiraClientError, T>
    ): Either<JiraClientError, T?>

    /**
     * Returns a list of issues for a given project and issue type.
     * @param projectId The ID of the project.
     * @param issueTypeId The ID of the issue type.
     * @param parser A parser that converts the issue data from JSON to the desired type.
     * @return Either an error or a list of issues.
     */
    suspend fun <T> getIssuesByIssueType(
        projectId: Long,
        issueTypeId: Int,
        parser: suspend (JsonObject, Map<String, String>) -> Either<JiraClientError, T>
    ): Either<JiraClientError, List<T>>

    /**
     * Returns a paginated list of issues for a given project and issue type.
     * @param projectId The ID of the project.
     * @param issueTypeId The ID of the issue type.
     * @param pageIndex The index of the page to retrieve.
     * @param pageSize The number of results per page.
     * @param parser A parser that converts the issue data from JSON to the desired type.
     * @return Either an error or a list of issues.
     */
    suspend fun <T> getIssuesByTypePaginated(
        projectId: Long,
        issueTypeId: Int,
        pageIndex: Int = 0,
        pageSize: Int = RESULTS_PER_PAGE,
        parser: suspend (JsonObject, Map<String, String>) -> Either<JiraClientError, T>
    ): Either<JiraClientError, List<T>>

    /**
     * Returns an issue by key.
     * @param key The key of the issue.
     * @param parser A parser that converts the issue data from JSON
     * to the desired type.
     * @return Either an error or the issue.
     */
    suspend fun <T> getIssueByKey(
        key: String,
        parser: suspend (JsonObject, Map<String, String>) -> Either<JiraClientError, T>
    ): Either<JiraClientError, T?>

    /**
     * Returns an issue by ID.
     * @param id The ID of the issue.
     * @param parser A parser that converts the issue data from JSON to the desired type.
     * @return Either an error or the issue.
     */
    suspend fun <T> getIssueById(
        id: Int,
        parser: suspend (JsonObject, Map<String, String>) -> Either<JiraClientError, T>
    ): Either<JiraClientError, T?>

    /**
     * Creates a new issue.
     * @param projectId The ID of the project in which to create the issue.
     * @param issueTypeId The ID of the issue type.
     * @param fields The fields of the issue to create.
     * @return Either an error or the created issue.
     */
    suspend fun createIssue(
        projectId: Long,
        issueTypeId: Int,
        fields: List<JiraFieldType>
    ): Either<JiraClientError, JiraIssue?>

    /**
     * Updates an existing issue.
     * @param projectId The ID of the project.
     * @param issueTypeId The ID of the issue type.
     * @param issueKey The key of the issue to update.
     * @param fields The fields to update.
     * @return Either an error or success.
     */
    suspend fun updateIssue(
        projectId: Long,
        issueTypeId: Int,
        issueKey: String,
        fields: List<JiraFieldType>
    ): Either<JiraClientError, Unit>

    /**
     * Deletes an existing issue.
     * @param issueKey The key of the issue to delete.
     * @return Either an error or success.
     */
    suspend fun deleteIssue(
        issueKey: String
    ): Either<JiraClientError, Unit>
}
