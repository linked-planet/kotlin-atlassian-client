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

data class JiraStatus(
    val id: String,
    val name: String,
    val statusCategory: String
)

data class JiraTransition(
    val id: String,
    val name: String
)

data class JiraProject(
    val id: String,
    val key: String,
    val name: String
)

data class JiraIssueType(
    val id: String,
    val name: String,
    val self: String,
    val description: String,
    val subTask: Boolean,
    val iconUrl: String,
    val avatarId: Long
)

data class JiraIssueTypeAttribute(
    val id: String,
    val name: String,
    val schema: JiraIssueTypeAttributeSchema
)

data class JiraIssueTypeAttributeSchema(
    val type: String, // "date" see com.atlassian.jira.issue.fields.rest.json.JsonType
    val items: String?, // unclear what this is
    val system: String?, // only used if its a system field, e.g. "assignee"
    val custom: String?, // e.g. com.atlassian.jira.plugin.system.customfieldtypes:datepicker
    val customId: Long? // e.g. 10202
)

data class JiraIssue(
    val id: String,
    val key: String,
    val self: String
)

data class JiraIssueComment(
    val id: String,
    val content: String,
    val author: String,
    val dateTime: String
)
