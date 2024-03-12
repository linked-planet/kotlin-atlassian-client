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
import arrow.core.raise.either
import arrow.core.left
import com.atlassian.jira.bc.project.ProjectService
import com.atlassian.jira.bc.projectroles.ProjectRoleService
import com.atlassian.jira.bc.user.search.DefaultAssigneeService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.permission.ProjectPermissions
import com.atlassian.jira.project.Project
import com.atlassian.jira.security.roles.ProjectRoleActors
import com.atlassian.jira.user.ApplicationUser
import com.linkedplanet.kotlinjiraclient.api.error.JiraClientError
import com.linkedplanet.kotlinjiraclient.api.interfaces.JiraUserOperator
import com.linkedplanet.kotlinatlassianclientcore.common.api.JiraUser
import com.linkedplanet.kotlinjiraclient.sdk.util.eitherAndCatch
import com.linkedplanet.kotlinjiraclient.sdk.util.toEither
import com.linkedplanet.kotlinjiraclient.sdk.util.withErrorCollection

object SdkJiraUserOperator : JiraUserOperator {

    private val projectService = ComponentAccessor.getComponent(ProjectService::class.java)
    private val permissionManager = ComponentAccessor.getPermissionManager()
    private val userUtil = ComponentAccessor.getUserUtil()
    private val projectRoleService = ComponentAccessor.getComponent(ProjectRoleService::class.java)
    private val avatarService = ComponentAccessor.getAvatarService()
    private val defaultAssigneeService = ComponentAccessor.getComponent(DefaultAssigneeService::class.java)
    private val jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext()

    private fun user() = jiraAuthenticationContext.loggedInUser

    override suspend fun getAssignableUsersByProjectKey(projectKey: String): Either<JiraClientError, List<JiraUser>> =
        eitherAndCatch {
            val project =
                projectService.getProjectByKey(user(), projectKey).toEither().bind().project
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
    ): Either<JiraClientError, List<JiraUser>> = eitherAndCatch {
        val project = projectService.getProjectByKey(user(), projectKey).toEither().bind().project
                ?: return createProjectNotFoundError(projectKey).left()

        withErrorCollection { projectRoleService.getProjectRoles(it) }.bind()
            .map { projectRole ->
                withErrorCollection { projectRoleService.getProjectRoleActors(projectRole, project, it) }.bind()
            }
            .flatMap(ProjectRoleActors::getUsers)
            .filter { predicate(project, it) }
            .map { it.toJiraUser() }
    }

    private fun createProjectNotFoundError(projectKey: String) = JiraClientError(
        "Project not found",
        "No Project with projectKey $projectKey found.",
        statusCode = 404
    )

    private fun ApplicationUser.hasPermissionWithName(permissionName: String, project: Project): Boolean {
        val permission =
            permissionManager.allProjectPermissions.firstOrNull { it.key == permissionName } ?: return false
        return permissionManager.hasPermission(permission.projectPermissionKey, project, this, false)
    }

    private fun ApplicationUser.toJiraUser(): JiraUser {
        val avatarUrl = avatarService.getAvatarURL(user(), this).toASCIIString()
        return JiraUser(key, name, emailAddress, avatarUrl, displayName)
    }
}
