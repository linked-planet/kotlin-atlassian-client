/*-
 * #%L
 * kotlin-insight-client-sdk
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
package com.linkedplanet.kotlininsightclient.sdk.services

import com.atlassian.jira.bc.project.ProjectService
import com.atlassian.jira.bc.project.version.VersionService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.config.properties.ApplicationProperties
import com.linkedplanet.kotlinatlassianclientcore.common.api.ProjectVersion
import com.atlassian.jira.project.version.Version
import com.linkedplanet.kotlininsightclient.sdk.util.getComponent
import com.linkedplanet.kotlininsightclient.sdk.util.getOSGiComponent

/**
 *  Kotlin Version of com.riadalabs.jira.plugins.insight.channel.web.api.rest.services.version.VersionAssemblerInJira
 */
class ReverseEngineeredVersionAssembler {

    private val projectService: ProjectService by getOSGiComponent()
    private val versionService: VersionService by getOSGiComponent()
    private val applicationProperties: ApplicationProperties by getComponent()
    private val jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext()
    private fun user() = jiraAuthenticationContext.loggedInUser
    private fun baseUrl(): String = applicationProperties.jiraBaseUrl

    fun assembleVersion(id: Long): ProjectVersion {
        val version: Version = versionService.getVersionById(user(), id).version
            ?: return createEmptyVersion(id)
        val project = projectService.getProjectById(user(), version.projectId).project
        return ProjectVersion(
            id = id.toInt(),
            name = version.name,
            avatarUrl = "${baseUrl()}/download/resources/com.riadalabs.jira.plugins.insight/images/version-logo.png",
            url = "${baseUrl()}/browse/${project?.key ?: -1}/fixforversion/${id}/?selectedTab=com.riadalabs.jira.plugins.insight:rlabs-version-summary-panel"
        )
    }

    private fun createEmptyVersion(id: Long) = ProjectVersion(
        id = id.toInt(),
        name = "Unknown",
        avatarUrl = "${baseUrl()}/download/resources/com.riadalabs.jira.plugins.insight/images/version-logo.png",
        url = "javascript:void(0);" // returns undefined when called
    )
}
