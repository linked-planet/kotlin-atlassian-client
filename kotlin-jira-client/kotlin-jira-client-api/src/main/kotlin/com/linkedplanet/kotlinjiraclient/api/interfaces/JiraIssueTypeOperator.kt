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
import com.linkedplanet.kotlinjiraclient.api.model.JiraIssueType
import com.linkedplanet.kotlinjiraclient.api.model.JiraIssueTypeAttribute

/**
 * Manages Jira issue types.
 */
interface JiraIssueTypeOperator {

    /**
     * Returns a list of issue types for a given project.
     * @param projectId The ID of the project.
     * @return Either an error or a list of issue types.
     */
    suspend fun getIssueTypes(
        projectId: Number
    ): Either<JiraClientError, List<JiraIssueType>>

    /**
     * Returns an issue type by ID.
     * @param issueTypeId The ID of the issue type.
     * @return Either an error or the issue type.
     */
    suspend fun getIssueType(
        issueTypeId: Number
    ): Either<JiraClientError, JiraIssueType?>

    /**
     * Returns a list of attributes for an issue type in a given project.
     * @param projectId The ID of the project.
     * @param issueTypeId The ID of the issue type.
     * @return Either an error or a list of issue type attributes.
     */
    suspend fun getAttributesOfIssueType(
        projectId: Number,
        issueTypeId: Number
    ): Either<JiraClientError, List<JiraIssueTypeAttribute>>
}
