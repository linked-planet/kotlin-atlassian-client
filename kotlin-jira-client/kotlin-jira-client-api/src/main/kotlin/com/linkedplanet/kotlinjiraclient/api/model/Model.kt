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
package com.linkedplanet.kotlinjiraclient.api.model

import javax.validation.constraints.NotNull

data class JiraStatus(
    @field:NotNull val id: String,
    @field:NotNull val name: String,
    @field:NotNull val statusCategory: String
)

data class JiraTransition(
    @field:NotNull val id: String,
    @field:NotNull val name: String
)

data class JiraProject(
    @field:NotNull val id: String,
    @field:NotNull val key: String,
    @field:NotNull val name: String
)

data class JiraIssueType(
    @field:NotNull val id: String,
    @field:NotNull val name: String,
    @field:NotNull val self: String,
    @field:NotNull val description: String,
    @field:NotNull val subTask: Boolean,
    @field:NotNull val iconUrl: String,
    @field:NotNull val avatarId: Long
)

data class JiraIssueTypeAttribute(
    @field:NotNull val id: String,
    @field:NotNull val name: String,
    @field:NotNull val schema: JiraIssueTypeAttributeSchema
)

data class JiraIssueTypeAttributeSchema(
    @field:NotNull val type: String, // "date" see com.atlassian.jira.issue.fields.rest.json.JsonType
    val items: String?, // unclear what this is
    val system: String?, // only used if its a system field, e.g. "assignee"
    val custom: String?, // e.g. com.atlassian.jira.plugin.system.customfieldtypes:datepicker
    val customId: Long? // e.g. 10202
)

data class JiraIssue(
    @field:NotNull val id: String,
    @field:NotNull val key: String,
    @field:NotNull val self: String
)

data class JiraIssueComment(
    @field:NotNull val id: String,
    @field:NotNull val content: String,
    @field:NotNull val author: String,
    @field:NotNull val dateTime: String
)
