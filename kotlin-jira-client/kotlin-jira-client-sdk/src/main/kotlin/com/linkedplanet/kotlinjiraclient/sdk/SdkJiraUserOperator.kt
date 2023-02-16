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
package com.linkedplanet.kotlinjiraclient.sdk

import arrow.core.Either
import arrow.core.computations.either
import arrow.core.left
import com.atlassian.jira.bc.user.search.DefaultAssigneeService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.permission.ProjectPermissions
import com.atlassian.jira.project.Project
import com.atlassian.jira.security.roles.ProjectRoleActors
import com.atlassian.jira.security.roles.ProjectRoleManager
import com.atlassian.jira.user.ApplicationUser
import com.linkedplanet.kotlinjiraclient.api.error.JiraClientError
import com.linkedplanet.kotlinjiraclient.api.interfaces.JiraUserOperator
import com.linkedplanet.kotlinjiraclient.api.model.JiraUser
import com.linkedplanet.kotlinjiraclient.sdk.util.catchJiraClientError

object SdkJiraUserOperator : JiraUserOperator {

    private val projectManager by lazy { ComponentAccessor.getProjectManager() }
    private val permissionManager by lazy { ComponentAccessor.getPermissionManager() }
    private val userUtil by lazy { ComponentAccessor.getUserUtil() }
    private val jiraAuthenticationContext by lazy { ComponentAccessor.getJiraAuthenticationContext() }
    private val projectRoleManager by lazy { ComponentAccessor.getComponent(ProjectRoleManager::class.java) }
    private val avatarService by lazy { ComponentAccessor.getAvatarService() }
    private val defaultAssigneeService by lazy { ComponentAccessor.getComponent(DefaultAssigneeService::class.java) }

    private fun loggedInUser() = jiraAuthenticationContext.loggedInUser

    override suspend fun getAssignableUsersByProjectKey(projectKey: String): Either<JiraClientError, List<JiraUser>> =
        Either.catchJiraClientError {
            val project =
                projectManager.getProjectByCurrentKey(projectKey)
                    ?: return createProjectNotFoundError(projectKey).left()
            val users = defaultAssigneeService.findAssignableUsers("", project)
            users.map { it.toJiraUser() }
        }

    override suspend fun getProjectAdminUsers(projectKey: String): Either<JiraClientError, List<JiraUser>> =
        getUsersByProjectKeyAndPermission(projectKey, ProjectPermissions.ADMINISTER_PROJECTS.permissionKey())

    override suspend fun getSystemAdminUsers(): Either<JiraClientError, List<JiraUser>> = either {
        userUtil.jiraAdministrators.map { it.toJiraUser() }
    }

    override suspend fun getUsersByProjectKeyAndPermission(
        projectKey: String,
        permissionName: String
    ): Either<JiraClientError, List<JiraUser>> =
        filteredProjectUsers(projectKey) { project, user ->
            user.hasPermissionWithName(permissionName, project)
        }

    override suspend fun getUsersByProjectKey(projectKey: String): Either<JiraClientError, List<JiraUser>> =
        filteredProjectUsers(projectKey) { _, _ -> true }

    private fun filteredProjectUsers(
        projectKey: String,
        predicate: (Project, ApplicationUser) -> Boolean
    ): Either<JiraClientError, List<JiraUser>> = Either.catchJiraClientError {
        val project =
            projectManager.getProjectByCurrentKey(projectKey) ?: return createProjectNotFoundError(projectKey).left()

        projectRoleManager.projectRoles
            .map { projectRole -> projectRoleManager.getProjectRoleActors(projectRole, project) }
            .flatMap(ProjectRoleActors::getUsers)
            .filter { predicate(project, it) }
            .map { it.toJiraUser() }
    }

    private fun createProjectNotFoundError(projectKey: String) = JiraClientError(
        "Project not found",
        "No Project with projectKey $projectKey found."
    )

    private fun ApplicationUser.hasPermissionWithName(permissionName: String, project: Project): Boolean {
        val permission =
            permissionManager.allProjectPermissions.firstOrNull { it.key == permissionName } ?: return false
        return permissionManager.hasPermission(permission.projectPermissionKey, project, this, false)
    }

    private fun ApplicationUser.toJiraUser(): JiraUser {
        val avatarUrl = avatarService.getAvatarURL(loggedInUser(), this).toASCIIString()
        return JiraUser(key, name, emailAddress, avatarUrl, displayName)
    }
}
