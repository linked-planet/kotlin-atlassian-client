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
import com.linkedplanet.kotlinjiraclient.api.model.JiraUser

/**
 * Manages Jira users.
 */
interface JiraUserOperator {

    /**
     * Returns a list of users in the specified project.
     * @param projectKey The key of the project.
     * @return Either an error or a list of users.
     */
    suspend fun getUsersByProjectKey(projectKey: String): Either<JiraClientError, List<JiraUser>>

    /**
     * Returns a list of project admin users for the specified project.
     * @param projectKey The key of the project.
     * @return Either an error or a list of users.
     */
    suspend fun getProjectAdminUsers(projectKey: String): Either<JiraClientError, List<JiraUser>>

    /**
     * Returns a list of system admin users.
     * @return Either an error or a list of users.
     */
    suspend fun getSystemAdminUsers(): Either<JiraClientError, List<JiraUser>>

    /**
     * Returns a list of assignable users for the specified project.
     * @param projectKey The key of the project.
     * @return Either an error or a list of users.
     */
    suspend fun getAssignableUsersByProjectKey(projectKey: String): Either<JiraClientError, List<JiraUser>>

    /**
     * Returns a list of users in the specified project with the specified permission.
     * @param projectKey The key of the project.
     * @param permissionName The name of the permission.
     * @return Either an error or a list of users.
     */
    suspend fun getUsersByProjectKeyAndPermission(
        projectKey: String,
        permissionName: String
    ): Either<JiraClientError, List<JiraUser>>
}
