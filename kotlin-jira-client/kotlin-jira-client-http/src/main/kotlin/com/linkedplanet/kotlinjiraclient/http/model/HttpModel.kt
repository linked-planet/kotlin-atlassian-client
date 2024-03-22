/*-
 * #%L
 * kotlin-jira-client-http
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
package com.linkedplanet.kotlinjiraclient.http.model

import com.linkedplanet.kotlinatlassianclientcore.common.api.JiraUser
import com.linkedplanet.kotlinhttpclient.api.http.HttpPage
import com.linkedplanet.kotlinjiraclient.api.model.*

const val DEFAULT_AVATAR_SIZE = "48x48"

data class HttpJiraUser(
    val key: String,
    val name: String,
    val emailAddress: String,
    var avatarUrls: Map<String, String>?,
    val displayName: String
) {
    fun toJiraUser() =
        JiraUser(key, name, emailAddress, avatarUrls?.get(DEFAULT_AVATAR_SIZE), displayName)
}

fun List<HttpJiraUser>.toJiraUsers(): List<JiraUser> =
    map { it.toJiraUser() }

data class HttpMappingField(
    val fieldId: String,
    val name: String
)

data class HttpJiraIssueCommentAuthor(
    val name: String
)

data class HttpJiraIssueComment(
    val id: String,
    val body: String,
    val author: HttpJiraIssueCommentAuthor,
    val created: String
) {
    fun toJiraIssueComment() =
        JiraIssueComment(id, body, author.name, created)
}

fun List<HttpJiraIssueComment>.toJiraIssueComments(): List<JiraIssueComment> =
    map { it.toJiraIssueComment() }

class HttpCommentPage(
    private val maxResults: Number,
    private val startAt: Number,
    private val total: Number,
    private val comments: List<HttpJiraIssueComment>
) : HttpPage<HttpJiraIssueComment> {

    override fun getMaxResults() = maxResults

    override fun getStartAt() = startAt

    override fun getTotal() = total
    override fun getValues(): List<HttpJiraIssueComment> = comments
}

data class HttpJiraIssueType(
    val self: String,
    val id: String,
    val description: String,
    val iconUrl: String,
    val name: String,
    val subtask: Boolean,
    val avatarId: Int
) {
    fun toJiraIssueType() =
        JiraIssueType(id, name)
}

fun List<HttpJiraIssueType>.toJiraIssueTypes(): List<JiraIssueType> =
    map { it.toJiraIssueType() }

data class HttpJiraIssueTypeAttribute(
    val required: Boolean,
    val schema: HttpJiraIssueTypeAttributeJsonSchema?,
    val name: String,
    val fieldId: String,
    val hasDefaultValue: Boolean,
) {
    fun toJiraIssueTypeAttribute() =
        JiraIssueTypeAttribute(
            fieldId,
            name,
            JiraIssueTypeAttributeSchema(
                schema?.type ?: "Any",
                schema?.items,
                schema?.system,
                schema?.custom,
                schema?.customId,
            )
        )
}

data class HttpJiraIssueTypeAttributeJsonSchema(
    val type: String, // "date" see com.atlassian.jira.issue.fields.rest.json.JsonType
    val items: String?, // unclear what this is
    val system: String?, // only used if its a system field, e.g. "assignee"
    val custom: String?, // e.g. com.atlassian.jira.plugin.system.customfieldtypes:datepicker
    val customId: Long? // e.g. 10202
)

fun List<HttpJiraIssueTypeAttribute>.toJiraIssueTypeAttributes(): List<JiraIssueTypeAttribute> =
    map { it.toJiraIssueTypeAttribute() }

data class HttpJiraTransition(
    val id: String,
    val name: String
) {
    fun toJiraTransition() =
        JiraTransition(id, name)
}

fun List<HttpJiraTransition>.toJiraTransitions(): List<JiraTransition> =
    map { it.toJiraTransition() }

data class HttpJiraTransitions(
    val transitions: List<HttpJiraTransition>
)

data class HttpJiraProject(
    val id: String,
    val key: String,
    val name: String
) {
    fun toJiraProject() =
        JiraProject(id, key, name)
}

fun List<HttpJiraProject>.toJiraProjects(): List<JiraProject> =
    map { it.toJiraProject() }

data class HttpJiraRole (
    val id: String,
    val name: String,
    val actors: List<HttpJiraRoleActor>
)

data class HttpJiraRoleActor (
    val id: String,
    val name: String,
    val type: String
)
