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
import com.linkedplanet.kotlinjiraclient.api.model.JiraProject

/**
 * Manages Jira projects.
 */
interface JiraProjectOperator {

    /**
     * Returns a list of all projects in Jira.
     * @return Either an error or a list of projects.
     */
    suspend fun getProjects(): Either<JiraClientError, List<JiraProject>>

    /**
     * Returns a project by ID.
     * @param projectId The ID of the project.
     * @return Either an error or the project.
     */
    suspend fun getProject(projectId: Number): Either<JiraClientError, JiraProject?>
}
