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
import com.atlassian.jira.avatar.Avatar
import com.atlassian.jira.bc.project.ProjectService
import com.atlassian.jira.component.ComponentAccessor
import com.linkedplanet.kotlinatlassianclientcore.common.api.JiraProject
import com.linkedplanet.kotlinjiraclient.api.error.JiraClientError
import com.linkedplanet.kotlinjiraclient.api.interfaces.JiraProjectOperator
import com.linkedplanet.kotlinjiraclient.sdk.util.eitherAndCatch
import com.linkedplanet.kotlinjiraclient.sdk.util.toEither

object SdkJiraProjectOperator : JiraProjectOperator {

    private val projectService = ComponentAccessor.getComponent(ProjectService::class.java)
    private val jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext()
    private val avatarService = ComponentAccessor.getAvatarService()

    private fun user() = jiraAuthenticationContext.loggedInUser

    override suspend fun getProject(projectId: Number): Either<JiraClientError, JiraProject?> =
        eitherAndCatch {
            projectService.getProjectById(user(), projectId.toLong()).toEither().bind().get().let {
                val avatarUrl = avatarService.getProjectAvatarAbsoluteURL(it, Avatar.Size.defaultSize())
                JiraProject(it.id, it.key, it.name, it.url, avatarUrl.toASCIIString())
            }
        }

    override suspend fun getProjects(): Either<JiraClientError, List<JiraProject>> =
        eitherAndCatch {
            return Either.Right(projectService.getAllProjects(user()).toEither().bind().get().map {
                val avatarUrl = avatarService.getProjectAvatarAbsoluteURL(it, Avatar.Size.defaultSize())
                JiraProject(it.id, it.key, it.name, it.url, avatarUrl.toASCIIString())
            })
        }
}
