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

/**
 * Manages Jira issue links.
 */
interface JiraIssueLinkOperator {

    /**
     * Creates a new issue link between two issues.
     * @param inwardIssueKey The key of the inward issue.
     * @param outwardIssueKey The key of the outward issue.
     * @param relationName The name of the relationship between the two issues. Defaults to "Relates".
     * @return Either an error or success.
     */
    suspend fun createIssueLink(
        inwardIssueKey: String,
        outwardIssueKey: String,
        relationName: String = "Relates"
    ): Either<JiraClientError, Unit>

    /**
     * Deletes an existing issue link.
     * @param linkId The ID of the issue link to delete.
     * @return Either an error or success.
     */
    suspend fun deleteIssueLink(linkId: String): Either<JiraClientError, Unit>
}
